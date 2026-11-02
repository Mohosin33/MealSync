package com.example.mealsync.ui.mess;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.MainActivity;
import com.example.mealsync.R;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityCreateMessBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.google.android.material.snackbar.Snackbar;

public class CreateMessActivity extends AppCompatActivity {

    private ActivityCreateMessBinding binding;
    private final MessRepository messRepository = new MessRepository();
    private String generatedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityCreateMessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

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
        binding.btnCreateMess.setOnClickListener(v -> attemptCreateMess());

        binding.btnCopyCode.setOnClickListener(v -> copyCodeToClipboard());
        binding.btnShareCode.setOnClickListener(v -> shareCode());
        binding.btnGoToDashboard.setOnClickListener(v -> navigateToMain());

        SimpleTextWatcher clearErrorsWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilMessName.setError(null);
                binding.tilManagerName.setError(null);
            }
        };
        binding.etMessName.addTextChangedListener(clearErrorsWatcher);
        binding.etManagerName.addTextChangedListener(clearErrorsWatcher);
    }

    private void attemptCreateMess() {
        String messName = binding.etMessName.getText() != null ? binding.etMessName.getText().toString().trim() : "";
        String managerName = binding.etManagerName.getText() != null ? binding.etManagerName.getText().toString().trim() : "";
        String timezone = binding.etTimezone.getText() != null ? binding.etTimezone.getText().toString().trim() : "Asia/Dhaka";
        String cutoffTime = binding.etCutoffTime.getText() != null ? binding.etCutoffTime.getText().toString().trim() : "21:00";

        if (messName.isEmpty()) {
            binding.tilMessName.setError(getString(R.string.err_empty_mess_name));
            return;
        }
        if (managerName.isEmpty()) {
            binding.tilManagerName.setError(getString(R.string.err_empty_manager_name));
            return;
        }
        if (timezone.isEmpty()) {
            timezone = "Asia/Dhaka";
        }
        if (cutoffTime.isEmpty()) {
            cutoffTime = "21:00";
        }

        setLoading(true);
        messRepository.createMess(messName, managerName, timezone, cutoffTime, new MessRepository.CreateMessCallback() {
            @Override
            public void onSuccess(Mess mess, String code) {
                setLoading(false);
                generatedCode = code;
                showSuccessCode(code);
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to create mess");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccessCode(String code) {
        binding.panelForm.setVisibility(View.GONE);
        binding.tvGeneratedCode.setText(code);
        binding.panelSuccess.setVisibility(View.VISIBLE);
    }

    private void copyCodeToClipboard() {
        if (generatedCode == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invitation Code", generatedCode);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Snackbar.make(binding.getRoot(), R.string.code_copied, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareCode() {
        if (generatedCode == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, generatedCode));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnCreateMess.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
    }
}
