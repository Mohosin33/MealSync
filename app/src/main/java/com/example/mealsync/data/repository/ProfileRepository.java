package com.example.mealsync.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ProfileRepository {

    public interface Callback {
        void onSuccess();
        void onFailure(Exception error);
    }

    private final FirebaseFirestore db;

    public ProfileRepository() {
        this(FirebaseFirestore.getInstance());
    }

    ProfileRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void updateProfile(String uid, String messId, String displayName, Callback callback) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("displayName", displayName);

        WriteBatch batch = db.batch();
        batch.update(db.collection("users").document(uid), userUpdates);
        batch.update(db.collection("messes").document(messId)
                .collection("members").document(uid), "name", displayName);
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}
