package com.example.mealsync.data.repository;

import com.example.mealsync.data.models.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface ExpensesListener {
        void onExpensesUpdated(List<Expense> expenses);
        void onError(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ExpenseRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public ExpenseRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public ListenerRegistration observeExpenses(String messId, String periodId, ExpensesListener listener) {
        return db.collection("messes").document(messId).collection("expenses")
                .whereEqualTo("periodId", periodId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Expense> expenses = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Expense expense = doc.toObject(Expense.class);
                            if (expense != null && "fixed".equalsIgnoreCase(expense.getType())) {
                                expenses.add(expense);
                            }
                        }
                        // Sort by expenseDate descending in memory (zero composite index)
                        Collections.sort(expenses, (e1, e2) -> {
                            if (e1.getExpenseDate() == null) return 1;
                            if (e2.getExpenseDate() == null) return -1;
                            return e2.getExpenseDate().compareTo(e1.getExpenseDate());
                        });
                        listener.onExpensesUpdated(expenses);
                    }
                });
    }

    public void addExpense(String messId, Expense expense, ActionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", expense.getId());
        data.put("amountPaisa", expense.getAmountPaisa());
        data.put("description", expense.getDescription());
        data.put("category", expense.getCategory());
        data.put("type", expense.getType());
        data.put("expenseDate", expense.getExpenseDate());
        data.put("periodId", expense.getPeriodId());
        data.put("createdBy", currentUser.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());
        if (expense.getCorrectsId() != null) {
            data.put("correctsId", expense.getCorrectsId());
        }

        db.collection("messes").document(messId).collection("expenses").document(expense.getId())
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
