package com.example.mealsync.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.mealsync.MainActivity;
import com.example.mealsync.R;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityLoginBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.ui.mess.ChooseActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String STATE_MANAGER = "state_manager";

    private ActivityLoginBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private boolean managerSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        String roleExtra = getIntent().getStringExtra(AuthExtras.EXTRA_ROLE);
        if (roleExtra != null) {
            managerSelected = AuthExtras.ROLE_MANAGER.equalsIgnoreCase(roleExtra);
        } else if (savedInstanceState != null) {
            managerSelected = savedInstanceState.getBoolean(STATE_MANAGER, false);
        }

        applyWindowInsets();
        applyRoleTheme();
        setupListeners();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int pad = Math.round(24 * getResources().getDisplayMetrics().density);
            binding.header.setPadding(pad, bars.top + Math.round(16 * getResources().getDisplayMetrics().density), pad, pad);
            binding.sheet.setPadding(pad, pad, pad, pad + Math.max(bars.bottom, imeBottom));
            return insets;
        });
    }

    private void applyRoleTheme() {
        int accent = ContextCompat.getColor(this,
                managerSelected ? R.color.ms_manager_end : R.color.ms_teal);
        binding.bgManager.setAlpha(managerSelected ? 1f : 0f);
        binding.tvLoginSubtitle.setText(managerSelected ? R.string.login_subtitle_manager : R.string.login_subtitle_member);

        ColorStateList tint = ColorStateList.valueOf(accent);
        binding.btnLogin.setBackgroundTintList(tint);
        binding.btnResendVerification.setTextColor(accent);
        binding.tilEmail.setBoxStrokeColor(accent);
        binding.tilPassword.setBoxStrokeColor(accent);
        binding.btnGoogle.setTextColor(accent);
        binding.btnGoogle.setStrokeColor(tint);
        binding.tvGoToRegister.setTextColor(accent);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnLogin.setOnClickListener(v -> attemptEmailLogin());
        binding.btnResendVerification.setOnClickListener(v -> attemptResendVerification());
        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra(AuthExtras.EXTRA_ROLE, managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
            startActivity(intent);
            finish();
        });

        SimpleTextWatcher clearErrorsWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilEmail.setError(null);
                binding.tilPassword.setError(null);
            }
        };
        binding.etEmail.addTextChangedListener(clearErrorsWatcher);
        binding.etPassword.addTextChangedListener(clearErrorsWatcher);
    }

    private void attemptEmailLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.err_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.err_empty_password));
            return;
        }

        setLoading(true);
        authRepository.signInWithEmail(email, password, new AuthRepository.EmailSignInCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                navigateToNextScreen(user);
            }

            @Override
            public void onUnverifiedEmail(String email) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.err_email_not_verified, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.resend_action, v -> attemptResendVerification())
                        .show();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Login failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void attemptResendVerification() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.err_empty_password));
            return;
        }

        setLoading(true);
        authRepository.resendVerificationEmail(email, password, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), getString(R.string.verification_email_sent, email), Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Resend failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void signInWithGoogle() {
        CredentialManager credentialManager = CredentialManager.create(this);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        setLoading(true);

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredentialResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        setLoading(false);
                        if (!(e instanceof GetCredentialCancellationException)) {
                            String message = e.getLocalizedMessage() != null
                                    ? e.getLocalizedMessage() : getString(R.string.err_generic, "Google sign-in failed");
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void handleGoogleCredentialResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        if (credential instanceof CustomCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();
                AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
                firebaseSignInWithGoogle(authCredential);
            } catch (Exception e) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.err_generic, Snackbar.LENGTH_LONG).show();
            }
        } else {
            setLoading(false);
            Snackbar.make(binding.getRoot(), R.string.err_generic, Snackbar.LENGTH_LONG).show();
        }
    }

    private void firebaseSignInWithGoogle(AuthCredential credential) {
        authRepository.signInWithCredential(credential, new AuthRepository.SignInCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                navigateToNextScreen(user);
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Sign in failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToNextScreen(FirebaseUser user) {
        if (user == null) {
            return;
        }
        MessRepository messRepository = new MessRepository();
        messRepository.getUserProfile(user.getUid(), new MessRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                Class<?> target = (profile != null && profile.getMessId() != null && !profile.getMessId().trim().isEmpty())
                        ? MainActivity.class : ChooseActivity.class;
                Intent intent = new Intent(LoginActivity.this, target);
                intent.putExtra(AuthExtras.EXTRA_ROLE, managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception error) {
                Intent intent = new Intent(LoginActivity.this, ChooseActivity.class);
                intent.putExtra(AuthExtras.EXTRA_ROLE, managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnResendVerification.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
        binding.tvGoToRegister.setEnabled(!loading);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MANAGER, managerSelected);
    }
}
