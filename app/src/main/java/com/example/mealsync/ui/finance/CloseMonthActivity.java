package com.example.mealsync.ui.finance;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.R;
import com.example.mealsync.close.CloseStep;
import com.example.mealsync.close.PeriodDates;
import com.example.mealsync.data.models.Adjustment;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Meal;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.data.repository.AdjustmentRepository;
import com.example.mealsync.data.repository.DepositRepository;
import com.example.mealsync.data.repository.ExpenseRepository;
import com.example.mealsync.data.repository.FinanceRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.data.repository.PeriodCloseRepository;
import com.example.mealsync.data.repository.PeriodRepository;
import com.example.mealsync.databinding.ActivityCloseMonthBinding;
import com.example.mealsync.finance.FinanceResult;
import com.example.mealsync.finance.MemberFinanceResult;
import com.example.mealsync.util.MoneyUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CloseMonthActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";
    public static final String EXTRA_PERIOD_ID = "extra_period_id";

    private ActivityCloseMonthBinding binding;
    private final MessRepository messRepository = new MessRepository();
    private final PeriodRepository periodRepository = new PeriodRepository();
    private final PeriodCloseRepository closeRepository = new PeriodCloseRepository();
    private final FinanceRepository financeRepository = new FinanceRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final DepositRepository depositRepository = new DepositRepository();
    private final AdjustmentRepository adjustmentRepository = new AdjustmentRepository();

    private ListenerRegistration messListener;
    private ListenerRegistration membersListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration depositsListener;
    private ListenerRegistration adjustmentsListener;
    private ListenerRegistration mealsListener;
    private final List<Member> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final List<Deposit> deposits = new ArrayList<>();
    private final List<Adjustment> adjustments = new ArrayList<>();
    private final List<Meal> meals = new ArrayList<>();

    private String messId;
    private String periodId;
    private Mess currentMess;
    private Period currentPeriod;

    private String closeRequestId;
    private boolean isClosingInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityCloseMonthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);
        periodId = getIntent().getStringExtra(EXTRA_PERIOD_ID);
        closeRequestId = UUID.randomUUID().toString(); // Persistent for form session

        applyWindowInsets();
        setupListeners();
        loadData();
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

        binding.cbConfirm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.btnCloseMonthConfirm.setEnabled(isChecked && !isClosingInProgress);
        });

        binding.btnCloseMonthConfirm.setOnClickListener(v -> startMonthClose());
        binding.btnRetry.setOnClickListener(v -> startMonthClose());
    }

    private void loadData() {
        if (messId == null || periodId == null) return;

        messListener = messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                currentMess = mess;
                if (mess != null) {
                    periodRepository.getPeriod(messId, periodId, new PeriodRepository.PeriodCallback() {
                        @Override
                        public void onSuccess(Period period) {
                            currentPeriod = period;
                            if (period != null) {
                                observePreviewData();
                                renderPreview();
                            }
                        }

                        @Override
                        public void onFailure(Exception error) {
                            showPreviewError(error);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                showPreviewError(error);
            }
        });
    }

    private void observePreviewData() {
        removePreviewListeners();
        if (currentMess == null || currentPeriod == null) return;

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String startDate = PeriodDates.getFirstDay(periodId);
        String endDate = PeriodDates.getLastDay(periodId);

        membersListener = messRepository.observeMembers(messId, new MessRepository.MembersListener() {
            @Override
            public void onMembersUpdated(List<Member> updatedMembers) {
                members.clear();
                if (updatedMembers != null) members.addAll(updatedMembers);
                renderPreview();
            }

            @Override
            public void onError(Exception error) {
                showPreviewError(error);
            }
        });

        expensesListener = expenseRepository.observeExpenses(messId, periodId, new ExpenseRepository.ExpensesListener() {
            @Override
            public void onExpensesUpdated(List<Expense> updatedExpenses) {
                expenses.clear();
                if (updatedExpenses != null) expenses.addAll(updatedExpenses);
                renderPreview();
            }

            @Override
            public void onError(Exception error) {
                showPreviewError(error);
            }
        });

        depositsListener = depositRepository.observeDeposits(messId, periodId, true, currentUid,
                new DepositRepository.DepositsListener() {
                    @Override
                    public void onDepositsUpdated(List<Deposit> updatedDeposits) {
                        deposits.clear();
                        if (updatedDeposits != null) deposits.addAll(updatedDeposits);
                        renderPreview();
                    }

                    @Override
                    public void onError(Exception error) {
                        showPreviewError(error);
                    }
                });

        adjustmentsListener = adjustmentRepository.observeAdjustments(messId, periodId,
                new AdjustmentRepository.AdjustmentsListener() {
                    @Override
                    public void onAdjustmentsUpdated(List<Adjustment> updatedAdjustments) {
                        adjustments.clear();
                        if (updatedAdjustments != null) adjustments.addAll(updatedAdjustments);
                        renderPreview();
                    }

                    @Override
                    public void onError(Exception error) {
                        showPreviewError(error);
                    }
                });

        mealsListener = FirebaseFirestore.getInstance()
                .collection("messes").document(messId).collection("meals")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        showPreviewError(error);
                        return;
                    }
                    meals.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Meal meal = doc.toObject(Meal.class);
                            if (meal != null) meals.add(meal);
                        }
                    }
                    renderPreview();
                });
    }

    private void renderPreview() {
        if (currentPeriod == null || currentMess == null) return;

        String startDate = PeriodDates.getFirstDay(periodId);
        String endDate = PeriodDates.getLastDay(periodId);

        binding.tvPeriodDates.setText("Period: " + startDate + " to " + endDate);

        FinanceResult finRes = financeRepository.calculateLive(
                currentMess, currentPeriod,
                expenses, deposits, adjustments, members, meals
        );

        if (finRes != null) {
            String rateStr = finRes.isRateAvailable()
                    ? MoneyUtils.formatPaisa(Math.round(finRes.getRatePaisaPerMeal())) + " / meal"
                    : "Not set";
            binding.tvPreviewRate.setText("Fixed Meal Rate: " + rateStr);

            long totalMeals = finRes.getTotalCountedUnits() / 100;
            long mealCharges = finRes.isRateAvailable()
                    ? totalMeals * Math.round(finRes.getRatePaisaPerMeal()) : 0L;
            String totalsStr = "Meals: " + (finRes.getTotalCountedUnits() / 100)
                    + " | Meal Charges: " + MoneyUtils.formatPaisa(mealCharges)
                    + " | Fixed Expenses: " + MoneyUtils.formatPaisa(finRes.getTotalFixedPaisa());
            binding.tvPreviewTotals.setText(totalsStr);

            long sumSurplus = 0;
            long sumOwed = 0;
            for (MemberFinanceResult mRes : finRes.getMemberResults().values()) {
                long bal = mRes.getNetBalancePaisa();
                if (bal > 0) sumSurplus += bal;
                if (bal < 0) sumOwed += Math.abs(bal);
            }

            String memStr = "Members: " + finRes.getMemberResults().size()
                    + " | Total Surplus: " + MoneyUtils.formatPaisa(sumSurplus)
                    + " | Total Owed: " + MoneyUtils.formatPaisa(sumOwed);
            binding.tvPreviewMembers.setText(memStr);

            if (!finRes.isRateAvailable()) {
                binding.cardWarning.setVisibility(View.VISIBLE);
                binding.tvWarningText.setText("Set a fixed meal rate in Mess Settings before closing this period.");
            } else {
                binding.cardWarning.setVisibility(View.GONE);
            }
        }
    }

    private void showPreviewError(Exception error) {
        String message = error != null && error.getLocalizedMessage() != null
                ? error.getLocalizedMessage() : "Failed to load month preview";
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void removePreviewListeners() {
        if (membersListener != null) membersListener.remove();
        if (expensesListener != null) expensesListener.remove();
        if (depositsListener != null) depositsListener.remove();
        if (adjustmentsListener != null) adjustmentsListener.remove();
        if (mealsListener != null) mealsListener.remove();
    }

    private void startMonthClose() {
        if (currentMess == null || currentPeriod == null || isClosingInProgress) return;
        if (currentMess.getMealRatePaisa() <= 0) {
            Snackbar.make(binding.getRoot(), "Set a fixed meal rate in Mess Settings before closing this period.", Snackbar.LENGTH_LONG).show();
            return;
        }

        isClosingInProgress = true;
        binding.cbConfirm.setEnabled(false);
        binding.btnCloseMonthConfirm.setEnabled(false);
        binding.btnRetry.setVisibility(View.GONE);
        binding.layoutSteps.setVisibility(View.VISIBLE);

        closeRepository.executeCloseFlow(currentMess, currentPeriod, closeRequestId, new PeriodCloseRepository.CloseProgressCallback() {
            @Override
            public void onStepChanged(CloseStep step, String message) {
                updateStepUI(step, message);
            }

            @Override
            public void onSuccess(String nextPeriodId) {
                isClosingInProgress = false;
                Snackbar.make(binding.getRoot(), "Month closed successfully!", Snackbar.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception error) {
                isClosingInProgress = false;
                binding.cbConfirm.setEnabled(true);
                binding.btnCloseMonthConfirm.setEnabled(true);
                binding.btnRetry.setVisibility(View.VISIBLE);
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to close month");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void updateStepUI(CloseStep step, String message) {
        int teal = ContextCompat.getColor(this, R.color.ms_teal);
        int gray = ContextCompat.getColor(this, R.color.ms_text_secondary);

        binding.tvStep1.setTextColor(step.ordinal() >= CloseStep.LOCK.ordinal() ? teal : gray);
        binding.tvStep2.setTextColor(step.ordinal() >= CloseStep.CALCULATE.ordinal() ? teal : gray);
        binding.tvStep3.setTextColor(step.ordinal() >= CloseStep.WRITE_RESULTS.ordinal() ? teal : gray);
        binding.tvStep4.setTextColor(step.ordinal() >= CloseStep.WRITE_NEXT_PERIOD.ordinal() ? teal : gray);
        binding.tvStep5.setTextColor(step.ordinal() >= CloseStep.FINALIZE.ordinal() ? teal : gray);
    }

    @Override
    protected void onDestroy() {
        if (messListener != null) messListener.remove();
        removePreviewListeners();
        super.onDestroy();
    }
}
