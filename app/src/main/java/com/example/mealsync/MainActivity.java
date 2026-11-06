package com.example.mealsync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mealsync.close.PeriodDates;
import com.example.mealsync.data.models.Adjustment;
import com.example.mealsync.data.models.DayTotals;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Meal;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AdjustmentRepository;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.DepositRepository;
import com.example.mealsync.data.repository.ExpenseRepository;
import com.example.mealsync.data.repository.FinanceRepository;
import com.example.mealsync.data.repository.MealRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.data.repository.PeriodRepository;
import com.example.mealsync.databinding.ActivityMainBinding;
import com.example.mealsync.finance.FinanceResult;
import com.example.mealsync.finance.MemberFinanceResult;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.example.mealsync.ui.finance.AddDepositActivity;
import com.example.mealsync.ui.finance.AddExpenseActivity;
import com.example.mealsync.ui.finance.ArchiveAdapter;
import com.example.mealsync.ui.finance.ArchiveDetailActivity;
import com.example.mealsync.ui.finance.CloseMonthActivity;
import com.example.mealsync.ui.finance.DepositAdapter;
import com.example.mealsync.ui.finance.ExpenseAdapter;
import com.example.mealsync.ui.mess.ChooseActivity;
import com.example.mealsync.ui.mess.MealHistoryAdapter;
import com.example.mealsync.ui.mess.MemberAdapter;
import com.example.mealsync.ui.mess.SettingsActivity;
import com.example.mealsync.util.CutoffCalculator;
import com.example.mealsync.util.CutoffResult;
import com.example.mealsync.util.MoneyUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements MemberAdapter.OnRemoveMemberClickListener {

    private ActivityMainBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private final MessRepository messRepository = new MessRepository();
    private final MealRepository mealRepository = new MealRepository();
    private final PeriodRepository periodRepository = new PeriodRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final DepositRepository depositRepository = new DepositRepository();
    private final AdjustmentRepository adjustmentRepository = new AdjustmentRepository();
    private final FinanceRepository financeRepository = new FinanceRepository();

    private UserProfile currentProfile;
    private Mess currentMess;
    private Period currentPeriod;

    private MemberAdapter memberAdapter;
    private MealHistoryAdapter mealHistoryAdapter;
    private ExpenseAdapter expenseAdapter;
    private DepositAdapter depositAdapter;
    private ArchiveAdapter archiveAdapter;

    private ListenerRegistration messListener;
    private ListenerRegistration membersListener;
    private ListenerRegistration myMealListener;
    private ListenerRegistration myMealsListener;
    private ListenerRegistration dayTotalsListener;

    private ListenerRegistration expensesListener;
    private ListenerRegistration depositsListener;
    private ListenerRegistration adjustmentsListener;
    private ListenerRegistration periodMealsListener;
    private ListenerRegistration archiveListener;

    private CutoffResult currentCutoffResult;
    private Meal tomorrowMeal;
    private boolean isUpdatingControls = false;
    private boolean isSavingMeal = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = this::updateCutoffTimer;

    private final Map<String, String> memberNamesMap = new HashMap<>();
    private final List<Member> activeMemberList = new ArrayList<>();
    private final List<Meal> personalMealsList = new ArrayList<>();
    private final List<Expense> currentExpensesList = new ArrayList<>();
    private final List<Deposit> currentDepositsList = new ArrayList<>();
    private final List<Adjustment> currentAdjustmentsList = new ArrayList<>();
    private final List<Meal> currentPeriodMealsList = new ArrayList<>();

    private final Set<String> reversedExpenseIds = new HashSet<>();
    private final Set<String> reversedDepositIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());

        FirebaseUser firebaseUser = authRepository.getCurrentUser();
        if (firebaseUser == null) {
            openWelcome();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        binding.header.setBackgroundResource(R.drawable.bg_gradient_member);
        setupNavigation();
        setupListeners();
        setupFinanceSubTabs();
        updateThemeToggleIcon();
        binding.btnThemeToggle.setOnClickListener(v ->
                MealSyncApplication.setDarkMode(this, !MealSyncApplication.isDarkMode(this)));
        loadUserProfile(firebaseUser.getUid());
    }

    private void updateThemeToggleIcon() {
        boolean darkMode = MealSyncApplication.isDarkMode(this);
        binding.btnThemeToggle.setImageResource(darkMode ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon);
        binding.btnThemeToggle.setContentDescription(getString(
                darkMode ? R.string.switch_to_light_mode : R.string.switch_to_dark_mode));
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            binding.containerContent.setPadding(bars.left, bars.top, bars.right, 0);
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
    }

    private void setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showTab(R.id.tabHome);
                return true;
            } else if (itemId == R.id.nav_meals) {
                showTab(R.id.tabMeals);
                return true;
            } else if (itemId == R.id.nav_money) {
                showTab(R.id.tabMoney);
                return true;
            } else if (itemId == R.id.nav_members) {
                showTab(R.id.tabMembers);
                return true;
            } else if (itemId == R.id.nav_profile) {
                showTab(R.id.tabProfile);
                return true;
            }
            return false;
        });
    }

    private void showTab(int tabId) {
        binding.tabHome.setVisibility(tabId == R.id.tabHome ? View.VISIBLE : View.GONE);
        binding.header.setVisibility(tabId == R.id.tabHome ? View.VISIBLE : View.GONE);
        binding.tabMeals.setVisibility(tabId == R.id.tabMeals ? View.VISIBLE : View.GONE);
        binding.tabMoney.setVisibility(tabId == R.id.tabMoney ? View.VISIBLE : View.GONE);
        binding.tabMembers.setVisibility(tabId == R.id.tabMembers ? View.VISIBLE : View.GONE);
        binding.tabProfile.setVisibility(tabId == R.id.tabProfile ? View.VISIBLE : View.GONE);
    }

    private void setupFinanceSubTabs() {
        showMoneySubPanel(R.id.panelSubExpenses);
        binding.btnSubExpenses.setOnClickListener(v -> showMoneySubPanel(R.id.panelSubExpenses));
        binding.btnSubDeposits.setOnClickListener(v -> showMoneySubPanel(R.id.panelSubDeposits));
        binding.btnSubSummary.setOnClickListener(v -> showMoneySubPanel(R.id.panelSubSummary));
        binding.btnSubArchive.setOnClickListener(v -> {
            showMoneySubPanel(R.id.panelSubArchive);
            loadArchivePeriods();
        });

        binding.fabAddExpense.setOnClickListener(v -> {
            if (currentMess != null) {
                Intent intent = new Intent(this, AddExpenseActivity.class);
                intent.putExtra(AddExpenseActivity.EXTRA_MESS_ID, currentMess.getMessId());
                startActivity(intent);
            }
        });

        binding.fabAddDeposit.setOnClickListener(v -> {
            if (currentMess != null) {
                Intent intent = new Intent(this, AddDepositActivity.class);
                intent.putExtra(AddDepositActivity.EXTRA_MESS_ID, currentMess.getMessId());
                startActivity(intent);
            }
        });

        binding.btnCloseMonth.setOnClickListener(v -> openCloseMonthActivity());

        binding.cardClosingBanner.setOnClickListener(v -> {
            if (checkIsManager()) {
                openCloseMonthActivity();
            }
        });
    }

    private boolean checkIsManager() {
        FirebaseUser user = authRepository.getCurrentUser();
        String currentUid = user != null ? user.getUid() : "";
        if (currentMess != null && currentMess.getManagerUid() != null && currentMess.getManagerUid().equals(currentUid)) {
            return true;
        }
        return currentProfile != null && "manager".equalsIgnoreCase(currentProfile.getRole());
    }

    private void openCloseMonthActivity() {
        if (currentMess != null && currentPeriod != null) {
            Intent intent = new Intent(this, CloseMonthActivity.class);
            intent.putExtra(CloseMonthActivity.EXTRA_MESS_ID, currentMess.getMessId());
            intent.putExtra(CloseMonthActivity.EXTRA_PERIOD_ID, currentPeriod.getPeriodId());
            startActivity(intent);
        } else if (currentMess != null && currentMess.getCurrentPeriodId() != null) {
            Intent intent = new Intent(this, CloseMonthActivity.class);
            intent.putExtra(CloseMonthActivity.EXTRA_MESS_ID, currentMess.getMessId());
            intent.putExtra(CloseMonthActivity.EXTRA_PERIOD_ID, currentMess.getCurrentPeriodId());
            startActivity(intent);
        }
    }

    private void showMoneySubPanel(int panelId) {
        binding.panelSubExpenses.setVisibility(panelId == R.id.panelSubExpenses ? View.VISIBLE : View.GONE);
        binding.panelSubDeposits.setVisibility(panelId == R.id.panelSubDeposits ? View.VISIBLE : View.GONE);
        binding.panelSubSummary.setVisibility(panelId == R.id.panelSubSummary ? View.VISIBLE : View.GONE);
        binding.panelSubArchive.setVisibility(panelId == R.id.panelSubArchive ? View.VISIBLE : View.GONE);
        binding.btnSubExpenses.setSelected(panelId == R.id.panelSubExpenses);
        binding.btnSubDeposits.setSelected(panelId == R.id.panelSubDeposits);
        binding.btnSubSummary.setSelected(panelId == R.id.panelSubSummary);
        binding.btnSubArchive.setSelected(panelId == R.id.panelSubArchive);
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            openWelcome();
        });

        binding.btnSettings.setOnClickListener(v -> {
            if (currentMess != null) {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_MESS_ID, currentMess.getMessId());
                startActivity(intent);
            }
        });

        binding.btnCopyCode.setOnClickListener(v -> copyInvitationCode());
        binding.btnShareCode.setOnClickListener(v -> shareInvitationCode());
        binding.btnRevokeCode.setOnClickListener(v -> revokeInvitationCode());
        binding.btnRegenerateCode.setOnClickListener(v -> regenerateInvitationCode());

        binding.switchLunch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingControls && tomorrowMeal != null) {
                submitMealChange(isChecked, tomorrowMeal.isDinner(), tomorrowMeal.getGuests());
            }
        });

        binding.switchDinner.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingControls && tomorrowMeal != null) {
                submitMealChange(tomorrowMeal.isLunch(), isChecked, tomorrowMeal.getGuests());
            }
        });

        binding.btnGuestMinus.setOnClickListener(v -> {
            if (!isUpdatingControls && tomorrowMeal != null && tomorrowMeal.getGuests() > 0) {
                submitMealChange(tomorrowMeal.isLunch(), tomorrowMeal.isDinner(), tomorrowMeal.getGuests() - 1);
            }
        });

        binding.btnGuestPlus.setOnClickListener(v -> {
            if (!isUpdatingControls && tomorrowMeal != null && tomorrowMeal.getGuests() < 10) {
                submitMealChange(tomorrowMeal.isLunch(), tomorrowMeal.isDinner(), tomorrowMeal.getGuests() + 1);
            }
        });

        mealHistoryAdapter = new MealHistoryAdapter();
        binding.rvMealHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMealHistory.setAdapter(mealHistoryAdapter);
    }

    private void loadUserProfile(String uid) {
        messRepository.getUserProfile(uid, new MessRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile == null || profile.getMessId() == null || profile.getMessId().trim().isEmpty()) {
                    openChooseScreen();
                    return;
                }
                currentProfile = profile;
                populateProfileUI(profile);
                setupFinanceAdapters();
                observeMessData(profile.getMessId());
            }

            @Override
            public void onFailure(Exception error) {
                openChooseScreen();
            }
        });
    }

    private void setupFinanceAdapters() {
        boolean isManager = checkIsManager();

        expenseAdapter = new ExpenseAdapter(isManager, this::onExpenseLongClick);
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExpenses.setAdapter(expenseAdapter);

        depositAdapter = new DepositAdapter(isManager, this::onDepositLongClick);
        binding.rvDeposits.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDeposits.setAdapter(depositAdapter);

        archiveAdapter = new ArchiveAdapter(isManager, new ArchiveAdapter.OnArchiveClickListener() {
            @Override
            public void onArchiveClick(Period p) {
                if (currentMess != null) {
                    Intent intent = new Intent(MainActivity.this, ArchiveDetailActivity.class);
                    intent.putExtra(ArchiveDetailActivity.EXTRA_MESS_ID, currentMess.getMessId());
                    intent.putExtra(ArchiveDetailActivity.EXTRA_PERIOD_ID, p.getPeriodId());
                    startActivity(intent);
                }
            }

            @Override
            public void onArchiveLongClick(Period p) {
                if (currentMess == null || p == null) return;
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.delete_archive_title)
                        .setMessage(getString(R.string.delete_archive_prompt, p.getPeriodId()))
                        .setPositiveButton(R.string.delete_action, (dialog, which) -> {
                            periodRepository.deleteArchivedPeriod(currentMess.getMessId(), p.getPeriodId(), new PeriodRepository.ActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Snackbar.make(binding.mainRoot, R.string.archive_deleted, Snackbar.LENGTH_SHORT).show();
                                    loadArchivePeriods();
                                }

                                @Override
                                public void onFailure(Exception error) {
                                    String msg = error != null && error.getLocalizedMessage() != null
                                            ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to delete archive");
                                    Snackbar.make(binding.mainRoot, msg, Snackbar.LENGTH_LONG).show();
                                }
                            });
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        binding.rvArchive.setLayoutManager(new LinearLayoutManager(this));
        binding.rvArchive.setAdapter(archiveAdapter);

        binding.fabAddExpense.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.fabAddDeposit.setVisibility(View.VISIBLE);
    }

    private void observeMessData(String messId) {
        if (messListener != null) messListener.remove();
        if (membersListener != null) membersListener.remove();

        messListener = messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                currentMess = mess;
                boolean isManager = checkIsManager();

                if (mess != null && isManager) {
                    if (mess.getUtcOffsetMinutes() == 0) {
                        messRepository.ensureMessSetup(mess, null);
                    }
                    if (mess.getCurrentPeriodId() == null) {
                        periodRepository.ensurePeriodSetup(mess, null);
                    }
                }

                populateMessUI(mess);
                updateMealDashboard();
                updatePeriodStatusUI(currentPeriod);
                if (mess != null && mess.getCurrentPeriodId() != null) {
                    loadPeriodAndFinance(mess);
                }
            }

            @Override
            public void onError(Exception error) {
                showFinanceLoadError(error);
            }
        });

        boolean isManager = checkIsManager();
        String currentUid = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUid() : "";

        memberAdapter = new MemberAdapter(isManager, currentUid, this);
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMembers.setAdapter(memberAdapter);

        membersListener = messRepository.observeMembers(messId, new MessRepository.MembersListener() {
            @Override
            public void onMembersUpdated(List<Member> members) {
                if (memberAdapter != null) {
                    memberAdapter.setMembers(members);
                }
                memberNamesMap.clear();
                activeMemberList.clear();
                if (members != null) {
                    for (Member m : members) {
                        activeMemberList.add(m);
                        memberNamesMap.put(m.getUid(), m.getName() != null ? m.getName() : "Member");
                    }
                }
                if (depositAdapter != null) {
                    depositAdapter.setDeposits(currentDepositsList, reversedDepositIds, memberNamesMap);
                }
                recalculateAndRenderFinance();
            }

            @Override
            public void onError(Exception error) {
                showFinanceLoadError(error);
            }
        });

        observePersonalMealHistory(messId, currentUid);
    }

    private void loadPeriodAndFinance(Mess mess) {
        periodRepository.getPeriod(mess.getMessId(), mess.getCurrentPeriodId(), new PeriodRepository.PeriodCallback() {
            @Override
            public void onSuccess(Period period) {
                if (period != null) {
                    currentPeriod = period;
                    updatePeriodStatusUI(period);
                    observeFinanceLedger(mess, period);
                    loadPeriodMeals(mess, period);
                } else {
                    periodRepository.ensurePeriodSetup(mess, new PeriodRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            periodRepository.getPeriod(mess.getMessId(), mess.getCurrentPeriodId(), new PeriodRepository.PeriodCallback() {
                                @Override
                                public void onSuccess(Period p2) {
                                    if (p2 != null) {
                                        currentPeriod = p2;
                                        updatePeriodStatusUI(p2);
                                        observeFinanceLedger(mess, p2);
                                        loadPeriodMeals(mess, p2);
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
            }

            @Override
            public void onFailure(Exception error) {
            }
        });
    }

    private void updatePeriodStatusUI(Period period) {
        boolean isManager = checkIsManager();
        String status = period != null && period.getStatus() != null ? period.getStatus() : "open";

        if ("closing".equalsIgnoreCase(status)) {
            binding.cardClosingBanner.setVisibility(View.VISIBLE);
            binding.fabAddExpense.setVisibility(View.GONE);
            binding.fabAddDeposit.setVisibility(View.GONE);
            binding.layoutManagerCloseMonth.setVisibility(View.GONE);
        } else if ("closed".equalsIgnoreCase(status)) {
            binding.cardClosingBanner.setVisibility(View.GONE);
            binding.fabAddExpense.setVisibility(View.GONE);
            binding.fabAddDeposit.setVisibility(View.GONE);
            binding.layoutManagerCloseMonth.setVisibility(View.GONE);
        } else {
            // OPEN
            binding.cardClosingBanner.setVisibility(View.GONE);
            binding.fabAddExpense.setVisibility(isManager ? View.VISIBLE : View.GONE);
            binding.fabAddDeposit.setVisibility(View.VISIBLE);

            if (isManager) {
                binding.layoutManagerCloseMonth.setVisibility(View.VISIBLE);
                String today = CutoffCalculator.calculate(Instant.now(), currentMess != null ? currentMess.getTimezone() : "Asia/Dhaka", currentMess != null ? currentMess.getCutoffTime() : "21:00").getTodayDate();
                String currentMonthId = today.substring(0, 7);
                String currentMonthLastDate = PeriodDates.getLastDay(currentMonthId);
                boolean canClose = PeriodDates.canClose(today, currentMonthLastDate);

                binding.btnCloseMonth.setEnabled(canClose);
                if (canClose) {
                    binding.tvCloseMonthSubtitle.setVisibility(View.GONE);
                } else {
                    binding.tvCloseMonthSubtitle.setVisibility(View.VISIBLE);
                    binding.tvCloseMonthSubtitle.setText(getString(R.string.close_month_earliest_date, currentMonthLastDate));
                }
            } else {
                binding.layoutManagerCloseMonth.setVisibility(View.GONE);
            }
        }
    }

    private void loadArchivePeriods() {
        if (currentMess == null) return;
        if (archiveListener != null) archiveListener.remove();

        archiveListener = FirebaseFirestore.getInstance()
                .collection("messes").document(currentMess.getMessId()).collection("periods")
                .whereEqualTo("status", "closed")
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null) {
                        List<Period> closedPeriods = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Period p = doc.toObject(Period.class);
                            if (p != null) closedPeriods.add(p);
                        }
                        archiveAdapter.setPeriods(closedPeriods);
                        binding.tvArchiveEmpty.setVisibility(closedPeriods.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void observeFinanceLedger(Mess mess, Period period) {
        if (expensesListener != null) expensesListener.remove();
        if (depositsListener != null) depositsListener.remove();
        if (adjustmentsListener != null) adjustmentsListener.remove();

        String messId = mess.getMessId();
        String periodId = period.getPeriodId();
        boolean isManager = checkIsManager();
        String currentUid = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUid() : "";

        expensesListener = expenseRepository.observeExpenses(messId, periodId, new ExpenseRepository.ExpensesListener() {
            @Override
            public void onExpensesUpdated(List<Expense> expenses) {
                currentExpensesList.clear();
                if (expenses != null) currentExpensesList.addAll(expenses);
                if (expenseAdapter != null) {
                    expenseAdapter.setExpenses(currentExpensesList, reversedExpenseIds);
                }
                recalculateAndRenderFinance();
            }

            @Override
            public void onError(Exception error) {
                showFinanceLoadError(error);
            }
        });

        depositsListener = depositRepository.observeDeposits(messId, periodId, isManager, currentUid, new DepositRepository.DepositsListener() {
            @Override
            public void onDepositsUpdated(List<Deposit> deposits) {
                currentDepositsList.clear();
                if (deposits != null) currentDepositsList.addAll(deposits);
                if (depositAdapter != null) {
                    depositAdapter.setDeposits(currentDepositsList, reversedDepositIds, memberNamesMap);
                }
                recalculateAndRenderFinance();
            }

            @Override
            public void onError(Exception error) {
                showFinanceLoadError(error);
            }
        });

        adjustmentsListener = adjustmentRepository.observeAdjustments(
                messId, periodId, isManager, currentUid, new AdjustmentRepository.AdjustmentsListener() {
            @Override
            public void onAdjustmentsUpdated(List<Adjustment> adjustments) {
                currentAdjustmentsList.clear();
                if (adjustments != null) currentAdjustmentsList.addAll(adjustments);

                reversedExpenseIds.clear();
                reversedDepositIds.clear();
                for (Adjustment adj : currentAdjustmentsList) {
                    if ("reversal".equalsIgnoreCase(adj.getType())) {
                        if ("expense".equalsIgnoreCase(adj.getTargetType())) {
                            reversedExpenseIds.add(adj.getTargetId());
                        } else if ("deposit".equalsIgnoreCase(adj.getTargetType())) {
                            reversedDepositIds.add(adj.getTargetId());
                        }
                    }
                }
                if (expenseAdapter != null) {
                    expenseAdapter.setExpenses(currentExpensesList, reversedExpenseIds);
                }
                if (depositAdapter != null) {
                    depositAdapter.setDeposits(currentDepositsList, reversedDepositIds, memberNamesMap);
                }
                recalculateAndRenderFinance();
            }

            @Override
            public void onError(Exception error) {
                showFinanceLoadError(error);
            }
        });
    }

    private void loadPeriodMeals(Mess mess, Period period) {
        if (periodMealsListener != null) periodMealsListener.remove();

        CutoffResult cutoffRes = CutoffCalculator.calculate(Instant.now(), mess.getTimezone(), mess.getCutoffTime());
        String startDate = period.getStartDate() != null ? period.getStartDate() : period.getPeriodId() + "-01";
        String endDate = period.getEndDate() != null ? period.getEndDate() : PeriodDates.getLastDay(period.getPeriodId());

        periodMealsListener = FirebaseFirestore.getInstance()
                .collection("messes").document(mess.getMessId()).collection("meals")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        showFinanceLoadError(error);
                        return;
                    }
                    currentPeriodMealsList.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Meal m = doc.toObject(Meal.class);
                            if (m != null) {
                                currentPeriodMealsList.add(m);
                            }
                        }
                    }
                    recalculateAndRenderFinance();
                });
    }

    private void recalculateAndRenderFinance() {
        if (currentMess == null || currentPeriod == null) return;
        FinanceResult result = financeRepository.calculateLive(
                currentMess, currentPeriod,
                currentExpensesList, currentDepositsList, currentAdjustmentsList,
                activeMemberList, currentPeriodMealsList
        );
        renderFinanceUI(result);
    }

    private void renderFinanceUI(FinanceResult result) {
        if (result == null) return;

        String currentUid = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUid() : "";
        MemberFinanceResult myRes = result.getMemberResults().get(currentUid);

        // Show the manager-configured meal rate on the summary and home dashboard.
        String rateText = result.isRateAvailable()
                ? MoneyUtils.formatPaisa(Math.round(result.getRatePaisaPerMeal())) + " / meal"
                : "Not set";
        binding.tvSummaryRate.setText(rateText);

        String dashboardRateText = result.isRateAvailable()
                ? MoneyUtils.formatPaisa(Math.round(result.getRatePaisaPerMeal()))
                : "Not set";
        binding.tvHomeMealRate.setText(dashboardRateText);

        binding.tvSummaryTotalFixed.setText(MoneyUtils.formatPaisa(result.getTotalFixedPaisa()));

        if (myRes != null) {
            binding.tvSummaryMyFixedShare.setText(MoneyUtils.formatPaisa(myRes.getFixedSharePaisa()));
            long netBalance = myRes.getNetBalancePaisa();
            String balanceFormatted = MoneyUtils.formatPaisa(netBalance);
            binding.tvSummaryMyBalance.setText(balanceFormatted);
            binding.tvHomePersonalBalance.setText(balanceFormatted + " (Estimated)");

            if (netBalance > 0) {
                binding.tvSummaryMyBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_teal));
                binding.tvHomePersonalBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_teal));
            } else if (netBalance < 0) {
                binding.tvSummaryMyBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_negative));
                binding.tvHomePersonalBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_negative));
            } else {
                binding.tvSummaryMyBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_text_secondary));
                binding.tvHomePersonalBalance.setTextColor(ContextCompat.getColor(this, R.color.ms_text_secondary));
            }
        }

    }

    private void showFinanceLoadError(Exception error) {
        String message = error != null && error.getLocalizedMessage() != null
                ? error.getLocalizedMessage() : "Failed to load finance data";
        Snackbar.make(binding.mainRoot, message, Snackbar.LENGTH_LONG).show();
    }

    private void onExpenseLongClick(Expense expense) {
        if (currentMess == null || currentPeriod == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.correct_expense_title)
                .setMessage(getString(R.string.expense_action_prompt, expense.getDescription()))
                .setPositiveButton(R.string.reverse_entry, (dialog, which) -> showReversalDialog("expense", expense.getId(), null))
                .setNeutralButton(R.string.reverse_and_reenter, (dialog, which) -> showReversalDialog("expense", expense.getId(), () -> {
                    Intent intent = new Intent(this, AddExpenseActivity.class);
                    intent.putExtra(AddExpenseActivity.EXTRA_MESS_ID, currentMess.getMessId());
                    intent.putExtra(AddExpenseActivity.EXTRA_CORRECTS_ID, expense.getId());
                    startActivity(intent);
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onDepositLongClick(Deposit deposit) {
        if (currentMess == null || currentPeriod == null) return;
        if ("pending".equalsIgnoreCase(deposit.getStatus())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.deposit_status_pending)
                    .setMessage(getString(R.string.review_deposit_prompt,
                            MoneyUtils.formatPaisa(deposit.getAmountPaisa()),
                            memberNamesMap.getOrDefault(deposit.getMemberId(), getString(R.string.role_member))))
                    .setPositiveButton(R.string.approve_deposit, (dialog, which) -> updateDepositStatus(deposit, "approved"))
                    .setNeutralButton(R.string.reject_deposit, (dialog, which) -> updateDepositStatus(deposit, "rejected"))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        if (!"approved".equalsIgnoreCase(deposit.getStatus())) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.correct_deposit_title)
                .setMessage(R.string.deposit_action_prompt)
                .setPositiveButton(R.string.reverse_entry, (dialog, which) -> showReversalDialog("deposit", deposit.getId(), null))
                .setNeutralButton(R.string.reverse_and_reenter, (dialog, which) -> showReversalDialog("deposit", deposit.getId(), () -> {
                    Intent intent = new Intent(this, AddDepositActivity.class);
                    intent.putExtra(AddDepositActivity.EXTRA_MESS_ID, currentMess.getMessId());
                    intent.putExtra(AddDepositActivity.EXTRA_CORRECTS_ID, deposit.getId());
                    startActivity(intent);
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateDepositStatus(Deposit deposit, String status) {
        if (currentMess == null || deposit == null) return;
        depositRepository.updateDepositStatus(currentMess.getMessId(), deposit.getId(), status,
                new DepositRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        String message = getString("approved".equals(status)
                                ? R.string.deposit_approved : R.string.deposit_rejected);
                        Snackbar.make(binding.mainRoot, message, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception error) {
                        String message = error != null && error.getLocalizedMessage() != null
                                ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to update deposit");
                        Snackbar.make(binding.mainRoot, message, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showReversalDialog(String targetType, String targetId, Runnable onReversed) {
        if (currentMess == null || currentMess.getCurrentPeriodId() == null) return;

        EditText etReason = new EditText(this);
        etReason.setHint(R.string.reversal_reason_hint);
        etReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);
        container.addView(etReason);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reverse_confirm_title)
                .setMessage(R.string.reverse_confirm_msg)
                .setView(container)
                .setPositiveButton(R.string.reverse_entry, (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = getString(R.string.correction_reason_default);

                    String adjId = UUID.randomUUID().toString();
                    String periodId = currentMess.getCurrentPeriodId();
                    String createdBy = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUid() : "";

                    Adjustment reversal = Adjustment.createReversal(adjId, targetType, targetId, reason, periodId, createdBy);

                    adjustmentRepository.addAdjustment(currentMess.getMessId(), reversal, new AdjustmentRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Snackbar.make(binding.mainRoot, R.string.entry_reversed, Snackbar.LENGTH_SHORT).show();
                            if (onReversed != null) onReversed.run();
                        }

                        @Override
                        public void onFailure(Exception error) {
                            String msg = error != null && error.getLocalizedMessage() != null
                                    ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to reverse");
                            Snackbar.make(binding.mainRoot, msg, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void observePersonalMealHistory(String messId, String uid) {
        if (myMealsListener != null) myMealsListener.remove();
        binding.progressMeals.setVisibility(View.VISIBLE);

        myMealsListener = mealRepository.observeMyMeals(messId, uid, new MealRepository.MyMealsListener() {
            @Override
            public void onMealsUpdated(List<Meal> meals) {
                binding.progressMeals.setVisibility(View.GONE);
                if (mealHistoryAdapter != null) {
                    mealHistoryAdapter.setMeals(meals);
                }
                binding.tvMealsEmpty.setVisibility(meals == null || meals.isEmpty() ? View.VISIBLE : View.GONE);
                personalMealsList.clear();
                if (meals != null) {
                    personalMealsList.addAll(meals);
                }
                calculateMonthlyCounts(personalMealsList);
            }

            @Override
            public void onError(Exception error) {
                binding.progressMeals.setVisibility(View.GONE);
            }
        });
    }

    private void calculateMonthlyCounts(List<Meal> meals) {
        if (meals == null || currentCutoffResult == null) {
            return;
        }
        String currentMonthPrefix = currentCutoffResult.getTodayDate().substring(0, 7); // e.g. "2025-05"
        int totalLunches = 0;
        int totalDinners = 0;
        int totalGuests = 0;

        for (Meal m : meals) {
            if (m.getDate() != null && m.getDate().startsWith(currentMonthPrefix)) {
                if (m.isLunch()) totalLunches++;
                if (m.isDinner()) totalDinners++;
                totalGuests += m.getGuests();
            }
        }

        binding.tvMonthLunches.setText(String.valueOf(totalLunches));
        binding.tvMonthDinners.setText(String.valueOf(totalDinners));
        binding.tvMonthGuests.setText(String.valueOf(totalGuests));

        long mealRatePaisa = currentMess != null ? currentMess.getMealRatePaisa() : 0L;
        long totalMealChargePaisa = mealRatePaisa > 0
                ? (long) (totalLunches + totalDinners + totalGuests) * mealRatePaisa
                : 0L;
        String totalMealCharge = mealRatePaisa > 0
                ? MoneyUtils.formatPaisa(totalMealChargePaisa)
                : "Not set";
        long tomorrowMealCount = tomorrowMeal == null ? 0L
                : (tomorrowMeal.isLunch() ? 1L : 0L)
                + (tomorrowMeal.isDinner() ? 1L : 0L)
                + tomorrowMeal.getGuests();
        String tomorrowMealCharge = mealRatePaisa > 0
                ? MoneyUtils.formatPaisa(tomorrowMealCount * mealRatePaisa)
                : "Not set";
        binding.tvHomeTomorrowMealCharge.setText(tomorrowMealCharge);
        binding.tvSummaryTomorrowMealCharge.setText(tomorrowMealCharge);
        binding.tvHomeTotalMealCharge.setText(totalMealCharge);
        binding.tvSummaryTotalMealCharge.setText(totalMealCharge);
    }

    private void updateMealDashboard() {
        if (currentMess == null) return;

        currentCutoffResult = CutoffCalculator.calculate(
                Instant.now(), currentMess.getTimezone(), currentMess.getCutoffTime()
        );
        calculateMonthlyCounts(personalMealsList);

        binding.tvHomeTodayDate.setText(getString(R.string.label_today_date, currentCutoffResult.getTodayDate()));
        binding.tvTomorrowHeader.setText(getString(R.string.label_tomorrow_meals_date, currentCutoffResult.getTomorrowDate()));

        boolean isManager = checkIsManager();
        boolean needsSetup = currentMess.getUtcOffsetMinutes() == 0;

        if (needsSetup && !isManager) {
            binding.cardSetupNotice.setVisibility(View.VISIBLE);
            setControlsEnabled(false);
            binding.tvCutoffStatus.setText(R.string.setup_incomplete_short);
            return;
        } else {
            binding.cardSetupNotice.setVisibility(View.GONE);
        }

        if (myMealListener != null) myMealListener.remove();
        myMealListener = mealRepository.observeMyMeal(currentMess.getMessId(), currentCutoffResult.getTomorrowDate(), new MealRepository.MealListener() {
            @Override
            public void onMealUpdated(Meal meal) {
                tomorrowMeal = meal;
                renderTomorrowMeal(meal);
            }

            @Override
            public void onError(Exception error) {
            }
        });

        if (isManager) {
            observeTomorrowDayTotals(currentMess.getMessId(), currentCutoffResult.getTomorrowDate());
        }

        startCutoffTimer();
    }

    private void observeTomorrowDayTotals(String messId, String tomorrowDate) {
        if (dayTotalsListener != null) dayTotalsListener.remove();
        binding.cardManagerDayTotals.setVisibility(View.VISIBLE);

        dayTotalsListener = mealRepository.observeDayTotals(messId, tomorrowDate, new MealRepository.DayTotalsListener() {
            @Override
            public void onTotalsUpdated(DayTotals totals) {
                binding.tvTotalLunch.setText(getString(R.string.label_lunch_count, totals.getTotalLunch()));
                binding.tvTotalDinner.setText(getString(R.string.label_dinner_count, totals.getTotalDinner()));
                binding.tvTotalGuests.setText(getString(R.string.label_guests_count_total, totals.getTotalGuests()));
                binding.tvGrandTotal.setText(getString(R.string.label_grand_total_meals, totals.getGrandTotal()));
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void renderTomorrowMeal(Meal meal) {
        tomorrowMeal = meal;
        isUpdatingControls = true;
        binding.switchLunch.setChecked(meal.isLunch());
        binding.switchDinner.setChecked(meal.isDinner());
        binding.tvGuestCount.setText(String.valueOf(meal.getGuests()));
        binding.tvMealPending.setVisibility(meal.isPending() ? View.VISIBLE : View.GONE);
        isUpdatingControls = false;
        calculateMonthlyCounts(personalMealsList);

        boolean open = currentCutoffResult != null && currentCutoffResult.isOpen();
        setControlsEnabled(open && !isSavingMeal);
    }

    private void startCutoffTimer() {
        handler.removeCallbacks(timerRunnable);
        updateCutoffTimer();
    }

    private void updateCutoffTimer() {
        if (currentMess == null) return;

        currentCutoffResult = CutoffCalculator.calculate(
                Instant.now(), currentMess.getTimezone(), currentMess.getCutoffTime()
        );

        if (currentCutoffResult.isOpen()) {
            long millis = currentCutoffResult.getMillisUntilCutoff();
            long seconds = millis / 1000;
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            binding.tvCutoffStatus.setText(getString(R.string.cutoff_closes_in, hours, mins));
            setControlsEnabled(!isSavingMeal);
            handler.postDelayed(timerRunnable, 30000L);
        } else {
            binding.tvCutoffStatus.setText(getString(R.string.cutoff_closed, currentMess.getCutoffTime()));
            setControlsEnabled(false);
        }
    }

    private void submitMealChange(boolean newLunch, boolean newDinner, int newGuests) {
        if (currentMess == null || currentCutoffResult == null || isSavingMeal) return;

        if (!currentCutoffResult.isOpen()) {
            Snackbar.make(binding.mainRoot, R.string.meal_editing_closed, Snackbar.LENGTH_LONG).show();
            if (tomorrowMeal != null) renderTomorrowMeal(tomorrowMeal);
            return;
        }

        isSavingMeal = true;
        setControlsEnabled(false);

        mealRepository.setMeal(currentMess.getMessId(), currentCutoffResult.getTomorrowDate(), newLunch, newDinner, newGuests, new MealRepository.SetMealCallback() {
            @Override
            public void onSuccess() {
                isSavingMeal = false;
                setControlsEnabled(currentCutoffResult != null && currentCutoffResult.isOpen());
            }

            @Override
            public void onFailure(Exception error) {
                isSavingMeal = false;
                Snackbar.make(binding.mainRoot, R.string.meal_editing_closed, Snackbar.LENGTH_LONG).show();
                if (tomorrowMeal != null) {
                    renderTomorrowMeal(tomorrowMeal);
                } else {
                    setControlsEnabled(currentCutoffResult != null && currentCutoffResult.isOpen());
                }
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        binding.switchLunch.setEnabled(enabled);
        binding.switchDinner.setEnabled(enabled);
        binding.btnGuestMinus.setEnabled(enabled);
        binding.btnGuestPlus.setEnabled(enabled);
    }

    private void populateProfileUI(UserProfile profile) {
        binding.tvProfileName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "User");
        binding.tvProfileEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
        boolean isManager = checkIsManager();
        binding.tvProfileRole.setText(isManager ? R.string.role_badge_manager : R.string.role_badge_member);
        binding.btnSettings.setVisibility(isManager ? View.VISIBLE : View.GONE);
    }

    private void populateMessUI(Mess mess) {
        if (mess == null) return;
        binding.tvHomeMessName.setText(mess.getName());

        boolean isManager = checkIsManager();
        binding.layoutManagerCodeActions.setVisibility(isManager ? View.VISIBLE : View.GONE);

        if (mess.getInvitationCode() != null && !mess.getInvitationCode().isEmpty()) {
            binding.cardInvitationCode.setVisibility(View.VISIBLE);
            binding.tvInvitationCode.setText(mess.getInvitationCode());
        } else {
            binding.tvInvitationCode.setText("------");
        }
    }

    private void copyInvitationCode() {
        if (currentMess == null || currentMess.getInvitationCode() == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Invitation Code", currentMess.getInvitationCode());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Snackbar.make(binding.mainRoot, R.string.code_copied, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareInvitationCode() {
        if (currentMess == null || currentMess.getInvitationCode() == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, currentMess.getInvitationCode()));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_code)));
    }

    private void revokeInvitationCode() {
        if (currentMess == null || currentMess.getInvitationCode() == null) return;
        messRepository.revokeCode(currentMess.getInvitationCode(), new MessRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(binding.mainRoot, R.string.code_revoked, Snackbar.LENGTH_SHORT).show();
                binding.tvInvitationCode.setText("------");
            }

            @Override
            public void onFailure(Exception error) {
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to revoke");
                Snackbar.make(binding.mainRoot, msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void regenerateInvitationCode() {
        if (currentMess == null) return;
        String oldCode = currentMess.getInvitationCode();
        messRepository.regenerateCode(currentMess.getMessId(), oldCode, new MessRepository.RegenerateCodeCallback() {
            @Override
            public void onSuccess(String newCode) {
                Snackbar.make(binding.mainRoot, getString(R.string.code_regenerated, newCode), Snackbar.LENGTH_SHORT).show();
                binding.tvInvitationCode.setText(newCode);
            }

            @Override
            public void onFailure(Exception error) {
                String msg = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to regenerate");
                Snackbar.make(binding.mainRoot, msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRemoveMember(Member member) {
        if (currentMess == null || member == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_remove_member_title)
                .setMessage(getString(R.string.confirm_remove_member_msg, member.getName()))
                .setPositiveButton(R.string.remove_member, (dialog, which) -> messRepository.removeMember(currentMess.getMessId(), member.getUid(), new MessRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Snackbar.make(binding.mainRoot, R.string.member_removed, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception error) {
                        String msg = error != null && error.getLocalizedMessage() != null
                                ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to remove member");
                        Snackbar.make(binding.mainRoot, msg, Snackbar.LENGTH_LONG).show();
                    }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }



    private void openChooseScreen() {
        Intent intent = new Intent(this, ChooseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerRunnable);
        if (messListener != null) messListener.remove();
        if (membersListener != null) membersListener.remove();
        if (myMealListener != null) myMealListener.remove();
        if (myMealsListener != null) myMealsListener.remove();
        if (dayTotalsListener != null) dayTotalsListener.remove();
        if (expensesListener != null) expensesListener.remove();
        if (depositsListener != null) depositsListener.remove();
        if (adjustmentsListener != null) adjustmentsListener.remove();
        if (periodMealsListener != null) periodMealsListener.remove();
        if (archiveListener != null) archiveListener.remove();
        super.onDestroy();
    }
}
