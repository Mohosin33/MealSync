package com.example.mealsync.ui.mess;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivitySettingsBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.util.MoneyUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";

    private ActivitySettingsBinding binding;
    private final MessRepository messRepository = new MessRepository();
    private String messId;
    private int selectedHour = 21;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);

        applyWindowInsets();
        setupListeners();
        loadMessData();
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
        binding.etCutoffTime.setOnClickListener(v -> showTimePicker());
        binding.tilCutoffTime.setOnClickListener(v -> showTimePicker());
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());

        SimpleTextWatcher clearWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilMessName.setError(null);
                binding.tilTimezone.setError(null);
                binding.tilMealRate.setError(null);
            }
        };
        binding.etMessName.addTextChangedListener(clearWatcher);
        binding.etTimezone.addTextChangedListener(clearWatcher);
        binding.etMealRate.addTextChangedListener(clearWatcher);
    }

    private void loadMessData() {
        if (messId == null || messId.trim().isEmpty()) {
            return;
        }
        setLoading(true);
        messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                setLoading(false);
                if (mess != null) {
                    if (binding.etMessName.getText() == null || binding.etMessName.getText().toString().isEmpty()) {
                        binding.etMessName.setText(mess.getName());
                    }
                    if (binding.etTimezone.getText() == null || binding.etTimezone.getText().toString().isEmpty()) {
                        binding.etTimezone.setText(mess.getTimezone());
                    }
                    if (binding.etCutoffTime.getText() == null || binding.etCutoffTime.getText().toString().isEmpty()) {
                        binding.etCutoffTime.setText(mess.getCutoffTime());
                        parseCutoffTime(mess.getCutoffTime());
                    }
                    if (binding.etMealRate.getText() == null || binding.etMealRate.getText().toString().isEmpty()) {
                        binding.etMealRate.setText(mess.getMealRatePaisa() > 0
                                ? String.format(Locale.US, "%.2f", mess.getMealRatePaisa() / 100.0)
                                : "");
                    }
                }
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
            }
        });
    }

    private void parseCutoffTime(String cutoffTime) {
        if (cutoffTime == null || !cutoffTime.contains(":")) return;
        try {
            String[] parts = cutoffTime.trim().split(":");
            selectedHour = Integer.parseInt(parts[0]);
            selectedMinute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
        }
    }

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText(R.string.select_cutoff_time)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();
            String formatted = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
            binding.etCutoffTime.setText(formatted);
        });

        picker.show(getSupportFragmentManager(), "cutoff_time_picker");
    }

    private void saveSettings() {
        String name = binding.etMessName.getText() != null ? binding.etMessName.getText().toString().trim() : "";
        String timezone = binding.etTimezone.getText() != null ? binding.etTimezone.getText().toString().trim() : "";
        String cutoffTime = binding.etCutoffTime.getText() != null ? binding.etCutoffTime.getText().toString().trim() : "";
        String mealRateText = binding.etMealRate.getText() != null ? binding.etMealRate.getText().toString().trim() : "";
        long mealRatePaisa;

        if (name.isEmpty()) {
            binding.tilMessName.setError(getString(R.string.err_empty_mess_name));
            return;
        }
        if (timezone.isEmpty()) {
            timezone = "Asia/Dhaka";
        }
        if (cutoffTime.isEmpty()) {
            cutoffTime = "21:00";
        }
        try {
            mealRatePaisa = MoneyUtils.parseTakaToPaisa(mealRateText);
        } catch (IllegalArgumentException e) {
            binding.tilMealRate.setError(getString(R.string.err_invalid_meal_rate));
            return;
        }

        setLoading(true);
        messRepository.updateMessSettings(messId, name, timezone, cutoffTime, mealRatePaisa, new MessRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception error) {
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to save");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveSettings.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
    }
}
