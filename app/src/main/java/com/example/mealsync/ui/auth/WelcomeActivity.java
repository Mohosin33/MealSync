package com.example.mealsync.ui.auth;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.util.Patterns;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

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
import com.example.mealsync.databinding.ActivityWelcomeBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.ui.mess.ChooseActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Welcome screen with the Member / Manager path switch.
 * The switch only changes the starting path and the theme. It does NOT grant any
 * permission: the real role is stored when a mess is created or joined and is
 * enforced by Firestore Security Rules.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final String STATE_MANAGER = "state_manager";
    private static final long ROLE_ANIM_MS = 450L;

    private ActivityWelcomeBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private boolean managerSelected = false;
    private int indicatorWidth = 0;
    private int currentAccent;
    private ValueAnimator accentAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightNavigationBars(true);

        String roleExtra = getIntent().getStringExtra(AuthExtras.EXTRA_ROLE);
        managerSelected = AuthExtras.ROLE_MANAGER.equalsIgnoreCase(roleExtra);
        if (savedInstanceState != null) {
            managerSelected = savedInstanceState.getBoolean(STATE_MANAGER, false);
        }

        applyWindowInsets();
        setupRoleToggle();
        setupButtons();
        applyRole(managerSelected, false);
    }

    // ---------- Layout and insets ----------

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            binding.header.setPadding(0, bars.top + dp(12), 0, 0);
            binding.sheet.setPadding(dp(24) + bars.left, dp(16),
                    dp(24) + bars.right, dp(24) + Math.max(bars.bottom, imeBottom));
            return insets;
        });
    }

    private void navigateToNextScreen(FirebaseUser user) {
        if (user == null) return;

        new MessRepository().getUserProfile(user.getUid(), new MessRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile != null && profile.getMessId() != null
                        && !profile.getMessId().trim().isEmpty()) {
                    openNextActivity(MainActivity.class);
                } else {
                    openChooseActivity();
                }
            }

            @Override
            public void onFailure(Exception error) {
                String message = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage()
                        : getString(R.string.err_generic, "Could not load your profile");
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void openChooseActivity() {
        Intent intent = new Intent(this, ChooseActivity.class);
        intent.putExtra(AuthExtras.EXTRA_ROLE,
                managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openNextActivity(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------- Role toggle ----------

    private void setupRoleToggle() {
        binding.roleMember.setOnClickListener(v -> onRoleTapped(false));
        binding.roleManager.setOnClickListener(v -> onRoleTapped(true));
        binding.roleToggle.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        syncIndicator());
    }

    private void onRoleTapped(boolean manager) {
        if (manager == managerSelected) {
            return;
        }
        managerSelected = manager;
        binding.roleToggle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        applyRole(manager, true);
    }

    /** Sizes the sliding pill to half of the track and places it under the chosen role. */
    private void syncIndicator() {
        int track = binding.roleToggle.getWidth()
                - binding.roleToggle.getPaddingLeft()
                - binding.roleToggle.getPaddingRight();
        int half = track / 2;
        if (half <= 0 || half == indicatorWidth) {
            return;
        }
        indicatorWidth = half;
        ViewGroup.LayoutParams lp = binding.roleIndicator.getLayoutParams();
        lp.width = half;
        binding.roleIndicator.setLayoutParams(lp);
        binding.roleIndicator.setTranslationX(managerSelected ? half : 0f);
    }

    private void applyRole(boolean manager, boolean animate) {
        binding.roleMember.setSelected(!manager);
        binding.roleManager.setSelected(manager);
        int toAccent = ContextCompat.getColor(this,
                manager ? R.color.ms_manager_end : R.color.ms_teal);
        if (!animate) {
            renderFraction(1f, toAccent, toAccent, manager);
            syncIndicator();
            return;
        }

        final int fromAccent = currentAccent;
        binding.roleIndicator.animate().translationX(manager ? indicatorWidth : 0f)
                .setDuration(ROLE_ANIM_MS)
                .setInterpolator(new DecelerateInterpolator(1.5f)).start();

        if (accentAnimator != null) {
            accentAnimator.cancel();
        }
        accentAnimator = ValueAnimator.ofFloat(0f, 1f);
        accentAnimator.setDuration(ROLE_ANIM_MS);
        accentAnimator.addUpdateListener(a ->
                renderFraction((float) a.getAnimatedValue(), fromAccent, toAccent, manager));
        accentAnimator.start();
    }

    /** Draws one frame of the colour change (f goes from 0 to 1). */
    private void renderFraction(float f, int fromAccent, int toAccent, boolean toManager) {
        ArgbEvaluator ev = new ArgbEvaluator();
        int accent = (Integer) ev.evaluate(f, fromAccent, toAccent);
        currentAccent = accent;

        ColorStateList tint = ColorStateList.valueOf(accent);
        binding.roleIndicator.setBackgroundTintList(tint);
        binding.btnLogin.setBackgroundTintList(tint);
        binding.btnGoogle.setTextColor(accent);
        binding.btnGoogle.setStrokeColor(tint);
        binding.tvGoToRegister.setTextColor(accent);
        binding.tvForgotPassword.setTextColor(accent);
        binding.header.getChildAt(0).setBackgroundTintList(tint);
        binding.tilEmail.setBoxStrokeColor(accent);
        binding.tilPassword.setBoxStrokeColor(accent);

        int white = Color.WHITE;
        int grey = ContextCompat.getColor(this, R.color.ms_text_secondary);
        binding.roleMember.setTextColor((Integer) ev.evaluate(f,
                toManager ? white : grey, toManager ? grey : white));
        binding.roleManager.setTextColor((Integer) ev.evaluate(f,
                toManager ? grey : white, toManager ? white : grey));
    }

    // ---------- Buttons & Auth ----------

    private void setupButtons() {
        binding.btnLogin.setOnClickListener(v -> attemptEmailLogin());
        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());
        binding.tvForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra(AuthExtras.EXTRA_ROLE,
                    managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
            startActivity(intent);
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

    private void sendPasswordResetEmail() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.err_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }

        setLoading(true);
        authRepository.sendPasswordResetEmail(email, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.password_reset_sent, Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String message = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage()
                        : getString(R.string.err_generic, "Password reset email could not be sent");
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void attemptEmailLogin() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

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
            public void onUnverifiedEmail(String unverifiedEmail) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.err_email_not_verified, Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String message = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage()
                        : getString(R.string.err_generic, "Sign in failed");
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogle.setEnabled(!loading);
        binding.tvGoToRegister.setEnabled(!loading);
        binding.tvForgotPassword.setEnabled(!loading);
        binding.roleMember.setEnabled(!loading);
        binding.roleManager.setEnabled(!loading);
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
                                    ? e.getLocalizedMessage()
                                    : getString(R.string.err_generic, "Google sign-in failed");
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
                Snackbar.make(binding.getRoot(),
                        getString(R.string.err_generic, "Google sign-in failed"),
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            setLoading(false);
            Snackbar.make(binding.getRoot(),
                    getString(R.string.err_generic, "Google sign-in failed"),
                    Snackbar.LENGTH_LONG).show();
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
                        ? error.getLocalizedMessage()
                        : getString(R.string.err_generic, "Sign in failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MANAGER, managerSelected);
    }

    @Override
    protected void onDestroy() {
        if (accentAnimator != null) {
            accentAnimator.cancel();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
