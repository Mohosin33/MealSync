package com.example.mealsync.ui.auth;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import com.example.mealsync.ui.mess.ChooseActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

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
    private final List<ObjectAnimator> floatAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        if (savedInstanceState != null) {
            managerSelected = savedInstanceState.getBoolean(STATE_MANAGER, false);
        }

        applyWindowInsets();
        setupRoleToggle();
        setupButtons();
        applyRole(managerSelected, false);
        startFloatingCircles();
        if (savedInstanceState == null) {
            playEntranceAnimation();
        }
    }

    // ---------- Layout and insets ----------

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            binding.header.setPadding(0, bars.top, 0, 0);
            binding.sheet.setPadding(dp(24) + bars.left, dp(28),
                    dp(24) + bars.right, dp(24) + bars.bottom);
            return insets;
        });
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
        updateTexts(manager, animate);

        if (!animate) {
            binding.bgManager.setAlpha(manager ? 1f : 0f);
            renderFraction(1f, toAccent, toAccent, manager);
            syncIndicator();
            return;
        }

        final int fromAccent = currentAccent;
        binding.bgManager.animate().alpha(manager ? 1f : 0f)
                .setDuration(ROLE_ANIM_MS).start();
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
        binding.btnPhone.setBackgroundTintList(tint);
        binding.btnGoogle.setTextColor(accent);
        binding.btnGoogle.setStrokeColor(tint);

        int white = Color.WHITE;
        int grey = ContextCompat.getColor(this, R.color.ms_text_secondary);
        binding.roleMember.setTextColor((Integer) ev.evaluate(f,
                toManager ? white : grey, toManager ? grey : white));
        binding.roleManager.setTextColor((Integer) ev.evaluate(f,
                toManager ? grey : white, toManager ? white : grey));
    }

    private void updateTexts(boolean manager, boolean animate) {
        Runnable setTexts = () -> {
            binding.tvWelcomeTitle.setText(manager
                    ? R.string.welcome_manager_title : R.string.welcome_member_title);
            binding.tvWelcomeSubtitle.setText(manager
                    ? R.string.welcome_manager_subtitle : R.string.welcome_member_subtitle);
        };
        if (!animate) {
            setTexts.run();
            return;
        }
        binding.textBlock.animate().alpha(0f).translationY(dp(8)).setDuration(130)
                .withEndAction(() -> {
                    setTexts.run();
                    binding.textBlock.setTranslationY(-dp(8));
                    binding.textBlock.animate().alpha(1f).translationY(0f)
                            .setDuration(220).start();
                }).start();
    }

    // ---------- Buttons & Auth ----------

    private void setupButtons() {
        binding.btnPhone.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(AuthExtras.EXTRA_ROLE,
                    managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
            startActivity(intent);
        });
        binding.btnGoogle.setOnClickListener(v -> signInWithGoogle());
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

        binding.btnGoogle.setEnabled(false);
        binding.btnPhone.setEnabled(false);

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
                        binding.btnGoogle.setEnabled(true);
                        binding.btnPhone.setEnabled(true);
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
                binding.btnGoogle.setEnabled(true);
                binding.btnPhone.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.err_generic, Snackbar.LENGTH_LONG).show();
            }
        } else {
            binding.btnGoogle.setEnabled(true);
            binding.btnPhone.setEnabled(true);
            Snackbar.make(binding.getRoot(), R.string.err_generic, Snackbar.LENGTH_LONG).show();
        }
    }

    private void firebaseSignInWithGoogle(AuthCredential credential) {
        authRepository.signInWithCredential(credential, new AuthRepository.SignInCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                new MessRepository().getUserProfile(user.getUid(), new MessRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile profile) {
                        Class<?> target = (profile != null && profile.getMessId() != null && !profile.getMessId().trim().isEmpty())
                                ? MainActivity.class : ChooseActivity.class;
                        Intent intent = new Intent(WelcomeActivity.this, target);
                        intent.putExtra(AuthExtras.EXTRA_ROLE, managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception error) {
                        Intent intent = new Intent(WelcomeActivity.this, ChooseActivity.class);
                        intent.putExtra(AuthExtras.EXTRA_ROLE, managerSelected ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception error) {
                binding.btnGoogle.setEnabled(true);
                binding.btnPhone.setEnabled(true);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage()
                        : getString(R.string.err_generic, "Sign in failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ---------- Animations ----------

    private void playEntranceAnimation() {
        binding.header.setAlpha(0f);
        binding.header.setTranslationY(-dp(24));
        binding.header.animate().alpha(1f).translationY(0f).setDuration(700)
                .setInterpolator(new DecelerateInterpolator()).start();

        // The sheet slides up first, then its content appears one item at a time.
        binding.sheet.setTranslationY(dp(480));
        binding.sheet.animate().translationY(0f).setStartDelay(250).setDuration(650)
                .setInterpolator(new DecelerateInterpolator(2f)).start();

        View[] items = {binding.roleToggle, binding.textBlock,
                binding.btnPhone, binding.btnGoogle, binding.tvNote};
        for (int i = 0; i < items.length; i++) {
            items[i].setAlpha(0f);
            items[i].setTranslationY(dp(30));
            items[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(500 + i * 90L).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void startFloatingCircles() {
        addFloat(binding.circleOne, dp(28), 5200L);
        addFloat(binding.circleTwo, -dp(22), 4300L);
        addFloat(binding.circleThree, dp(18), 6100L);
    }

    private void addFloat(View view, float distance, long duration) {
        ObjectAnimator a = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, distance);
        a.setDuration(duration);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
        floatAnimators.add(a);
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onPause() {
        super.onPause();
        for (ObjectAnimator a : floatAnimators) {
            a.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (ObjectAnimator a : floatAnimators) {
            a.resume();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MANAGER, managerSelected);
    }

    @Override
    protected void onDestroy() {
        for (ObjectAnimator a : floatAnimators) {
            a.cancel();
        }
        floatAnimators.clear();
        if (accentAnimator != null) {
            accentAnimator.cancel();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
