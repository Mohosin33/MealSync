package com.example.mealsync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mealsync.data.models.DayTotals;
import com.example.mealsync.data.models.Meal;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.MealRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityMainBinding;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.example.mealsync.ui.mess.ChooseActivity;
import com.example.mealsync.ui.mess.MealHistoryAdapter;
import com.example.mealsync.ui.mess.MemberAdapter;
import com.example.mealsync.ui.mess.SettingsActivity;
import com.example.mealsync.util.CutoffCalculator;
import com.example.mealsync.util.CutoffResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.Instant;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MemberAdapter.OnRemoveMemberClickListener {

    private ActivityMainBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private final MessRepository messRepository = new MessRepository();
    private final MealRepository mealRepository = new MealRepository();

    private UserProfile currentProfile;
    private Mess currentMess;
    private MemberAdapter memberAdapter;
    private MealHistoryAdapter mealHistoryAdapter;

    private ListenerRegistration messListener;
    private ListenerRegistration membersListener;
    private ListenerRegistration myMealListener;
    private ListenerRegistration myMealsListener;
    private ListenerRegistration dayTotalsListener;

    private CutoffResult currentCutoffResult;
    private Meal tomorrowMeal;
    private boolean isUpdatingControls = false;
    private boolean isSavingMeal = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = this::updateCutoffTimer;

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
        setupNavigation();
        setupListeners();
        loadUserProfile(firebaseUser.getUid());
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
        binding.tabMeals.setVisibility(tabId == R.id.tabMeals ? View.VISIBLE : View.GONE);
        binding.tabMembers.setVisibility(tabId == R.id.tabMembers ? View.VISIBLE : View.GONE);
        binding.tabProfile.setVisibility(tabId == R.id.tabProfile ? View.VISIBLE : View.GONE);
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
                observeMessData(profile.getMessId());
            }

            @Override
            public void onFailure(Exception error) {
                openChooseScreen();
            }
        });
    }

    private void observeMessData(String messId) {
        if (messListener != null) messListener.remove();
        if (membersListener != null) membersListener.remove();

        messListener = messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                currentMess = mess;
                boolean isManager = currentProfile != null && "manager".equalsIgnoreCase(currentProfile.getRole());

                if (mess != null && mess.getUtcOffsetMinutes() == 0 && isManager) {
                    messRepository.ensureMessSetup(mess, null);
                }

                populateMessUI(mess);
                updateMealDashboard();
            }

            @Override
            public void onError(Exception error) {
            }
        });

        boolean isManager = currentProfile != null && "manager".equalsIgnoreCase(currentProfile.getRole());
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
            }

            @Override
            public void onError(Exception error) {
            }
        });

        observePersonalMealHistory(messId, currentUid);
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
                calculateMonthlyCounts(meals);
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
    }

    private void updateMealDashboard() {
        if (currentMess == null) return;

        currentCutoffResult = CutoffCalculator.calculate(
                Instant.now(), currentMess.getTimezone(), currentMess.getCutoffTime()
        );

        binding.tvHomeTodayDate.setText("Today: " + currentCutoffResult.getTodayDate());
        binding.tvTomorrowHeader.setText("Tomorrow's Meals (" + currentCutoffResult.getTomorrowDate() + ")");

        boolean isManager = currentProfile != null && "manager".equalsIgnoreCase(currentProfile.getRole());
        boolean needsSetup = currentMess.getUtcOffsetMinutes() == 0;

        if (needsSetup && !isManager) {
            binding.cardSetupNotice.setVisibility(View.VISIBLE);
            setControlsEnabled(false);
            binding.tvCutoffStatus.setText("Setup incomplete");
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
                binding.tvTotalLunch.setText("Lunch: " + totals.getTotalLunch());
                binding.tvTotalDinner.setText("Dinner: " + totals.getTotalDinner());
                binding.tvTotalGuests.setText("Guests: " + totals.getTotalGuests());
                binding.tvGrandTotal.setText("Grand Total: " + totals.getGrandTotal() + " meals");
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void renderTomorrowMeal(Meal meal) {
        isUpdatingControls = true;
        binding.switchLunch.setChecked(meal.isLunch());
        binding.switchDinner.setChecked(meal.isDinner());
        binding.tvGuestCount.setText(String.valueOf(meal.getGuests()));
        binding.tvMealPending.setVisibility(meal.isPending() ? View.VISIBLE : View.GONE);
        isUpdatingControls = false;

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
            binding.tvCutoffStatus.setText("Closes in " + hours + "h " + mins + "m");
            setControlsEnabled(!isSavingMeal);
            handler.postDelayed(timerRunnable, 30000L);
        } else {
            binding.tvCutoffStatus.setText("Meal editing for tomorrow is closed. Cutoff was " + currentMess.getCutoffTime() + ".");
            setControlsEnabled(false);
        }
    }

    private void submitMealChange(boolean newLunch, boolean newDinner, int newGuests) {
        if (currentMess == null || currentCutoffResult == null || isSavingMeal) return;

        if (!currentCutoffResult.isOpen()) {
            Snackbar.make(binding.mainRoot, "Meal editing is closed", Snackbar.LENGTH_LONG).show();
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
                Snackbar.make(binding.mainRoot, "Meal editing is closed", Snackbar.LENGTH_LONG).show();
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
        boolean isManager = "manager".equalsIgnoreCase(profile.getRole());
        binding.tvProfileRole.setText(isManager ? R.string.role_badge_manager : R.string.role_badge_member);
        binding.btnSettings.setVisibility(isManager ? View.VISIBLE : View.GONE);
    }

    private void populateMessUI(Mess mess) {
        if (mess == null) return;
        binding.tvHomeMessName.setText(mess.getName());

        boolean isManager = currentProfile != null && "manager".equalsIgnoreCase(currentProfile.getRole());
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
                        Snackbar.make(binding.mainRoot, "Member removed", Snackbar.LENGTH_SHORT).show();
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
        super.onDestroy();
    }
}
