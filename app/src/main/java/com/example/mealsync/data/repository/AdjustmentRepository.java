package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.Adjustment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface AdjustmentsListener {
        void onAdjustmentsUpdated(List<Adjustment> adjustments);
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public AdjustmentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public AdjustmentRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public ListenerRegistration observeAdjustments(String messId, String periodId, AdjustmentsListener listener) {
        return db.collection("messes").document(messId).collection("adjustments")
                .whereEqualTo("periodId", periodId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Adjustment> adjustments = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Adjustment adj = doc.toObject(Adjustment.class);
                            if (adj != null) {
                                adjustments.add(adj);
                            }
                        }
                        listener.onAdjustmentsUpdated(adjustments);
                    }
                });
    }

    public ListenerRegistration observeAdjustments(String messId, String periodId,
                                                   boolean isManager, String currentUid,
                                                   AdjustmentsListener listener) {
        if (isManager) {
            return observeAdjustments(messId, periodId, listener);
        }

        Map<String, Adjustment> visibleAdjustments = new LinkedHashMap<>();
        ListenerRegistration reversalListener = db.collection("messes").document(messId)
                .collection("adjustments")
                .whereEqualTo("periodId", periodId)
                .whereEqualTo("type", "reversal")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    updateVisibleAdjustments(snapshot, visibleAdjustments, "reversal", listener);
                });

        ListenerRegistration memberAdjustmentListener = db.collection("messes").document(messId)
                .collection("adjustments")
                .whereEqualTo("periodId", periodId)
                .whereEqualTo("memberId", currentUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    updateVisibleAdjustments(snapshot, visibleAdjustments, "member_adjustment", listener);
                });

        return () -> {
            reversalListener.remove();
            memberAdjustmentListener.remove();
        };
    }

    private void updateVisibleAdjustments(com.google.firebase.firestore.QuerySnapshot snapshot,
                                          Map<String, Adjustment> visibleAdjustments,
                                          String type, AdjustmentsListener listener) {
        if (snapshot == null) return;

        synchronized (visibleAdjustments) {
            for (com.google.firebase.firestore.DocumentChange change : snapshot.getDocumentChanges()) {
                String adjustmentId = change.getDocument().getId();
                if (change.getType() == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                    visibleAdjustments.remove(adjustmentId);
                    continue;
                }

                Adjustment adjustment = change.getDocument().toObject(Adjustment.class);
                if (adjustment != null && type.equalsIgnoreCase(adjustment.getType())) {
                    visibleAdjustments.put(adjustmentId, adjustment);
                }
            }
            listener.onAdjustmentsUpdated(new ArrayList<>(visibleAdjustments.values()));
        }
    }

    public void addAdjustment(String messId, Adjustment adjustment, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", adjustment.getId());
        data.put("type", adjustment.getType());
        data.put("targetType", adjustment.getTargetType());
        data.put("targetId", adjustment.getTargetId());
        data.put("memberId", adjustment.getMemberId());
        data.put("amountPaisa", adjustment.getAmountPaisa());
        data.put("reason", adjustment.getReason());
        data.put("periodId", adjustment.getPeriodId());
        data.put("createdBy", currentUser.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("messes").document(messId).collection("adjustments").document(adjustment.getId())
                .set(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
