package com.example.mealsync;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.databinding.ActivityMainBinding;
import com.example.mealsync.ui.auth.WelcomeActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * TEMPORARY placeholder. Shows who is signed in and offers Log out.
 * It becomes the real dashboard on Day 3.
 */
public class MainActivity extends AppCompatActivity {

    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            openWelcome();
            return;
        }

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int pad = Math.round(24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left + pad, bars.top + pad, bars.right + pad, bars.bottom + pad);
            return insets;
        });

        binding.tvSignedInAs.setText(getString(R.string.signed_in_as, describe(user)));
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.signOut();
            openWelcome();
        });
    }

    private String describe(FirebaseUser user) {
        String phone = user.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            return phone;
        }
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            return email;
        }
        return user.getUid();
    }

    private void openWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}