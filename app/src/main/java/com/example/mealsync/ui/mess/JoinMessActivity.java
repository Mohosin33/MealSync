package com.example.mealsync.ui.mess;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.MainActivity;
import com.example.mealsync.R;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityJoinMessBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.ui.common.StickyHeaderBehavior;
import com.example.mealsync.util.InvitationCodeValidator;

public class JoinMessActivity extends AppCompatActivity {

    private static final int OTP_LENGTH = 6;

    private ActivityJoinMessBinding binding;
    private final MessRepository messRepository = new MessRepository();
    private TextView[] otpBoxes;
    private boolean codeErrorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityJoinMessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        otpBoxes = new TextView[]{binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6};

        applyWindowInsets();
        StickyHeaderBehavior.attach(binding.scrollContent, binding.header, R.drawable.bg_gradient_member);
        setupListeners();
        updateOtpBoxes();
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

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnJoinMess.setOnClickListener(v -> attemptJoinMess());

        binding.etOtp.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (codeErrorShown) {
                    clearCodeError();
                }
                updateOtpBoxes();
            }
        });
        binding.etOtp.setOnFocusChangeListener((v, hasFocus) -> updateOtpBoxes());

        binding.etDisplayName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilDisplayName.setError(null);
            }
        });
    }

    private void attemptJoinMess() {
        String code = binding.etOtp.getText() != null ? binding.etOtp.getText().toString().trim() : "";
        String displayName = binding.etDisplayName.getText() != null ? binding.etDisplayName.getText().toString().trim() : "";

        if (!InvitationCodeValidator.isValidFormat(code)) {
            showCodeError(getString(R.string.err_invalid_code));
            return;
        }
        if (displayName.isEmpty()) {
            binding.tilDisplayName.setError(getString(R.string.err_empty_display_name));
            return;
        }

        setLoading(true);
        messRepository.joinMess(code, displayName, new MessRepository.JoinMessCallback() {
            @Override
            public void onSuccess(String messId) {
                setLoading(false);
                navigateToMain();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to join mess");
                showCodeError(msg);
            }
        });
    }

    private void updateOtpBoxes() {
        String text = binding.etOtp.getText() != null ? binding.etOtp.getText().toString() : "";
        int length = text.length();
        boolean focused = binding.etOtp.hasFocus();

        int accentColor = ContextCompat.getColor(this, R.color.ms_teal);
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
        binding.tvCodeError.setVisibility(View.GONE);
        updateOtpBoxes();
    }

    private void shake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, dp(10), -dp(10), dp(8), -dp(8), dp(4), -dp(4), 0f);
        animator.setDuration(450);
        animator.start();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnJoinMess.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
