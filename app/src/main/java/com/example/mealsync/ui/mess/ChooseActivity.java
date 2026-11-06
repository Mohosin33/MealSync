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
        boolean isManager = AuthExtras.ROLE_MANAGER.equalsIgnoreCase(role);
        binding.cardCreateMess.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.cardJoinMess.setVisibility(isManager ? View.GONE : View.VISIBLE);
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            Intent intent = new Intent(this, WelcomeActivity.class);
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
