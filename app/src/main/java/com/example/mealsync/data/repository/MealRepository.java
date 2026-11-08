package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.DayTotals;
import com.example.mealsync.data.models.Meal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealRepository {

    public interface SetMealCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface MealListener {
        void onMealUpdated(Meal meal);
        void onError(Exception error);
    }

    public interface MyMealsListener {
        void onMealsUpdated(List<Meal> meals);
        void onError(Exception error);
    }

    public interface DayTotalsListener {
        void onTotalsUpdated(DayTotals totals);
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MealRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public MealRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public ListenerRegistration observeMyMeal(String messId, String date, MealListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onError(new Exception("User not signed in"));
            return null;
        }

        String uid = currentUser.getUid();
        String docId = date + "_" + uid;
        DocumentReference docRef = db.collection("messes").document(messId).collection("meals").document(docId);

        return docRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Meal meal = snapshot.toObject(Meal.class);
                if (meal != null) {
                    meal.setPending(snapshot.getMetadata().hasPendingWrites());
                    listener.onMealUpdated(meal);
                } else {
                    Meal defaultMeal = new Meal(date, uid, true, true, 0);
                    defaultMeal.setPending(snapshot.getMetadata().hasPendingWrites());
                    listener.onMealUpdated(defaultMeal);
                }
            } else if (snapshot != null) {
                Meal defaultMeal = new Meal(date, uid, true, true, 0);
                defaultMeal.setPending(snapshot.getMetadata().hasPendingWrites());
                listener.onMealUpdated(defaultMeal);
                db.runTransaction(transaction -> {
                    DocumentSnapshot currentSnapshot = transaction.get(docRef);
                    if (!currentSnapshot.exists()) {
                        transaction.set(docRef, createMealData(date, uid, true, true, 0));
                    }
                    return null;
                }).addOnFailureListener(listener::onError);
            }
        });
    }

    public void setMeal(String messId, String date, boolean lunch, boolean dinner, int guests, SetMealCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        String uid = currentUser.getUid();
        String docId = date + "_" + uid;
        int clampedGuests = Math.max(0, Math.min(10, guests));

        DocumentReference docRef = db.collection("messes").document(messId).collection("meals").document(docId);
        docRef.set(createMealData(date, uid, lunch, dinner, clampedGuests)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    private Map<String, Object> createMealData(String date, String uid, boolean lunch, boolean dinner, int guests) {
        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("uid", uid);
        data.put("lunch", lunch);
        data.put("dinner", dinner);
        data.put("guests", guests);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    public ListenerRegistration observeMyMeals(String messId, String uid, MyMealsListener listener) {
        return db.collection("messes").document(messId).collection("meals")
                .whereEqualTo("uid", uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Meal> meals = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Meal meal = doc.toObject(Meal.class);
                            if (meal != null) {
                                meal.setPending(doc.getMetadata().hasPendingWrites());
                                meals.add(meal);
                            }
                        }
                        // Sort in-memory by date descending to avoid needing composite index
                        Collections.sort(meals, (m1, m2) -> {
                            if (m1.getDate() == null) return 1;
                            if (m2.getDate() == null) return -1;
                            return m2.getDate().compareTo(m1.getDate());
                        });
                        listener.onMealsUpdated(meals);
                    }
                });
    }

    public ListenerRegistration observeDayTotals(String messId, String date, DayTotalsListener listener) {
        return db.collection("messes").document(messId).collection("meals")
                .whereEqualTo("date", date)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot != null) {
                        int totalLunch = 0;
                        int totalDinner = 0;
                        int totalGuests = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Meal m = doc.toObject(Meal.class);
                            if (m != null) {
                                if (m.isLunch()) totalLunch++;
                                if (m.isDinner()) totalDinner++;
                                totalGuests += m.getGuests();
                            }
                        }
                        listener.onTotalsUpdated(new DayTotals(totalLunch, totalDinner, totalGuests));
                    }
                });
    }
}
