package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.Deposit;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepositRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface DepositsListener {
        void onDepositsUpdated(List<Deposit> deposits);
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public DepositRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public DepositRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public ListenerRegistration observeDeposits(String messId, String periodId, boolean isManager, String currentUid, DepositsListener listener) {
        Query query = db.collection("messes").document(messId).collection("deposits")
                .whereEqualTo("periodId", periodId);

        if (!isManager) {
            query = query.whereEqualTo("memberId", currentUid);
        }

        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot != null) {
                List<Deposit> deposits = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Deposit deposit = doc.toObject(Deposit.class);
                    if (deposit != null) {
                        deposits.add(deposit);
                    }
                }
                listener.onDepositsUpdated(deposits);
            }
        });
    }

    public void addDeposit(String messId, Deposit deposit, boolean isManager, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        DocumentReference docRef = db.collection("messes").document(messId).collection("deposits").document(deposit.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("id", deposit.getId());
        data.put("memberId", deposit.getMemberId());
        data.put("amountPaisa", deposit.getAmountPaisa());
        data.put("method", deposit.getMethod());
        data.put("description", deposit.getDescription());
        data.put("periodId", deposit.getPeriodId());
        data.put("status", isManager ? "approved" : "pending");
        data.put("createdBy", currentUser.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());
        if (deposit.getCorrectsId() != null) {
            data.put("correctsId", deposit.getCorrectsId());
        }

        docRef.set(data).addOnCompleteListener(setTask -> {
            if (setTask.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(setTask.getException());
            }
        });
    }

    public void updateDepositStatus(String messId, String depositId, String status, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            callback.onFailure(new IllegalArgumentException("Invalid deposit status"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("reviewedBy", currentUser.getUid());
        updates.put("reviewedAt", FieldValue.serverTimestamp());

        db.collection("messes").document(messId).collection("deposits").document(depositId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
