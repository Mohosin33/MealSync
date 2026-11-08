package com.example.mealsync.ui.contacts;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.view.View;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mealsync.R;
import com.example.mealsync.databinding.ActivityContactPickerBinding;
import com.example.mealsync.ui.common.SimpleTextWatcher;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactPickerActivity extends AppCompatActivity {

    private static final String TAG = "ContactPicker";
    public static final String EXTRA_MESS_NAME = "extra_mess_name";
    public static final String EXTRA_INVITATION_CODE = "extra_invitation_code";
    public static final String EXTRA_EXPIRES_AT = "extra_expires_at";

    private ActivityContactPickerBinding binding;
    private final ExecutorService contactsExecutor = Executors.newSingleThreadExecutor();
    private final List<ContactEntry> allContacts = new ArrayList<>();
    private ContactAdapter adapter;
    private String messName;
    private String invitationCode;
    private long expiresAt;
    private boolean contactsLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityContactPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        messName = getIntent().getStringExtra(EXTRA_MESS_NAME);
        invitationCode = getIntent().getStringExtra(EXTRA_INVITATION_CODE);
        expiresAt = getIntent().getLongExtra(EXTRA_EXPIRES_AT, -1L);
        if (invitationCode == null || expiresAt <= 0L) {
            finish();
            return;
        }

        adapter = new ContactAdapter(this::sendInvite);
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvContacts.setAdapter(adapter);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                filterContacts(editable == null ? "" : editable.toString());
            }
        });
        loadContacts();
    }

    private void loadContacts() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.tvContactsState.setVisibility(View.GONE);
        binding.rvContacts.setVisibility(View.GONE);
        contactsExecutor.execute(() -> {
            List<ContactEntry> loaded;
            try {
                loaded = queryContacts();
                runOnUiThread(() -> showContacts(loaded));
            } catch (SecurityException | IllegalArgumentException | IllegalStateException error) {
                Log.e(TAG, "Could not read contacts from the contacts provider", error);
                runOnUiThread(this::showContactsError);
            }
        });
    }

    private List<ContactEntry> queryContacts() {
        List<ContactEntry> contacts = new ArrayList<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) {
                return contacts;
            }
            int nameIndex = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                if (number == null || number.trim().isEmpty()) {
                    continue;
                }
                String displayName = name == null || name.trim().isEmpty()
                        ? getString(R.string.contact_without_name) : name.trim();
                contacts.add(new ContactEntry(displayName, number.trim()));
            }
        }
        return contacts;
    }

    private void showContacts(List<ContactEntry> contacts) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        binding.progress.setVisibility(View.GONE);
        allContacts.clear();
        allContacts.addAll(contacts);
        contactsLoaded = true;
        filterContacts(binding.etSearch.getText() == null
                ? "" : binding.etSearch.getText().toString());
    }

    private void showContactsError() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        binding.progress.setVisibility(View.GONE);
        binding.rvContacts.setVisibility(View.GONE);
        binding.tvContactsState.setVisibility(View.VISIBLE);
        binding.tvContactsState.setText(R.string.contacts_load_failed);
    }

    private void filterContacts(String query) {
        if (!contactsLoaded) {
            return;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<ContactEntry> filtered = new ArrayList<>();
        for (ContactEntry contact : allContacts) {
            if (contact.getDisplayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || contact.getPhoneNumber().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                filtered.add(contact);
            }
        }
        adapter.setContacts(filtered);
        boolean hasContacts = !filtered.isEmpty();
        binding.rvContacts.setVisibility(hasContacts ? View.VISIBLE : View.GONE);
        binding.tvContactsState.setVisibility(hasContacts ? View.GONE : View.VISIBLE);
        binding.tvContactsState.setText(normalizedQuery.isEmpty()
                ? R.string.contacts_empty : R.string.contacts_search_empty);
    }

    private void sendInvite(ContactEntry contact) {
        String expiry = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(expiresAt));
        String message = getString(R.string.invite_contact_sms, messName, invitationCode, expiry);
        Intent sendSms = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("smsto", contact.getPhoneNumber(), null));
        sendSms.putExtra("sms_body", message);
        try {
            startActivity(sendSms);
        } catch (ActivityNotFoundException error) {
            Snackbar.make(binding.getRoot(), R.string.no_sms_app_available,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        contactsExecutor.shutdownNow();
        super.onDestroy();
    }
}
