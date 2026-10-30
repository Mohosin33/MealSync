package com.example.mealsync.ui.auth;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.mealsync.MainActivity;
import com.example.mealsync.R;
import com.example.mealsync.databinding.ActivityPhoneLoginBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.viewmodel.PhoneLoginViewModel;
import com.google.android.material.snackbar.Snackbar;

/**
 * Phone number + six-box code screen. The invisible EditText (etOtp) receives all typing
 * and pasting; the six TextViews only display its digits.
 */
public class PhoneLoginActivity extends AppCompatActivity {

    private static final int OTP_LENGTH = 6;
    private static final long CHECK_START_DELAY_MS = 450L;
    private static final long SUCCESS_HOLD_MS = 1500L;

    private ActivityPhoneLoginBinding binding;
    private PhoneLoginViewModel viewModel;
    private TextView[] otpBoxes;
    private String role;
    private int accentColor;
    private boolean codeErrorShown = false;
    private boolean navigated = false;
    private PhoneLoginViewModel.Stage shownStage = null;
    private OnBackPressedCallback backCallback;

    private final Runnable startCheckRunnable = this::startCheckAnimation;
    private final Runnable openNextRunnable = this::openNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityPhoneLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        viewModel = new ViewModelProvider(this).get(PhoneLoginViewModel.class);
        role = getIntent().getStringExtra(AuthExtras.EXTRA_ROLE);
        if (!AuthExtras.ROLE_MANAGER.equals(role)) {
            role = AuthExtras.ROLE_MEMBER;
        }
        otpBoxes = new TextView[]{binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6};

        applyRoleTheme();
        applyWindowInsets();
        setupPhonePanel();
        setupCodePanel();
        setupBackHandling();
        observeViewModel();
        updateOtpBoxes();
    }

    // ---------- Setup ----------

    private void applyRoleTheme() {
        boolean manager = AuthExtras.ROLE_MANAGER.equals(role);
        accentColor = ContextCompat.getColor(this,
                manager ? R.color.ms_manager_end : R.color.ms_teal);
        binding.bgView.setBackgroundResource(manager
                ? R.drawable.bg_gradient_manager : R.drawable.bg_gradient_member);
        binding.tvHeaderSubtitle.setText(manager
                ? R.string.phone_login_subtitle_manager : R.string.phone_login_subtitle_member);

        ColorStateList tint = ColorStateList.valueOf(accentColor);
        binding.btnSendCode.setBackgroundTintList(tint);
        binding.btnVerify.setBackgroundTintList(tint);
        binding.tilPhone.setBoxStrokeColor(accentColor);
        binding.tvChangeNumber.setTextColor(accentColor);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            binding.header.setPadding(bars.left, bars.top, bars.right, 0);
            binding.sheet.setPadding(dp(24) + bars.left, dp(28), dp(24) + bars.right,
                    dp(24) + Math.max(bars.bottom, imeBottom));
            return insets;
        });
    }

    private void setupPhonePanel() {
        binding.etPhone.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilPhone.setError(null);
            }
        });
        binding.etPhone.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitPhone();
                return true;
            }
            return false;
        });
        binding.btnSendCode.setOnClickListener(v -> submitPhone());
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupCodePanel() {
        binding.etOtp.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (codeErrorShown) {
                    clearCodeError();
                }
                updateOtpBoxes();
                if (s.length() == OTP_LENGTH) {
                    viewModel.submitCode(s.toString());
                }
            }
        });
        binding.etOtp.setOnFocusChangeListener((v, hasFocus) -> updateOtpBoxes());
        binding.etOtp.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.submitCode(binding.etOtp.getText().toString());
                return true;
            }
            return false;
        });
        binding.btnVerify.setOnClickListener(v ->
                viewModel.submitCode(binding.etOtp.getText().toString()));
        binding.tvResend.setOnClickListener(v -> viewModel.resendCode(this));
        binding.tvChangeNumber.setOnClickListener(v -> changeNumber());
    }

    private void setupBackHandling() {
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Enabled only while the code panel or the success screen is showing.
                // On the code panel Back returns to the phone panel; on success it does nothing.
                if (viewModel.getStage().getValue() == PhoneLoginViewModel.Stage.ENTER_CODE) {
                    changeNumber();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    private void observeViewModel() {
        viewModel.getStage().observe(this, this::renderStage);
        viewModel.getBusy().observe(this, this::renderBusy);
        viewModel.getError().observe(this, this::renderError);
        viewModel.getResendSeconds().observe(this, this::renderResend);
    }

    // ---------- Actions ----------

    private void submitPhone() {
        binding.tilPhone.setError(null);
        Editable text = binding.etPhone.getText();
        viewModel.submitPhone(this, text == null ? "" : text.toString());
    }

    private void changeNumber() {
        binding.etOtp.setText("");
        clearCodeError();
        updateOtpBoxes();
        viewModel.backToPhone();
    }

    // ---------- Rendering ----------

    private void renderStage(PhoneLoginViewModel.Stage stage) {
        boolean animate = shownStage != null && shownStage != stage;
        shownStage = stage;
        backCallback.setEnabled(stage != PhoneLoginViewModel.Stage.ENTER_PHONE);

        switch (stage) {
            case ENTER_PHONE:
                switchPanels(binding.panelCode, binding.panelPhone, animate, null);
                break;
            case ENTER_CODE:
                binding.tvCodeSubtitle.setText(
                        getString(R.string.otp_sent_to, viewModel.getPhoneE164()));
                switchPanels(binding.panelPhone, binding.panelCode, animate, this::focusOtp);
                break;
            case SIGNED_IN:
                showSuccess();
                break;
        }
    }

    private void renderBusy(Boolean busy) {
        boolean isBusy = busy != null && busy;
        binding.progress.setVisibility(isBusy ? View.VISIBLE : View.INVISIBLE);
        binding.btnSendCode.setEnabled(!isBusy);
        binding.btnVerify.setEnabled(!isBusy);
        binding.tvChangeNumber.setEnabled(!isBusy);
    }

    private void renderError(PhoneLoginViewModel.UiError error) {
        if (error == null) {
            return;
        }
        viewModel.consumeError();
        String message = error.detail != null
                ? getString(error.messageRes, error.detail) : getString(error.messageRes);
        switch (error.target) {
            case PHONE:
                binding.tilPhone.setError(message);
                break;
            case CODE:
                showCodeError(message);
                break;
            default:
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void renderResend(Integer secondsValue) {
        int seconds = secondsValue == null ? 0 : secondsValue;
        if (seconds > 0) {
            binding.tvResend.setText(getString(R.string.resend_in, seconds / 60, seconds % 60));
            binding.tvResend.setEnabled(false);
            binding.tvResend.setTextColor(ContextCompat.getColor(this, R.color.ms_text_secondary));
        } else {
            binding.tvResend.setText(R.string.resend_code);
            binding.tvResend.setEnabled(true);
            binding.tvResend.setTextColor(accentColor);
        }
    }

    // ---------- Code boxes ----------

    private void updateOtpBoxes() {
        String text = binding.etOtp.getText().toString();
        int length = text.length();
        boolean focused = binding.etOtp.hasFocus();
        int errorColor = ContextCompat.getColor(this, R.color.ms_negative);
        int dividerColor = ContextCompat.getColor(this, R.color.ms_divider);
        int primaryText = ContextCompat.getColor(this, R.color.ms_text_primary);

        for (int i = 0; i < OTP_LENGTH; i++) {
            TextView box = otpBoxes[i];
            box.setText(i < length ? String.valueOf(text.charAt(i)) : "");
            int stroke;
            int width;
            if (codeErrorShown) {
                stroke = errorColor;
                width = dp(2);
            } else if (focused && i == Math.min(length, OTP_LENGTH - 1)) {
                stroke = accentColor;
                width = dp(2);
            } else if (i < length) {
                stroke = accentColor;
                width = dp(1);
            } else {
                stroke = dividerColor;
                width = dp(1);
            }
            box.setTextColor(codeErrorShown ? errorColor : primaryText);
            box.setBackground(makeBox(stroke, width));
        }
    }

    private GradientDrawable makeBox(int strokeColor, int strokeWidthPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(12));
        drawable.setColor(ContextCompat.getColor(this, R.color.white));
        drawable.setStroke(strokeWidthPx, strokeColor);
        return drawable;
    }

    private void showCodeError(String message) {
        codeErrorShown = true;
        binding.tvCodeError.setText(message);
        binding.tvCodeError.setVisibility(View.VISIBLE);
        updateOtpBoxes();
        shake(binding.otpContainer);
        binding.otpContainer.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
    }

    private void clearCodeError() {
        codeErrorShown = false;
        binding.tvCodeError.setVisibility(View.INVISIBLE);
    }

    private void focusOtp() {
        binding.etOtp.requestFocus();
        binding.etOtp.post(() -> WindowCompat.getInsetsController(getWindow(), binding.etOtp)
                .show(WindowInsetsCompat.Type.ime()));
    }

    // ---------- Animations ----------

    private void switchPanels(View from, View to, boolean animate, Runnable onShown) {
        from.animate().cancel();
        to.animate().cancel();
        if (!animate) {
            from.setVisibility(View.GONE);
            to.setAlpha(1f);
            to.setTranslationX(0f);
            to.setVisibility(View.VISIBLE);
            if (onShown != null) {
                onShown.run();
            }
            return;
        }
        from.animate().alpha(0f).translationX(-dp(24)).setDuration(160)
                .withEndAction(() -> {
                    from.setVisibility(View.GONE);
                    from.setAlpha(1f);
                    from.setTranslationX(0f);
                    to.setAlpha(0f);
                    to.setTranslationX(dp(24));
                    to.setVisibility(View.VISIBLE);
                    to.animate().alpha(1f).translationX(0f).setDuration(220).start();
                    if (onShown != null) {
                        onShown.run();
                    }
                }).start();
    }

    private void shake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, dp(10), -dp(10), dp(8), -dp(8), dp(4), -dp(4), 0f);
        animator.setDuration(450);
        animator.start();
    }

    private void showSuccess() {
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());

        binding.successOverlay.setAlpha(0f);
        binding.successOverlay.setVisibility(View.VISIBLE);
        binding.successOverlay.animate().alpha(1f).setDuration(220).start();

        binding.successCircle.setScaleX(0f);
        binding.successCircle.setScaleY(0f);
        binding.successCircle.animate().scaleX(1f).scaleY(1f)
                .setStartDelay(120).setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();

        binding.imgCheck.setAlpha(0f);
        binding.tvSuccess.setAlpha(0f);
        binding.tvSuccess.animate().alpha(1f).setStartDelay(500).setDuration(300).start();

        binding.getRoot().postDelayed(startCheckRunnable, CHECK_START_DELAY_MS);
        binding.getRoot().postDelayed(openNextRunnable, SUCCESS_HOLD_MS);
    }

    private void startCheckAnimation() {
        binding.imgCheck.setAlpha(1f);
        Drawable drawable = binding.imgCheck.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }

    private void openNextScreen() {
        if (navigated) {
            return;
        }
        navigated = true;
        // Day 2 will route by mess membership. For now go to the placeholder home screen.
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(AuthExtras.EXTRA_ROLE, role);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------- Lifecycle ----------

    @Override
    protected void onDestroy() {
        // Stop pending work so nothing runs after the screen is gone (prevents leaks).
        binding.getRoot().removeCallbacks(startCheckRunnable);
        binding.getRoot().removeCallbacks(openNextRunnable);
        binding.successOverlay.animate().cancel();
        binding.successCircle.animate().cancel();
        binding.tvSuccess.animate().cancel();
        binding.panelPhone.animate().cancel();
        binding.panelCode.animate().cancel();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}