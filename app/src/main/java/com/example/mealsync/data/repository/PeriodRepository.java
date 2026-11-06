package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.Period;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PeriodRepository {

    public interface PeriodCallback {
        void onSuccess(Period period);
        void onFailure(Exception error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    private final FirebaseFirestore db;

    public PeriodRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public PeriodRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void getPeriod(String messId, String periodId, PeriodCallback callback) {
        db.collection("messes").document(messId).collection("periods").document(periodId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Period period = task.getResult().toObject(Period.class);
                        callback.onSuccess(period);
                    } else if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void deleteArchivedPeriod(String messId, String periodId, ActionCallback callback) {
        DocumentReference periodRef = db.collection("messes").document(messId).collection("periods").document(periodId);
        periodRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (callback != null) callback.onFailure(task.getException());
            }
        });
    }

    public void ensurePeriodSetup(Mess mess, ActionCallback callback) {
        if (mess == null || mess.getMessId() == null) {
            return;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(mess.getTimezone() != null ? mess.getTimezone() : "Asia/Dhaka");
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Dhaka");
        }

        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String periodId = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        WriteBatch batch = db.batch();
        Map<String, Object> periodData = new HashMap<>();
        periodData.put("periodId", periodId);
        periodData.put("status", "open");
        periodData.put("startDate", startDate);
        periodData.put("endDate", endDate);
        periodData.put("openingBalances", Collections.emptyMap());
        periodData.put("createdAt", FieldValue.serverTimestamp());

        batch.set(db.collection("messes").document(mess.getMessId()).collection("periods").document(periodId), periodData);
        batch.update(db.collection("messes").document(mess.getMessId()), "currentPeriodId", periodId);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (callback != null) callback.onFailure(task.getException());
            }
        });
    }
}
