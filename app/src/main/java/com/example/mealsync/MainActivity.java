package com.example.mealsync;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
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
import com.example.mealsync.data.repository.ProfileRepository;
import com.example.mealsync.data.repository.TimeCheckRepository;
import com.example.mealsync.databinding.ActivityMainBinding;
import com.example.mealsync.finance.FinanceResult;
import com.example.mealsync.finance.MemberFinanceResult;
import com.example.mealsync.notification.MealReminderScheduler;
import com.example.mealsync.notification.SystemStateReceiver;
import com.example.mealsync.reports.ReportData;
import com.example.mealsync.reports.ReportPdfGenerator;
import com.example.mealsync.reports.ReportRepository;
import com.example.mealsync.sync.SyncWorker;
import com.example.mealsync.ui.auth.AuthExtras;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.example.mealsync.ui.contacts.ContactPickerActivity;
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
import com.example.mealsync.util.ClockCheckLogic;
import com.example.mealsync.util.CutoffCalculator;
import com.example.mealsync.util.CutoffResult;
import com.example.mealsync.util.MoneyUtils;
import com.example.mealsync.util.SystemBannerState;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final TimeCheckRepository timeCheckRepository = new TimeCheckRepository();
    private final ProfileRepository profileRepository = new ProfileRepository();
    private final ReportRepository reportRepository = new ReportRepository();

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

    private DatabaseReference connectedReference;
    private DatabaseReference ownPresenceReference;
    private DatabaseReference memberPresenceReference;
    private ValueEventListener connectionListener;
    private ValueEventListener memberPresenceListener;
    private String presenceMessId;
    private String presenceUid;
    private boolean presenceStarted;
    private boolean realtimeConnected;

    private CutoffResult currentCutoffResult;
    private Meal tomorrowMeal;
    private boolean isUpdatingControls = false;
    private boolean isSavingMeal = false;
    private boolean isDeletingMess = false;
    private boolean airplaneModeOn = false;
    private boolean offline = false;
    private boolean systemStateReceiverRegistered = false;
    private boolean connectivityCallbackRegistered = false;
    private Uri pendingProfilePhotoUri;
    private ShapeableImageView editProfilePhotoPreview;
    private AlertDialog editProfileDialog;
    private final ExecutorService profilePhotoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService reportExecutor = Executors.newSingleThreadExecutor();
    private String reportOpeningDate;
    private String reportEndingDate;
    private File generatedReportFile;
    private boolean isGeneratingReport;

    private interface ProfilePhotoSaveCallback {
        void onSuccess();
        void onFailure(Exception error);
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = this::updateCutoffTimer;
    private final SystemStateReceiver systemStateReceiver =
            new SystemStateReceiver(enabled -> {
                boolean wasAirplaneModeOn = airplaneModeOn;
                airplaneModeOn = enabled;
                updateSystemBanners();
                if (wasAirplaneModeOn && !enabled) {
                    SyncWorker.enqueue(MainActivity.this);
                }
            });
    private final ConnectivityManager.NetworkCallback connectivityCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(MainActivity.this::updateInternetState);
                }

                @Override
                public void onLost(Network network) {
                    runOnUiThread(MainActivity.this::updateInternetState);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    runOnUiThread(MainActivity.this::updateInternetState);
                }
            };
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && currentMess != null) {
                    scheduleMealReminder(currentMess);
                }
            });
    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openContactPickerWithCurrentCode();
                } else {
                    showContactsPermissionDenied();
                }
            });
    private final ActivityResultLauncher<String> profilePhotoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingProfilePhotoUri = uri;
                    if (editProfilePhotoPreview != null) {
                        loadProfilePhoto(editProfilePhotoPreview, uri);
                    }
                }
            });
    private final ActivityResultLauncher<String> savePdfLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),
                    uri -> saveGeneratedPdf(uri));

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

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) {
            return;
        }

        startPresence();
        startMemberPresenceListener();

        airplaneModeOn = Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        IntentFilter airplaneModeFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        ContextCompat.registerReceiver(this, systemStateReceiver, airplaneModeFilter,
                ContextCompat.RECEIVER_EXPORTED);
        systemStateReceiverRegistered = true;

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(connectivityCallback);
            connectivityCallbackRegistered = true;
        }
        updateInternetState();
        updateSystemBanners();
    }

    @Override
    protected void onStop() {
        stopPresence();
        if (systemStateReceiverRegistered) {
            unregisterReceiver(systemStateReceiver);
            systemStateReceiverRegistered = false;
        }
        if (connectivityCallbackRegistered) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(connectivityCallback);
            }
            connectivityCallbackRegistered = false;
        }
        super.onStop();
    }

    private void updateInternetState() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager != null
                ? connectivityManager.getActiveNetwork() : null;
        NetworkCapabilities capabilities = connectivityManager != null && activeNetwork != null
                ? connectivityManager.getNetworkCapabilities(activeNetwork) : null;
        offline = capabilities == null
                || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        updateSystemBanners();
    }

    private void updateSystemBanners() {
        if (binding == null) {
            return;
        }
        List<SystemBannerState.Banner> visibleBanners =
                SystemBannerState.visibleBanners(offline, airplaneModeOn);
        binding.cardOfflineBanner.setVisibility(visibleBanners.contains(SystemBannerState.Banner.OFFLINE)
                ? View.VISIBLE : View.GONE);
        binding.cardAirplaneModeBanner.setVisibility(
                visibleBanners.contains(SystemBannerState.Banner.AIRPLANE_MODE)
                        ? View.VISIBLE : View.GONE);
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
        binding.bottomNav.setItemIconTintList(null);
        tintNavigationIcon(R.id.nav_home);
        tintNavigationIcon(R.id.nav_meals);
        tintNavigationIcon(R.id.nav_money);
        tintNavigationIcon(R.id.nav_members);
        binding.bottomNav.getMenu().findItem(R.id.nav_profile).setIconTintList(null);
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
                checkTrustedTime();
                return true;
            }
            return false;
        });
    }

    private void tintNavigationIcon(int itemId) {
        android.view.MenuItem item = binding.bottomNav.getMenu().findItem(itemId);
        Drawable icon = DrawableCompat.wrap(item.getIcon()).mutate();
        DrawableCompat.setTintList(icon, ContextCompat.getColorStateList(this, R.color.nav_item_tint));
        item.setIcon(icon);
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
        binding.btnSubReports.setOnClickListener(v -> {
            if (checkIsManager()) {
                showMoneySubPanel(R.id.panelSubReports);
            } else {
                Snackbar.make(binding.mainRoot, R.string.report_not_available,
                        Snackbar.LENGTH_LONG).show();
            }
        });
        binding.btnReportOpeningDate.setOnClickListener(v -> showReportDatePicker(true));
        binding.btnReportEndingDate.setOnClickListener(v -> showReportDatePicker(false));
        binding.btnGenerateReport.setOnClickListener(v -> generatePdfReport());

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
        binding.panelSubReports.setVisibility(panelId == R.id.panelSubReports ? View.VISIBLE : View.GONE);
        binding.btnSubExpenses.setSelected(panelId == R.id.panelSubExpenses);
        binding.btnSubDeposits.setSelected(panelId == R.id.panelSubDeposits);
        binding.btnSubSummary.setSelected(panelId == R.id.panelSubSummary);
        binding.btnSubArchive.setSelected(panelId == R.id.panelSubArchive);
        binding.btnSubReports.setSelected(panelId == R.id.panelSubReports);
    }

    private void setupListeners() {
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.btnLogout.setOnClickListener(v -> {
            MealReminderScheduler.cancel(this);
            stopPresence();
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
        binding.btnDeleteMess.setOnClickListener(v -> confirmDeleteMess());

        binding.btnCopyCode.setOnClickListener(v -> copyInvitationCode());
        binding.btnShareCode.setOnClickListener(v -> shareInvitationCode());
        binding.btnInviteFromContacts.setOnClickListener(v -> explainContactAccess());
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
        binding.btnSubReports.setVisibility(isManager ? View.VISIBLE : View.GONE);
        if (!isManager && binding.panelSubReports.getVisibility() == View.VISIBLE) {
            showMoneySubPanel(R.id.panelSubExpenses);
        }

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
                if (mess == null) {
                    currentMess = null;
                    MealReminderScheduler.cancel(MainActivity.this);
                    openChooseScreen();
                    return;
                }
                currentMess = mess;
                setPresenceMessId(mess.getMessId());
                if (mess.isDeleting()) {
                    MealReminderScheduler.cancel(MainActivity.this);
                } else {
                    scheduleMealReminder(mess);
                    requestNotificationPermissionIfNeeded();
                }
                boolean isManager = checkIsManager();

                if (isManager && !mess.isDeleting()) {
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
                if (mess.getCurrentPeriodId() != null) {
                    loadPeriodAndFinance(mess);
                }
            }

            @Override
            public void onError(Exception error) {
                if (currentMess != null && currentMess.isDeleting()) {
                    openChooseScreen();
                    return;
                }
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

        if (currentMess.isDeleting()) {
            handler.removeCallbacks(timerRunnable);
            binding.tvCutoffStatus.setText(R.string.deleting_mess);
            setControlsEnabled(false);
            return;
        }

        if (myMealListener != null) myMealListener.remove();
        myMealListener = mealRepository.observeMyMeal(
                currentMess.getMessId(),
                currentCutoffResult.getTomorrowDate(),
                new MealRepository.MealListener() {
                    @Override
                    public void onMealUpdated(Meal meal) {
                        tomorrowMeal = meal;
                        renderTomorrowMeal(meal);
                    }

                    @Override
                    public void onError(Exception error) {
                        Snackbar.make(binding.mainRoot, R.string.meal_load_failed, Snackbar.LENGTH_LONG).show();
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
        setControlsEnabled(open && !isSavingMeal && (currentMess == null || !currentMess.isDeleting()));
    }

    private void startCutoffTimer() {
        handler.removeCallbacks(timerRunnable);
        updateCutoffTimer();
    }

    private void updateCutoffTimer() {
        if (currentMess == null) return;
        if (currentMess.isDeleting()) {
            binding.tvCutoffStatus.setText(R.string.deleting_mess);
            setControlsEnabled(false);
            return;
        }

        String previousTomorrowDate = currentCutoffResult != null
                ? currentCutoffResult.getTomorrowDate()
                : null;
        currentCutoffResult = CutoffCalculator.calculate(
                Instant.now(), currentMess.getTimezone(), currentMess.getCutoffTime()
        );

        if (previousTomorrowDate != null
                && !previousTomorrowDate.equals(currentCutoffResult.getTomorrowDate())) {
            tomorrowMeal = null;
            setControlsEnabled(false);
            updateMealDashboard();
            return;
        }

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

        String mealDate = currentCutoffResult.getTomorrowDate();
        mealRepository.setMeal(currentMess.getMessId(), mealDate, newLunch, newDinner, newGuests, new MealRepository.SetMealCallback() {
            @Override
            public void onSuccess() {
                isSavingMeal = false;
                if (currentCutoffResult != null
                        && mealDate.equals(currentCutoffResult.getTomorrowDate())) {
                    setControlsEnabled(currentCutoffResult.isOpen()
                            && (currentMess == null || !currentMess.isDeleting()));
                }
            }

            @Override
            public void onFailure(Exception error) {
                isSavingMeal = false;
                Snackbar.make(binding.mainRoot, R.string.meal_save_failed, Snackbar.LENGTH_LONG).show();
                if (tomorrowMeal != null && mealDate.equals(tomorrowMeal.getDate())) {
                    renderTomorrowMeal(tomorrowMeal);
                } else if (currentCutoffResult != null
                        && mealDate.equals(currentCutoffResult.getTomorrowDate())) {
                    setControlsEnabled(currentCutoffResult.isOpen()
                            && (currentMess == null || !currentMess.isDeleting()));
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

    private void showReportDatePicker(boolean openingDate) {
        LocalDate initialDate = parseReportDate(openingDate ? reportOpeningDate : reportEndingDate);
        if (initialDate == null) {
            initialDate = LocalDate.now();
        }
        LocalDate selectedInitialDate = initialDate;
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            String isoDate = selectedDate.toString();
            if (openingDate) {
                reportOpeningDate = isoDate;
                binding.btnReportOpeningDate.setText(getString(R.string.report_opening_date_value,
                        formatReportDate(selectedDate)));
            } else {
                reportEndingDate = isoDate;
                binding.btnReportEndingDate.setText(getString(R.string.report_ending_date_value,
                        formatReportDate(selectedDate)));
            }
            binding.tvReportStatus.setVisibility(View.GONE);
        }, selectedInitialDate.getYear(), selectedInitialDate.getMonthValue() - 1,
                selectedInitialDate.getDayOfMonth());
        if (!openingDate && reportOpeningDate != null) {
            LocalDate minDate = LocalDate.parse(reportOpeningDate);
            java.util.Calendar minimum = java.util.Calendar.getInstance();
            minimum.clear();
            minimum.set(minDate.getYear(), minDate.getMonthValue() - 1, minDate.getDayOfMonth());
            picker.getDatePicker().setMinDate(minimum.getTimeInMillis());
        }
        picker.show();
    }

    private LocalDate parseReportDate(String date) {
        return date == null ? null : LocalDate.parse(date);
    }

    private String formatReportDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault()));
    }

    private void generatePdfReport() {
        if (!checkIsManager()) {
            showReportError(R.string.report_not_available);
            return;
        }
        if (currentMess == null || reportOpeningDate == null || reportEndingDate == null) {
            showReportError(R.string.report_date_required);
            return;
        }
        LocalDate opening = LocalDate.parse(reportOpeningDate);
        LocalDate ending = LocalDate.parse(reportEndingDate);
        if (ending.isBefore(opening)) {
            showReportError(R.string.report_invalid_range);
            return;
        }
        if (isGeneratingReport) {
            return;
        }

        isGeneratingReport = true;
        binding.btnGenerateReport.setEnabled(false);
        binding.btnGenerateReport.setText(R.string.report_generating);
        binding.tvReportStatus.setVisibility(View.VISIBLE);
        binding.tvReportStatus.setText(R.string.report_generating);
        reportRepository.generate(currentMess, reportOpeningDate, reportEndingDate,
                new ReportRepository.Callback() {
                    @Override
                    public void onSuccess(ReportData report) {
                        if (isFinishing() || isDestroyed()) return;
                        reportExecutor.execute(() -> {
                            try {
                                File pdfFile = ReportPdfGenerator.generate(
                                        getApplicationContext(), report);
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    generatedReportFile = pdfFile;
                                    finishReportGeneration();
                                    binding.tvReportStatus.setText(R.string.report_generated);
                                    showReportReadyDialog(report, pdfFile);
                                });
                            } catch (IOException error) {
                                Log.e("MainActivity", "Could not create report PDF", error);
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    finishReportGeneration();
                                    showReportError(R.string.report_generation_failed);
                                });
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception error) {
                        Log.e("MainActivity", "Could not collect report data", error);
                        if (isFinishing() || isDestroyed()) return;
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            finishReportGeneration();
                            showReportError(R.string.report_generation_failed);
                        });
                    }
                });
    }

    private void finishReportGeneration() {
        isGeneratingReport = false;
        binding.btnGenerateReport.setEnabled(true);
        binding.btnGenerateReport.setText(R.string.generate_pdf_report);
    }

    private void showReportReadyDialog(ReportData report, File pdfFile) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_report_ready, null);
        TextView message = dialogView.findViewById(R.id.tvReportReadyMessage);
        message.setText(getString(R.string.report_ready_message,
                formatReportDate(LocalDate.parse(report.openingDate)),
                formatReportDate(LocalDate.parse(report.endingDate))));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        dialogView.findViewById(R.id.btnViewReportPdf).setOnClickListener(v -> {
            dialog.dismiss();
            viewGeneratedPdf(pdfFile);
        });
        dialogView.findViewById(R.id.btnShareReportPdf).setOnClickListener(v -> {
            dialog.dismiss();
            shareGeneratedPdf(pdfFile);
        });
        dialogView.findViewById(R.id.btnCancelReportPdf).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSaveReportPdf).setOnClickListener(v -> {
            dialog.dismiss();
            savePdfLauncher.launch(pdfFile.getName());
        });
        dialog.show();
    }

    private void showReportError(int messageResource) {
        binding.tvReportStatus.setVisibility(View.VISIBLE);
        binding.tvReportStatus.setText(messageResource);
        Snackbar.make(binding.mainRoot, messageResource, Snackbar.LENGTH_LONG).show();
    }

    private void saveGeneratedPdf(Uri destination) {
        File source = generatedReportFile;
        if (destination == null || source == null || !source.isFile()) {
            return;
        }
        reportExecutor.execute(() -> {
            try (InputStream input = new java.io.FileInputStream(source);
                 OutputStream output = getContentResolver().openOutputStream(destination)) {
                if (output == null) {
                    throw new IOException("Unable to open the selected destination.");
                }
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Snackbar.make(binding.mainRoot, R.string.report_saved,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
            } catch (IOException error) {
                Log.e("MainActivity", "Could not save report PDF", error);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showReportError(R.string.report_save_failed);
                    }
                });
            }
        });
    }

    private void shareGeneratedPdf(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.setClipData(ClipData.newUri(getContentResolver(), "PDF report", pdfUri));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.report_share)));
        } catch (ActivityNotFoundException | IllegalArgumentException error) {
            Log.e("MainActivity", "Could not share report PDF", error);
            showReportError(R.string.report_share_failed);
        }
    }

    private void viewGeneratedPdf(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", pdfFile);
            Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(pdfUri, "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            viewIntent.setClipData(ClipData.newUri(getContentResolver(), "PDF report", pdfUri));
            startActivity(Intent.createChooser(viewIntent, getString(R.string.report_view)));
        } catch (ActivityNotFoundException | IllegalArgumentException error) {
            Log.e("MainActivity", "Could not open report PDF", error);
            showReportError(R.string.report_view_failed);
        }
    }

    private void populateProfileUI(UserProfile profile) {
        binding.tvProfileName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "User");
        binding.tvProfileEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
        loadProfilePhoto(binding.ivProfilePhoto, profile.getUid());
        updateProfileNavigationIcon(profile.getUid());
        boolean isManager = checkIsManager();
        binding.tvProfileRole.setText(isManager ? R.string.role_badge_manager : R.string.role_badge_member);
        binding.btnSettings.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.btnDeleteMess.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.btnClockCheck.setOnClickListener(v -> checkTrustedTime());
    }

    private void showEditProfileDialog() {
        if (currentProfile == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText nameInput = dialogView.findViewById(R.id.etProfileName);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.tilProfileName);
        editProfilePhotoPreview = dialogView.findViewById(R.id.ivEditProfilePhoto);
        nameInput.setText(currentProfile.getDisplayName());
        pendingProfilePhotoUri = null;
        loadProfilePhoto(editProfilePhotoPreview, currentProfile.getUid());

        dialogView.findViewById(R.id.btnChooseProfilePhoto).setOnClickListener(v ->
                profilePhotoPicker.launch("image/*"));

        editProfileDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_profile_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_profile, null)
                .create();
        editProfileDialog.setOnShowListener(dialog -> {
            Button saveButton = editProfileDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String displayName = nameInput.getText() != null
                        ? nameInput.getText().toString().trim() : "";
                if (displayName.isEmpty()) {
                    nameLayout.setError(getString(R.string.profile_name_required));
                    return;
                }

                nameLayout.setError(null);
                saveButton.setEnabled(false);
                saveButton.setText(R.string.profile_saving);
                String uid = currentProfile.getUid();
                Runnable updateProfile = () -> profileRepository.updateProfile(uid,
                        currentProfile.getMessId(), displayName, new ProfileRepository.Callback() {
                            @Override
                            public void onSuccess() {
                                if (isFinishing() || isDestroyed()) return;
                                currentProfile.setDisplayName(displayName);
                                populateProfileUI(currentProfile);
                                Snackbar.make(binding.mainRoot, R.string.profile_saved,
                                        Snackbar.LENGTH_SHORT).show();
                                if (editProfileDialog != null && editProfileDialog.isShowing()) {
                                    editProfileDialog.dismiss();
                                }
                            }

                            @Override
                            public void onFailure(Exception error) {
                                if (isFinishing() || isDestroyed()) return;
                                Log.w("MainActivity", "Could not update profile", error);
                                saveButton.setEnabled(true);
                                saveButton.setText(R.string.save_profile);
                                Snackbar.make(binding.mainRoot, R.string.profile_update_failed,
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
                if (pendingProfilePhotoUri != null) {
                    saveProfilePhotoLocally(pendingProfilePhotoUri, uid,
                            new ProfilePhotoSaveCallback() {
                                @Override
                                public void onSuccess() {
                                    if (!isFinishing() && !isDestroyed()) updateProfile.run();
                                }

                                @Override
                                public void onFailure(Exception error) {
                                    if (isFinishing() || isDestroyed()) return;
                                    Log.w("MainActivity", "Could not save profile photo on this device", error);
                                    saveButton.setEnabled(true);
                                    saveButton.setText(R.string.save_profile);
                                    Snackbar.make(binding.mainRoot, R.string.profile_photo_save_failed,
                                            Snackbar.LENGTH_LONG).show();
                                }
                            });
                } else {
                    updateProfile.run();
                }
            });
        });
        editProfileDialog.setOnDismissListener(dialog -> {
            editProfilePhotoPreview = null;
            pendingProfilePhotoUri = null;
            editProfileDialog = null;
        });
        editProfileDialog.show();
    }

    private void saveProfilePhotoLocally(Uri photoUri, String uid,
                                         ProfilePhotoSaveCallback callback) {
        profilePhotoExecutor.execute(() -> {
            try {
                File photoDirectory = new File(getFilesDir(), "profile_photos");
                if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
                    throw new IOException("Could not create profile photo directory.");
                }
                File photoFile = new File(photoDirectory, uid + ".jpg");
                try (InputStream input = getContentResolver().openInputStream(photoUri);
                     OutputStream output = new FileOutputStream(photoFile)) {
                    if (input == null) {
                        throw new IOException("Could not open selected profile photo.");
                    }
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }
                }
                runOnUiThread(callback::onSuccess);
            } catch (IOException error) {
                runOnUiThread(() -> callback.onFailure(error));
            }
        });
    }

    private void loadProfilePhoto(ImageView imageView, String uid) {
        File photoFile = new File(new File(getFilesDir(), "profile_photos"), uid + ".jpg");
        Glide.with(this)
                .load(photoFile.exists() ? photoFile : null)
                .signature(new ObjectKey(photoFile.exists() ? photoFile.lastModified() : 0L))
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .circleCrop()
                .into(imageView);
    }

    private void updateProfileNavigationIcon(String uid) {
        File photoFile = new File(new File(getFilesDir(), "profile_photos"), uid + ".jpg");
        android.view.MenuItem profileItem = binding.bottomNav.getMenu().findItem(R.id.nav_profile);
        if (!photoFile.isFile()) {
            profileItem.setIcon(R.drawable.ic_nav_profile);
            return;
        }

        int iconSize = (int) (24 * getResources().getDisplayMetrics().density);
        Glide.with(this)
                .asBitmap()
                .load(photoFile)
                .signature(new ObjectKey(photoFile.lastModified()))
                .override(iconSize, iconSize)
                .circleCrop()
                .into(new CustomTarget<Bitmap>(iconSize, iconSize) {
                    @Override
                    public void onResourceReady(Bitmap resource,
                                                Transition<? super Bitmap> transition) {
                        if (!isFinishing() && !isDestroyed() && currentProfile != null
                                && uid.equals(currentProfile.getUid())) {
                            profileItem.setIcon(new BitmapDrawable(getResources(), resource));
                            profileItem.setIconTintList(null);
                        }
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }

    private void loadProfilePhoto(ImageView imageView, Uri photoUri) {
        Glide.with(this)
                .load(photoUri)
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .circleCrop()
                .into(imageView);
    }

    private void checkTrustedTime() {
        if (binding == null || isFinishing() || isDestroyed()) return;

        binding.btnClockCheck.setEnabled(false);
        binding.tvClockCheckStatus.setText(R.string.clock_check_loading);
        binding.tvClockCheckWarning.setVisibility(View.GONE);
        timeCheckRepository.check(new TimeCheckRepository.Callback() {
            @Override
            public void onSuccess(long networkTimeMillis, long phoneTimeMillis, long differenceMillis) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    DateFormat dateFormat = DateFormat.getDateTimeInstance();
                    binding.tvClockNetworkTime.setText(getString(R.string.clock_check_network_value,
                            dateFormat.format(new Date(networkTimeMillis))));
                    binding.tvClockPhoneTime.setText(getString(R.string.clock_check_phone_value,
                            dateFormat.format(new Date(phoneTimeMillis))));
                    binding.tvClockDifference.setText(getString(R.string.clock_check_difference_value,
                            formatClockDifference(differenceMillis)));
                    boolean clockIsOff = ClockCheckLogic.isClockOffByMoreThanTwoMinutes(
                            differenceMillis);
                    binding.tvClockCheckWarning.setVisibility(clockIsOff ? View.VISIBLE : View.GONE);
                    binding.tvClockCheckStatus.setText(clockIsOff
                            ? R.string.clock_check_complete_off
                            : R.string.clock_check_complete);
                    binding.btnClockCheck.setEnabled(true);
                });
            }

            @Override
            public void onFailure(TimeCheckRepository.Failure failure) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.tvClockCheckStatus.setText(clockCheckFailureMessage(failure));
                    binding.btnClockCheck.setEnabled(true);
                });
            }
        });
    }

    private String clockCheckFailureMessage(TimeCheckRepository.Failure failure) {
        switch (failure) {
            case OFFLINE:
                return getString(R.string.clock_check_error_offline);
            case TIMEOUT:
                return getString(R.string.clock_check_error_timeout);
            case HTTP:
                return getString(R.string.clock_check_error_http);
            case BAD_JSON:
                return getString(R.string.clock_check_error_bad_response);
            case OTHER:
            default:
                return getString(R.string.clock_check_error_other);
        }
    }

    private String formatClockDifference(long differenceMillis) {
        long totalSeconds = Math.abs(differenceMillis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        String sign = differenceMillis > 0L ? "+" : differenceMillis < 0L ? "\u2212" : "";
        return getString(R.string.clock_check_difference_format, sign, hours, minutes, seconds);
    }

    private void confirmDeleteMess() {
        if (currentMess == null || !checkIsManager() || isDeletingMess) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_mess_confirm_title)
                .setMessage(getString(R.string.delete_mess_confirm_message, currentMess.getName()))
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_mess_confirm_yes, (dialog, which) -> deleteMess())
                .show();
    }

    private void deleteMess() {
        if (currentMess == null || isDeletingMess) return;

        isDeletingMess = true;
        binding.btnDeleteMess.setEnabled(false);
        binding.btnDeleteMess.setText(R.string.deleting_mess);
        binding.btnSettings.setEnabled(false);
        binding.btnLogout.setEnabled(false);

        messRepository.deleteMess(currentMess.getMessId(), new MessRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                openChooseScreen();
            }

            @Override
            public void onFailure(Exception error) {
                isDeletingMess = false;
                binding.btnDeleteMess.setEnabled(true);
                binding.btnDeleteMess.setText(R.string.delete_mess);
                binding.btnSettings.setEnabled(true);
                binding.btnLogout.setEnabled(true);
                String message = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage()
                        : getString(R.string.delete_mess_failed);
                Snackbar.make(binding.mainRoot, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void populateMessUI(Mess mess) {
        if (mess == null) return;
        binding.tvHomeMessName.setText(mess.getName());

        boolean isManager = checkIsManager();
        binding.layoutManagerCodeActions.setVisibility(isManager && !mess.isDeleting() ? View.VISIBLE : View.GONE);
        binding.btnSettings.setEnabled(!mess.isDeleting());

        if (mess.getInvitationCode() != null && !mess.getInvitationCode().isEmpty()) {
            binding.cardInvitationCode.setVisibility(View.VISIBLE);
            binding.tvInvitationCode.setText(mess.getInvitationCode());
            binding.btnInviteFromContacts.setVisibility(mess.isDeleting() ? View.GONE : View.VISIBLE);
        } else {
            binding.btnInviteFromContacts.setVisibility(View.GONE);
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

    private void explainContactAccess() {
        if (currentMess == null
                || currentMess.getInvitationCode() == null
                || currentMess.getInvitationCode().trim().isEmpty()) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contacts_permission_title)
                .setMessage(R.string.contacts_permission_explanation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.continue_action, (dialog, which) -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                            == PackageManager.PERMISSION_GRANTED) {
                        openContactPickerWithCurrentCode();
                    } else {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
                    }
                })
                .show();
    }

    private void showContactsPermissionDenied() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.contacts_permission_denied)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.open_app_settings, (dialog, which) -> {
                    Intent settingsIntent = new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    try {
                        startActivity(settingsIntent);
                    } catch (ActivityNotFoundException error) {
                        Snackbar.make(binding.mainRoot,
                                R.string.invite_contacts_settings_unavailable,
                                Snackbar.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void openContactPickerWithCurrentCode() {
        if (currentMess == null) {
            return;
        }
        String code = currentMess.getInvitationCode();
        String messId = currentMess.getMessId();
        String messName = currentMess.getName();
        if (code == null || code.trim().isEmpty() || messId == null) {
            Snackbar.make(binding.mainRoot, R.string.invitation_code_unavailable,
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("invitationCodes").document(code)
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null
                            || !task.getResult().exists()) {
                        String message = task.getException() != null
                                && task.getException().getLocalizedMessage() != null
                                ? task.getException().getLocalizedMessage()
                                : getString(R.string.invitation_code_unavailable);
                        Snackbar.make(binding.mainRoot, message, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    DocumentSnapshot invitation = task.getResult();
                    Object expiryValue = invitation.get("expiresAt");
                    if (!Boolean.TRUE.equals(invitation.getBoolean("active"))
                            || !messId.equals(invitation.getString("messId"))
                            || !(expiryValue instanceof Number)) {
                        Snackbar.make(binding.mainRoot, R.string.invitation_code_unavailable,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    long expiresAt = ((Number) expiryValue).longValue();
                    if (expiresAt <= System.currentTimeMillis()) {
                        Snackbar.make(binding.mainRoot, R.string.invitation_code_expired,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (currentMess == null || !messId.equals(currentMess.getMessId())
                            || !code.equals(currentMess.getInvitationCode())) {
                        Snackbar.make(binding.mainRoot, R.string.invitation_code_unavailable,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(this, ContactPickerActivity.class);
                    intent.putExtra(ContactPickerActivity.EXTRA_MESS_NAME, messName);
                    intent.putExtra(ContactPickerActivity.EXTRA_INVITATION_CODE, code);
                    intent.putExtra(ContactPickerActivity.EXTRA_EXPIRES_AT, expiresAt);
                    startActivity(intent);
                });
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
        MealReminderScheduler.cancel(this);
        Intent intent = new Intent(this, ChooseActivity.class);
        intent.putExtra(AuthExtras.EXTRA_ROLE,
                checkIsManager() ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openWelcome() {
        MealReminderScheduler.cancel(this);
        Intent intent = new Intent(this, WelcomeActivity.class);
        if (currentProfile != null && currentProfile.getRole() != null) {
            intent.putExtra(AuthExtras.EXTRA_ROLE, currentProfile.getRole());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startPresence() {
        presenceStarted = true;
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            return;
        }
        presenceUid = user.getUid();

        try {
            connectedReference = FirebaseDatabase.getInstance().getReference(".info/connected");
        } catch (DatabaseException error) {
            Log.e("MainActivity", "Realtime Database is not configured", error);
            Snackbar.make(binding.getRoot(), R.string.realtime_database_not_configured,
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                realtimeConnected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (realtimeConnected) {
                    registerOwnPresence();
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                Log.e("MainActivity", "Realtime Database connection listener was cancelled",
                        error.toException());
                if (presenceStarted && binding != null) {
                    Snackbar.make(binding.getRoot(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            }
        };
        connectedReference.addValueEventListener(connectionListener);
    }

    private void setPresenceMessId(String messId) {
        if (presenceMessId != null && !presenceMessId.equals(messId)) {
            setPresenceOffline(ownPresenceReference);
            ownPresenceReference = null;
        }
        presenceMessId = messId;
        stopMemberPresenceListener();
        startMemberPresenceListener();
        if (presenceStarted && realtimeConnected) {
            registerOwnPresence();
        }
    }

    private void startMemberPresenceListener() {
        if (!presenceStarted || presenceMessId == null || presenceMessId.trim().isEmpty()
                || memberPresenceListener != null) {
            return;
        }
        try {
            memberPresenceReference = FirebaseDatabase.getInstance().getReference()
                    .child("presence").child(presenceMessId);
            memberPresenceListener = new ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    Map<String, MemberAdapter.PresenceStatus> presenceData = new HashMap<>();
                    for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                        Boolean online = memberSnapshot.child("online").getValue(Boolean.class);
                        Long lastSeen = memberSnapshot.child("lastSeen").getValue(Long.class);
                        presenceData.put(memberSnapshot.getKey(),
                                new MemberAdapter.PresenceStatus(Boolean.TRUE.equals(online),
                                        lastSeen != null ? lastSeen.longValue() : 0L));
                    }
                    if (memberAdapter != null) {
                        memberAdapter.setPresenceData(presenceData);
                    }
                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                    memberPresenceListener = null;
                    memberPresenceReference = null;
                    Log.e("MainActivity", "Member presence listener was cancelled",
                            error.toException());
                    if (presenceStarted && binding != null) {
                        Snackbar.make(binding.getRoot(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            };
            memberPresenceReference.addValueEventListener(memberPresenceListener);
        } catch (DatabaseException error) {
            Log.e("MainActivity", "Unable to initialize member presence listener", error);
            if (binding != null) {
                Snackbar.make(binding.getRoot(), R.string.realtime_database_not_configured,
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void stopMemberPresenceListener() {
        if (memberPresenceReference != null && memberPresenceListener != null) {
            memberPresenceReference.removeEventListener(memberPresenceListener);
        }
        memberPresenceReference = null;
        memberPresenceListener = null;
        if (memberAdapter != null) {
            memberAdapter.setPresenceData(null);
        }
    }

    private void registerOwnPresence() {
        if (!presenceStarted || !realtimeConnected || presenceMessId == null
                || presenceUid == null || presenceMessId.trim().isEmpty()) {
            return;
        }
        if (ownPresenceReference == null) {
            try {
                ownPresenceReference = FirebaseDatabase.getInstance().getReference()
                        .child("presence").child(presenceMessId).child(presenceUid);
            } catch (DatabaseException error) {
                Log.e("MainActivity", "Unable to initialize Realtime Database presence", error);
                Snackbar.make(binding.getRoot(), R.string.realtime_database_not_configured,
                        Snackbar.LENGTH_LONG).show();
                return;
            }
        }

        DatabaseReference reference = ownPresenceReference;
        reference.onDisconnect().setValue(createPresenceValue(false))
                .addOnSuccessListener(unused -> {
                    if (presenceStarted && realtimeConnected
                            && reference == ownPresenceReference) {
                        reference.setValue(createPresenceValue(true))
                                .addOnFailureListener(error ->
                                        reportPresenceWriteFailure("set online", error));
                    }
                })
                .addOnFailureListener(error ->
                        reportPresenceWriteFailure("register onDisconnect", error));
    }

    private Map<String, Object> createPresenceValue(boolean online) {
        Map<String, Object> value = new HashMap<>();
        value.put("online", online);
        value.put("lastSeen", ServerValue.TIMESTAMP);
        return value;
    }

    private void setPresenceOffline(DatabaseReference reference) {
        if (reference == null) {
            return;
        }
        reference.setValue(createPresenceValue(false))
                .addOnFailureListener(error -> reportPresenceWriteFailure("set offline", error));
    }

    private void stopPresence() {
        presenceStarted = false;
        realtimeConnected = false;
        stopMemberPresenceListener();
        setPresenceOffline(ownPresenceReference);
        ownPresenceReference = null;
        if (connectedReference != null && connectionListener != null) {
            connectedReference.removeEventListener(connectionListener);
        }
        connectedReference = null;
        connectionListener = null;
    }

    private void reportPresenceWriteFailure(String operation, Exception error) {
        Log.e("MainActivity", "Unable to " + operation + " presence", error);
        if (presenceStarted && binding != null) {
            String message = error.getLocalizedMessage() != null
                    ? error.getLocalizedMessage() : getString(R.string.err_generic, operation);
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void scheduleMealReminder(Mess mess) {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            MealReminderScheduler.schedule(this, user.getUid(), mess);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        android.content.SharedPreferences preferences =
                getSharedPreferences("notification_permissions", MODE_PRIVATE);
        if (!preferences.getBoolean("meal_reminder_requested", false)) {
            preferences.edit().putBoolean("meal_reminder_requested", true).apply();
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onDestroy() {
        stopPresence();
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
        timeCheckRepository.close();
        profilePhotoExecutor.shutdown();
        reportExecutor.shutdown();
        super.onDestroy();
    }
}
