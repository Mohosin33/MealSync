package com.example.mealsync.ui.finance;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mealsync.R;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.repository.DepositRepository;
import com.example.mealsync.data.repository.MessRepository;
import com.example.mealsync.databinding.ActivityAddDepositBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.example.mealsync.util.MoneyUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddDepositActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "extra_mess_id";
    public static final String EXTRA_CORRECTS_ID = "extra_corrects_id";

    private ActivityAddDepositBinding binding;
    private final DepositRepository depositRepository = new DepositRepository();
    private final MessRepository messRepository = new MessRepository();

    private String messId;
    private String correctsId;
    private String depositFormId; // UUID generated when form opens for deduplication
    private Mess currentMess;
    private String currentUid;
    private boolean isManager;
    private boolean membersLoaded;
    private boolean isSaving = false;

    private final List<Member> activeMembers = new ArrayList<>();
    private final List<String> memberNames = new ArrayList<>();
    private final String[] methods = new String[]{"cash", "bank", "bkash", "nagad", "other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityAddDepositBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tilMember.setVisibility(View.GONE);
        WindowCompat.getInsetsController(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(false);

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);
        correctsId = getIntent().getStringExtra(EXTRA_CORRECTS_ID);
        depositFormId = UUID.randomUUID().toString(); // Persistent ID across retries
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        applyWindowInsets();
        setupDropdowns();
        setupListeners();
        loadMessAndMembers();
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

    private void setupDropdowns() {
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, methods);
        binding.actMethod.setAdapter(methodAdapter);
        binding.actMethod.setText(methods[0], false);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSaveDeposit.setOnClickListener(v -> saveDeposit());

        SimpleTextWatcher clearWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                binding.tilAmount.setError(null);
                binding.tilDescription.setError(null);
                binding.tilMember.setError(null);
            }
        };
        binding.etAmount.addTextChangedListener(clearWatcher);
        binding.etDescription.addTextChangedListener(clearWatcher);
    }

    private void loadMessAndMembers() {
        if (messId == null) return;

        messRepository.observeMess(messId, new MessRepository.MessListener() {
            @Override
            public void onMessUpdated(Mess mess) {
                currentMess = mess;
            }

            @Override
            public void onError(Exception error) {
            }
        });

        messRepository.observeMembers(messId, new MessRepository.MembersListener() {
            @Override
            public void onMembersUpdated(List<Member> members) {
                membersLoaded = true;
                activeMembers.clear();
                memberNames.clear();
                isManager = false;
                if (members != null) {
                    for (Member m : members) {
                        activeMembers.add(m);
                        memberNames.add(m.getName() != null ? m.getName() : "Member");
                        if (currentUid != null && currentUid.equals(m.getUid())) {
                            isManager = "manager".equalsIgnoreCase(m.getRole());
                        }
                    }
                }
                binding.tilMember.setVisibility(isManager ? View.VISIBLE : View.GONE);
                ArrayAdapter<String> memberAdapter = new ArrayAdapter<>(AddDepositActivity.this,
                        android.R.layout.simple_dropdown_item_1line, memberNames);
                binding.actMember.setAdapter(memberAdapter);
                if (isManager && !memberNames.isEmpty()) {
                    binding.actMember.setText(memberNames.get(0), false);
                }
            }

            @Override
            public void onError(Exception error) {
                String message = error != null && error.getLocalizedMessage() != null
                        ? error.getLocalizedMessage() : "Failed to load mess members";
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void saveDeposit() {
        if (isSaving) return;
        if (!membersLoaded) {
            Snackbar.make(binding.getRoot(), "Member data is still loading.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Member targetMember = null;
        if (isManager) {
            String selectedMemberName = binding.actMember.getText() != null
                    ? binding.actMember.getText().toString() : "";
            for (int i = 0; i < memberNames.size(); i++) {
                if (memberNames.get(i).equalsIgnoreCase(selectedMemberName)) {
                    targetMember = activeMembers.get(i);
                    break;
                }
            }
        } else if (currentUid != null) {
            for (Member member : activeMembers) {
                if (currentUid.equals(member.getUid())) {
                    targetMember = member;
                    break;
                }
            }
        }

        if (targetMember == null) {
            if (isManager) {
                binding.tilMember.setError(getString(R.string.err_select_member));
            } else {
                Snackbar.make(binding.getRoot(), "Your member profile could not be found.", Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        String amountStr = binding.etAmount.getText() != null ? binding.etAmount.getText().toString().trim() : "";
        String method = binding.actMethod.getText() != null ? binding.actMethod.getText().toString().trim().toLowerCase() : "cash";
        String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";

        long amountPaisa;
        try {
            amountPaisa = MoneyUtils.parseTakaToPaisa(amountStr);
        } catch (IllegalArgumentException e) {
            binding.tilAmount.setError(getString(R.string.err_generic, "Invalid amount"));
            return;
        }

        if (currentMess == null || currentMess.getCurrentPeriodId() == null) {
            Snackbar.make(binding.getRoot(), "Period not initialized", Snackbar.LENGTH_LONG).show();
            return;
        }

        isSaving = true;
        setLoading(true);

        String periodId = currentMess.getCurrentPeriodId();
        Deposit deposit = new Deposit(depositFormId, targetMember.getUid(), amountPaisa, method, description, periodId, null, correctsId);

        depositRepository.addDeposit(messId, deposit, new DepositRepository.ActionCallback() {
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
                        ? error.getLocalizedMessage() : getString(R.string.err_generic, "Failed to save deposit");
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveDeposit.setEnabled(!loading);
        binding.btnBack.setEnabled(!loading);
    }
}
