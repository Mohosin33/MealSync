package com.example.mealsync;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityMainBinding;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.example.mealsync.ui.mess.ChooseActivity;
import com.example.mealsync.ui.mess.MemberAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MemberAdapter.OnRemoveMemberClickListener {

    private ActivityMainBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private final MessRepository messRepository = new MessRepository();

    private UserProfile currentProfile;
    private Mess currentMess;
    private MemberAdapter memberAdapter;
    private ListenerRegistration messListener;
    private ListenerRegistration membersListener;

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
        binding.tabMembers.setVisibility(tabId == R.id.tabMembers ? View.VISIBLE : View.GONE);
        binding.tabProfile.setVisibility(tabId == R.id.tabProfile ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            openWelcome();
        });

        binding.btnCopyCode.setOnClickListener(v -> copyInvitationCode());
        binding.btnShareCode.setOnClickListener(v -> shareInvitationCode());
        binding.btnRevokeCode.setOnClickListener(v -> revokeInvitationCode());
        binding.btnRegenerateCode.setOnClickListener(v -> regenerateInvitationCode());
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
                populateMessUI(mess);
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
    }

    private void populateProfileUI(UserProfile profile) {
        binding.tvProfileName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "User");
        binding.tvProfileEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
        boolean isManager = "manager".equalsIgnoreCase(profile.getRole());
        binding.tvProfileRole.setText(isManager ? R.string.role_badge_manager : R.string.role_badge_member);
    }

    private void populateMessUI(Mess mess) {
        if (mess == null) return;
        binding.tvHomeMessName.setText(mess.getName());
        binding.tvHomeSubDetails.setText(getString(R.string.timezone_hint) + ": " + mess.getTimezone()
                + " | " + getString(R.string.cutoff_time_hint) + ": " + mess.getCutoffTime());

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
                .setPositiveButton(R.string.remove_member, (dialog, which) -> {
                    messRepository.removeMember(currentMess.getMessId(), member.getUid(), new MessRepository.ActionCallback() {
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
                    });
                })
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
        if (messListener != null) messListener.remove();
        if (membersListener != null) membersListener.remove();
        super.onDestroy();
    }
}
