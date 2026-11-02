package com.example.mealsync.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.R;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.databinding.ActivityRegisterBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        role = getIntent().getStringExtra(AuthExtras.EXTRA_ROLE);
        if (role == null) {
            role = AuthExtras.ROLE_MEMBER;
        }

        applyWindowInsets();
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

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(AuthExtras.EXTRA_ROLE, role);
            startActivity(intent);
            finish();
        });
        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        SimpleTextWatcher clearErrorsWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilEmail.setError(null);
                binding.tilPassword.setError(null);
                binding.tilConfirmPassword.setError(null);
            }
        };

        binding.etEmail.addTextChangedListener(clearErrorsWatcher);
        binding.etPassword.addTextChangedListener(clearErrorsWatcher);
        binding.etConfirmPassword.addTextChangedListener(clearErrorsWatcher);
    }

    private void attemptRegister() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.err_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.err_short_password));
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.err_password_mismatch));
            return;
        }

        setLoading(true);
        authRepository.registerWithEmail(email, password, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                new MaterialAlertDialogBuilder(RegisterActivity.this)
                        .setTitle(R.string.registration_success_title)
                        .setMessage(getString(R.string.registration_success_msg, email))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra(AuthExtras.EXTRA_ROLE, role);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Registration failed");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
        binding.tvGoToLogin.setEnabled(!loading);
    }
}
