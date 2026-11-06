package com.example.mealsync.ui.finance;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.repository.ExpenseRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityAddExpenseBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.ui.common.StickyHeaderBehavior;
import com.example.mealsync.util.CutoffCalculator;
import com.example.mealsync.util.CutoffResult;
import com.example.mealsync.util.MoneyUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";
    public static final String EXTRA_CORRECTS_ID = "extra_corrects_id";

    private ActivityAddExpenseBinding binding;
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final MessRepository messRepository = new MessRepository();

    private String messId;
    private String correctsId;
    private Mess currentMess;
    private boolean isSaving = false;
    private static final String[] CATEGORY_VALUES = new String[]{
            "groceries", "utilities", "wi_fi", "cooks_salary", "cleaning", "maintenance", "other"
    };
    private String[] categoryLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);
        correctsId = getIntent().getStringExtra(EXTRA_CORRECTS_ID);

        applyWindowInsets();
        StickyHeaderBehavior.attach(binding.scrollContent, binding.header, R.drawable.bg_gradient_manager);
        setupDropdown();
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

    private void setupDropdown() {
        categoryLabels = new String[]{
                getString(R.string.category_groceries),
                getString(R.string.category_utilities),
                getString(R.string.category_wifi),
                getString(R.string.category_cooks_salary),
                getString(R.string.category_cleaning),
                getString(R.string.category_maintenance),
                getString(R.string.category_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryLabels);
        binding.actCategory.setAdapter(adapter);
        binding.actCategory.setText(categoryLabels[0], false);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.tilDate.setOnClickListener(v -> showDatePicker());
        binding.btnSaveExpense.setOnClickListener(v -> saveExpense());

        binding.etAmount.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                binding.tilAmount.setError(null);
            }
        });
    }

    private void loadMessData() {
        if (messId == null) return;
        messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                currentMess = mess;
                if (mess != null && (binding.etDate.getText() == null || binding.etDate.getText().toString().isEmpty())) {
                    CutoffResult cutoffRes = CutoffCalculator.calculate(Instant.now(), mess.getTimezone(), mess.getCutoffTime());
                    binding.etDate.setText(cutoffRes.getTodayDate());
                }
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Expense Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate localDate = Instant.ofEpochMilli(selection).atZone(ZoneId.of("UTC")).toLocalDate();
                binding.etDate.setText(localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        });

        datePicker.show(getSupportFragmentManager(), "expense_date_picker");
    }

    private void saveExpense() {
        if (isSaving) return;

        String amountStr = binding.etAmount.getText() != null ? binding.etAmount.getText().toString().trim() : "";
        String selectedCategory = binding.actCategory.getText() != null
                ? binding.actCategory.getText().toString().trim() : "";
        int categoryIndex = -1;
        for (int i = 0; i < categoryLabels.length; i++) {
            if (categoryLabels[i].equals(selectedCategory)) {
                categoryIndex = i;
                break;
            }
        }
        if (categoryIndex < 0) {
            binding.tilCategory.setError(getString(R.string.err_select_category));
            return;
        }
        binding.tilCategory.setError(null);
        String category = CATEGORY_VALUES[categoryIndex];
        String description = categoryLabels[categoryIndex];
        String dateStr = binding.etDate.getText() != null ? binding.etDate.getText().toString().trim() : "";

        long amountPaisa;
        try {
            amountPaisa = MoneyUtils.parseTakaToPaisa(amountStr);
        } catch (IllegalArgumentException e) {
            binding.tilAmount.setError(getString(R.string.err_generic, "Invalid amount"));
            return;
        }

        if (currentMess == null || currentMess.getCurrentPeriodId() == null) {
            Snackbar.make(binding.getRoot(), R.string.period_not_initialized, Snackbar.LENGTH_LONG).show();
            return;
        }

        isSaving = true;
        setLoading(true);

        String id = UUID.randomUUID().toString();
        String type = "fixed";
        String periodId = currentMess.getCurrentPeriodId();

        Expense expense = new Expense(id, amountPaisa, description, category, type, dateStr, periodId, null, correctsId);

        expenseRepository.addExpense(messId, expense, new ExpenseRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isSaving = false;
                setLoading(false);
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception error) {
                isSaving = false;
                setLoading(false);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to save expense");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveExpense.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
    }
}
