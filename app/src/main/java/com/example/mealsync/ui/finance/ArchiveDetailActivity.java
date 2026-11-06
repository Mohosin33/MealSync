package com.example.mealsync.ui.finance;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mealsync.R;
import com.example.mealsync.close.MemberResult;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.ExpenseRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.data.repository.PeriodRepository;
import com.example.mealsync.databinding.ActivityArchiveDetailBinding;
import com.example.mealsync.finance.MemberFinanceResult;
import com.example.mealsync.ui.mess.MemberSummaryAdapter;
import com.example.mealsync.util.MoneyUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";
    public static final String EXTRA_PERIOD_ID = "extra_period_id";

    private ActivityArchiveDetailBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthRepository authRepository = new AuthRepository();
    private final PeriodRepository periodRepository = new PeriodRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final MessRepository messRepository = new MessRepository();

    private String messId;
    private String periodId;
    private Period period;
    private boolean isManager = false;

    private MemberSummaryAdapter memberSummaryAdapter;
    private ExpenseAdapter expenseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityArchiveDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);
        periodId = getIntent().getStringExtra(EXTRA_PERIOD_ID);

        applyWindowInsets();
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        memberSummaryAdapter = new MemberSummaryAdapter();
        binding.rvArchiveMemberResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArchiveMemberResults.setAdapter(memberSummaryAdapter);

        expenseAdapter = new ExpenseAdapter(false, null);
        binding.rvArchiveExpenses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArchiveExpenses.setAdapter(expenseAdapter);

        loadReport();
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

    private void loadReport() {
        if (messId == null || periodId == null) return;

        FirebaseUser user = authRepository.getCurrentUser();
        String currentUid = user != null ? user.getUid() : "";

        messRepository.getUserProfile(currentUid, new MessRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                isManager = profile != null && "manager".equalsIgnoreCase(profile.getRole());

                periodRepository.getPeriod(messId, periodId, new PeriodRepository.PeriodCallback() {
                    @Override
                    public void onSuccess(Period p) {
                        period = p;
                        if (period != null) {
                            renderHeaderAndTotals();
                            loadMemberResults(currentUid);
                            loadExpenses();
                        }
                    }

                    @Override
                    public void onFailure(Exception error) {
                    }
                });
            }

            @Override
            public void onFailure(Exception error) {
            }
        });
    }

    private void renderHeaderAndTotals() {
        binding.tvArchiveDetailTitle.setText(period.getPeriodId() + " Monthly Report");

        Long rate = period.getFinalRatePaisa();
        String rateStr = rate != null ? MoneyUtils.formatPaisa(rate) + " / meal" : "Rate unavailable";
        binding.tvArchiveDetailRate.setText(rateStr);

        Map<String, Long> totals = period.getTotals();
        long meals = totals != null && totals.containsKey("totalMeals") ? totals.get("totalMeals") : 0L;
        long storedVariablePaisa = totals != null && totals.containsKey("totalVariablePaisa") ? totals.get("totalVariablePaisa") : 0L;
        long mealChargesPaisa = storedVariablePaisa > 0
                ? storedVariablePaisa
                : rate != null ? rate * meals : 0L;
        long fixPaisa = totals != null && totals.containsKey("totalFixedPaisa") ? totals.get("totalFixedPaisa") : 0L;
        long depPaisa = totals != null && totals.containsKey("totalDepositsPaisa") ? totals.get("totalDepositsPaisa") : 0L;

        String totalsStr = getString(R.string.label_archive_detail_totals, meals,
                MoneyUtils.formatPaisa(mealChargesPaisa), MoneyUtils.formatPaisa(fixPaisa),
                MoneyUtils.formatPaisa(depPaisa));
        binding.tvArchiveDetailTotals.setText(totalsStr);

        Map<String, Long> unallocated = period.getUnallocated();
        long unVar = unallocated != null && unallocated.containsKey("unallocatedVariablePaisa") ? unallocated.get("unallocatedVariablePaisa") : 0L;
        if (unVar > 0) {
            binding.tvArchiveDetailUnallocated.setVisibility(android.view.View.VISIBLE);
            binding.tvArchiveDetailUnallocated.setText(getString(R.string.label_unallocated_expenses,
                    MoneyUtils.formatPaisa(unVar)));
        } else {
            binding.tvArchiveDetailUnallocated.setVisibility(android.view.View.GONE);
        }
    }

    private void loadMemberResults(String currentUid) {
        db.collection("messes").document(messId)
                .collection("periods").document(periodId)
                .collection("results").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<MemberFinanceResult> finResults = new ArrayList<>();
                        Map<String, String> namesMap = new HashMap<>();

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            MemberResult res = doc.toObject(MemberResult.class);
                            if (res != null) {
                                // Manager sees all, member sees ONLY their own!
                                if (isManager || currentUid.equals(res.getUid())) {
                                    MemberFinanceResult mFin = new MemberFinanceResult(
                                            res.getUid(), res.getLunches(), res.getDinners(), res.getGuests(),
                                            res.getCountedUnits(), res.getMealCostPaisa(), res.getFixedSharePaisa(),
                                            res.getOpeningBalancePaisa(), res.getTotalDepositsPaisa(),
                                            res.getCreditsPaisa(), res.getDebitsPaisa(), res.getClosingBalancePaisa()
                                    );
                                    finResults.add(mFin);
                                    namesMap.put(res.getUid(), res.getName() != null ? res.getName() : "Member");
                                }
                            }
                        }
                        memberSummaryAdapter.setResults(finResults, namesMap);
                    }
                });
    }

    private void loadExpenses() {
        expenseRepository.observeExpenses(messId, periodId, new ExpenseRepository.ExpensesListener() {
            @Override
            public void onExpensesUpdated(List<Expense> expenses) {
                expenseAdapter.setExpenses(expenses, null);
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }
}
