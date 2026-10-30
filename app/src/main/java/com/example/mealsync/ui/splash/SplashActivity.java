package com.example.mealsync.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.WindowCompat;

import com.example.mealsync.MainActivity;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.databinding.ActivitySplashBinding;
import com.example.mealsync.ui.auth.WelcomeActivity;

/**
 * Animated launch screen. Plays a short intro, then opens the next screen.
 * A signed-in user goes straight to MainActivity; otherwise the welcome screen opens.
 * Day 2 will also check whether the user belongs to a mess.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long HOLD_AFTER_ANIMATION_MS = 800L;

    private ActivitySplashBinding binding;
    private boolean navigated = false;
    private final Runnable openNext = this::openNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        prepareInitialState();
        playIntro();
    }

    private void prepareInitialState() {
        binding.logoContainer.setAlpha(0f);
        binding.logoContainer.setScaleX(0.4f);
        binding.logoContainer.setScaleY(0.4f);
        binding.imgLogo.setRotation(-90f);

        float offset = 40f * getResources().getDisplayMetrics().density;
        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationY(offset);
        binding.tvTagline.setAlpha(0f);
        binding.tvTagline.setTranslationY(offset);
    }

    private void playIntro() {
        binding.logoContainer.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();

        binding.imgLogo.animate()
                .rotation(0f)
                .setDuration(900)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.tvAppName.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(350).setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        binding.tvTagline.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(600).setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() ->
                        binding.getRoot().postDelayed(openNext, HOLD_AFTER_ANIMATION_MS))
                .start();
    }

    private void openNextScreen() {
        if (navigated) {
            return;
        }
        navigated = true;
        Class<?> next = new AuthRepository().getCurrentUser() != null
                ? MainActivity.class : WelcomeActivity.class;
        Intent intent = new Intent(this, next);
        startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(
                this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
        finish();
    }

    @Override
    protected void onDestroy() {
        // Stop pending work so nothing runs after the screen is gone (prevents leaks).
        binding.getRoot().removeCallbacks(openNext);
        binding.logoContainer.animate().cancel();
        binding.imgLogo.animate().cancel();
        binding.tvAppName.animate().cancel();
        binding.tvTagline.animate().cancel();
        super.onDestroy();
    }
}