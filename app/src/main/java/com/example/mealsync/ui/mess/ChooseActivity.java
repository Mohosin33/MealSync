package com.example.mealsync.ui.mess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.data.models.UserProfile;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityChooseBinding;
import com.example.mealsync.ui.auth.AuthExtras;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.example.mealsync.ui.common.StickyHeaderBehavior;
import com.google.firebase.auth.FirebaseUser;

public class ChooseActivity extends AppCompatActivity {

    private ActivityChooseBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private String selectedRole = AuthExtras.ROLE_MEMBER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityChooseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        applyWindowInsets();
        StickyHeaderBehavior.attach(binding.scrollContent, binding.header, com.example.mealsync.R.drawable.bg_gradient_member);
        setupListeners();
        binding.cardCreateMess.setVisibility(View.GONE);
        binding.cardJoinMess.setVisibility(View.GONE);
        applyRoleVisibility();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int pad = Math.round(24 * getResources().getDisplayMetrics().density);
            binding.header.setPadding(pad, bars.top + Math.round(16 * getResources().getDisplayMetrics().density), pad, pad);
            binding.sheet.setPadding(pad, pad, pad, pad + bars.bottom);
            return insets;
        });
    }

    private void applyRoleVisibility() {
        String role = getIntent().getStringExtra(AuthExtras.EXTRA_ROLE);
        if (role == null) {
            FirebaseUser user = authRepository.getCurrentUser();
            if (user != null) {
                new MessRepository().getUserProfile(user.getUid(), new MessRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile profile) {
                        if (profile != null && profile.getRole() != null) {
                            renderOptionCards(profile.getRole());
                        } else {
                            renderOptionCards(AuthExtras.ROLE_MEMBER);
                        }
                    }

                    @Override
                    public void onFailure(Exception error) {
                        renderOptionCards(AuthExtras.ROLE_MEMBER);
                    }
                });
                return;
            }
        }
        renderOptionCards(role);
    }

    private void renderOptionCards(String role) {
        selectedRole = AuthExtras.ROLE_MANAGER.equalsIgnoreCase(role)
                ? AuthExtras.ROLE_MANAGER : AuthExtras.ROLE_MEMBER;
        boolean isManager = AuthExtras.ROLE_MANAGER.equalsIgnoreCase(role);
        binding.cardCreateMess.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.cardJoinMess.setVisibility(isManager ? View.GONE : View.VISIBLE);
        if (isManager) {
            binding.tvChooseSubtitle.setText(com.example.mealsync.R.string.choose_manager_subtitle);
            binding.tvCreateMessTitle.setText(com.example.mealsync.R.string.create_mess_manager_title);
            binding.tvCreateMessDescription.setText(com.example.mealsync.R.string.create_mess_manager_desc);
            binding.btnCreateMess.setText(com.example.mealsync.R.string.create_mess_manager_title);
        } else {
            binding.tvChooseSubtitle.setText(com.example.mealsync.R.string.choose_member_subtitle);
            binding.tvJoinMessTitle.setText(com.example.mealsync.R.string.join_mess_member_title);
            binding.tvJoinMessDescription.setText(com.example.mealsync.R.string.join_mess_member_desc);
            binding.btnJoinMess.setText(com.example.mealsync.R.string.join_mess_member_title);
        }
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.putExtra(AuthExtras.EXTRA_ROLE, selectedRole);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.cardCreateMess.setOnClickListener(v -> openCreateMess());
        binding.btnCreateMess.setOnClickListener(v -> openCreateMess());

        binding.cardJoinMess.setOnClickListener(v -> openJoinMess());
        binding.btnJoinMess.setOnClickListener(v -> openJoinMess());
    }

    private void openCreateMess() {
        startActivity(new Intent(this, CreateMessActivity.class));
    }

    private void openJoinMess() {
        startActivity(new Intent(this, JoinMessActivity.class));
    }
}
