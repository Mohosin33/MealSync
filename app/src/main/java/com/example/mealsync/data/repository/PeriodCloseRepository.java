package com.example.mealsync.data.repository;

import com.example.mealsync.close.CloseStep;
import com.example.mealsync.close.MemberResult;
import com.example.mealsync.close.PeriodDates;
import com.example.mealsync.close.PeriodSnapshot;
import com.example.mealsync.close.SnapshotBuilder;
import com.example.mealsync.data.models.Adjustment;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Meal;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.finance.EngineAdjustment;
import com.example.mealsync.finance.EngineDeposit;
import com.example.mealsync.finance.EngineExpense;
import com.example.mealsync.finance.EngineMeal;
import com.example.mealsync.finance.EngineMember;
import com.example.mealsync.finance.FinanceEngine;
import com.example.mealsync.finance.FinanceInput;
import com.example.mealsync.finance.FinanceResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeriodCloseRepository {

    public interface CloseProgressCallback {
        void onStepChanged(CloseStep step, String message);
        void onSuccess(String nextPeriodId);
        void onFailure(Exception error);
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public PeriodCloseRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public PeriodCloseRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    public void executeCloseFlow(Mess mess, Period period, String initialCloseRequestId, CloseProgressCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not signed in"));
            return;
        }

        if (mess == null || period == null || mess.getMessId() == null || period.getPeriodId() == null) {
            callback.onFailure(new Exception("Invalid mess or period configuration"));
            return;
        }

        String messId = mess.getMessId();
        String periodId = period.getPeriodId();
        DocumentReference periodRef = db.collection("messes").document(messId).collection("periods").document(periodId);

        // STEP 1: LOCK
        callback.onStepChanged(CloseStep.LOCK, "Locking period for month close...");

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(periodRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Period document does not exist");
            }

            String currentStatus = snapshot.getString("status");
            String storedReqId = snapshot.getString("closeRequestId");

            if ("closed".equalsIgnoreCase(currentStatus)) {
                return "closed:" + (storedReqId != null ? storedReqId : initialCloseRequestId);
            }

            if ("closing".equalsIgnoreCase(currentStatus)) {
                return "closing:" + (storedReqId != null ? storedReqId : initialCloseRequestId);
            }

            // OPEN -> set to closing
            String reqIdToUse = initialCloseRequestId != null ? initialCloseRequestId : java.util.UUID.randomUUID().toString();
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "closing");
            updates.put("closeRequestId", reqIdToUse);
            updates.put("closingStartedAt", FieldValue.serverTimestamp());

            transaction.update(periodRef, updates);
            return "closing:" + reqIdToUse;
        }).addOnCompleteListener(lockTask -> {
            if (!lockTask.isSuccessful()) {
                callback.onFailure(lockTask.getException());
                return;
            }

            String txResult = lockTask.getResult();
            if (txResult != null && txResult.startsWith("closed:")) {
                callback.onSuccess(PeriodDates.getNextPeriodId(periodId));
                return;
            }

            String activeCloseRequestId = txResult != null && txResult.contains(":")
                    ? txResult.split(":")[1]
                    : initialCloseRequestId;

            // STEP 2: CALCULATE
            callback.onStepChanged(CloseStep.CALCULATE, "Calculating final period financial snapshot...");
            executeStepCalculate(mess, period, activeCloseRequestId, callback);
        });
    }

    private void executeStepCalculate(Mess mess, Period period, String activeCloseRequestId, CloseProgressCallback callback) {
        String messId = mess.getMessId();
        String periodId = period.getPeriodId();

        String startDate = period.getStartDate() != null ? period.getStartDate() : PeriodDates.getFirstDay(periodId);
        String endDate = period.getEndDate() != null ? period.getEndDate() : PeriodDates.getLastDay(periodId);

        // Fetch members, meals, expenses, deposits, adjustments
        db.collection("messes").document(messId).collection("members").get()
                .addOnCompleteListener(memberTask -> {
                    if (!memberTask.isSuccessful()) {
                        callback.onFailure(memberTask.getException());
                        return;
                    }

                    List<DocumentSnapshot> memberDocs = memberTask.getResult() != null ? memberTask.getResult().getDocuments() : new ArrayList<>();

                    db.collection("messes").document(messId).collection("meals")
                            .whereGreaterThanOrEqualTo("date", startDate)
                            .whereLessThanOrEqualTo("date", endDate)
                            .get()
                            .addOnCompleteListener(mealTask -> {
                                if (!mealTask.isSuccessful()) {
                                    callback.onFailure(mealTask.getException());
                                    return;
                                }

                                List<DocumentSnapshot> mealDocs = mealTask.getResult() != null ? mealTask.getResult().getDocuments() : new ArrayList<>();

                                db.collection("messes").document(messId).collection("expenses")
                                        .whereEqualTo("periodId", periodId)
                                        .get()
                                        .addOnCompleteListener(expenseTask -> {
                                            if (!expenseTask.isSuccessful()) {
                                                callback.onFailure(expenseTask.getException());
                                                return;
                                            }

                                            List<DocumentSnapshot> expenseDocs = expenseTask.getResult() != null ? expenseTask.getResult().getDocuments() : new ArrayList<>();

                                            db.collection("messes").document(messId).collection("deposits")
                                                    .whereEqualTo("periodId", periodId)
                                                    .get()
                                                    .addOnCompleteListener(depositTask -> {
                                                        if (!depositTask.isSuccessful()) {
                                                            callback.onFailure(depositTask.getException());
                                                            return;
                                                        }

                                                        List<DocumentSnapshot> depositDocs = depositTask.getResult() != null ? depositTask.getResult().getDocuments() : new ArrayList<>();

                                                        db.collection("messes").document(messId).collection("adjustments")
                                                                .whereEqualTo("periodId", periodId)
                                                                .get()
                                                                .addOnCompleteListener(adjTask -> {
                                                                    if (!adjTask.isSuccessful()) {
                                                                        callback.onFailure(adjTask.getException());
                                                                        return;
                                                                    }

                                                                    List<DocumentSnapshot> adjDocs = adjTask.getResult() != null ? adjTask.getResult().getDocuments() : new ArrayList<>();

                                                                    // Build Engine objects
                                                                    List<EngineMember> engineMembers = new ArrayList<>();
                                                                    Map<String, String> memberNamesMap = new HashMap<>();
                                                                    for (DocumentSnapshot doc : memberDocs) {
                                                                        Member m = doc.toObject(Member.class);
                                                                        if (m != null) {
                                                                            engineMembers.add(new EngineMember(m.getUid(), true, m.isIncludeInFixed(), m.getJoinedAt(), null));
                                                                            memberNamesMap.put(m.getUid(), m.getName() != null ? m.getName() : "Member");
                                                                        }
                                                                    }

                                                                    Map<String, int[]> memberMealCounts = new HashMap<>();
                                                                    for (DocumentSnapshot doc : mealDocs) {
                                                                        Meal meal = doc.toObject(Meal.class);
                                                                        if (meal != null && meal.getUid() != null) {
                                                                            String uid = meal.getUid();
                                                                            int[] counts = memberMealCounts.computeIfAbsent(uid, k -> new int[]{0, 0, 0});
                                                                            if (meal.isLunch()) counts[0]++;
                                                                            if (meal.isDinner()) counts[1]++;
                                                                            counts[2] += meal.getGuests();
                                                                        }
                                                                    }

                                                                    List<EngineMeal> engineMeals = new ArrayList<>();
                                                                    for (Map.Entry<String, int[]> entry : memberMealCounts.entrySet()) {
                                                                        int[] c = entry.getValue();
                                                                        engineMeals.add(new EngineMeal(entry.getKey(), c[0], c[1], c[2]));
                                                                    }

                                                                    List<EngineExpense> engineExpenses = new ArrayList<>();
                                                                    for (DocumentSnapshot doc : expenseDocs) {
                                                                        Expense e = doc.toObject(Expense.class);
                                                                        if (e != null) {
                                                                            engineExpenses.add(new EngineExpense(e.getId(), e.getAmountPaisa(), e.getType()));
                                                                        }
                                                                    }

                                                                    List<EngineDeposit> engineDeposits = new ArrayList<>();
                                                                    for (DocumentSnapshot doc : depositDocs) {
                                                                        Deposit d = doc.toObject(Deposit.class);
                                                                        if (d != null) {
                                                                            engineDeposits.add(new EngineDeposit(d.getId(), d.getMemberId(), d.getAmountPaisa()));
                                                                        }
                                                                    }

                                                                    List<EngineAdjustment> engineAdjustments = new ArrayList<>();
                                                                    for (DocumentSnapshot doc : adjDocs) {
                                                                        Adjustment a = doc.toObject(Adjustment.class);
                                                                        if (a != null) {
                                                                            if ("reversal".equalsIgnoreCase(a.getType())) {
                                                                                engineAdjustments.add(EngineAdjustment.createReversal(a.getId(), a.getTargetType(), a.getTargetId()));
                                                                            } else if ("member_adjustment".equalsIgnoreCase(a.getType())) {
                                                                                engineAdjustments.add(EngineAdjustment.createMemberAdjustment(a.getId(), a.getMemberId(), a.getAmountPaisa()));
                                                                            }
                                                                        }
                                                                    }

                                                                    Map<String, Long> openingBalances = period.getOpeningBalances() != null ? period.getOpeningBalances() : new HashMap<>();

                                                                    FinanceInput input = new FinanceInput(
                                                                            engineMembers,
                                                                            engineMeals,
                                                                            engineExpenses,
                                                                            engineDeposits,
                                                                            engineAdjustments,
                                                                            openingBalances,
                                                                            mess.getGuestWeightPercent(),
                                                                            "EQUAL",
                                                                            0L, 0L,
                                                                            mess.getMealRatePaisa()
                                                                    );

                                                                    FinanceResult finRes = FinanceEngine.calculate(input);
                                                                    PeriodSnapshot snapshot = SnapshotBuilder.build(periodId, finRes, engineMembers, memberNamesMap);

                                                                    // STEP 3: WRITE RESULTS
                                                                    callback.onStepChanged(CloseStep.WRITE_RESULTS, "Writing member results...");
                                                                    executeStepWriteResults(mess, period, snapshot, activeCloseRequestId, callback);
                                                                });
                                                    });
                                        });
                            });
                });
    }

    private void executeStepWriteResults(Mess mess, Period period, PeriodSnapshot snapshot, String activeCloseRequestId, CloseProgressCallback callback) {
        String messId = mess.getMessId();
        String periodId = period.getPeriodId();

        WriteBatch batch = db.batch();
        for (Map.Entry<String, MemberResult> entry : snapshot.getMemberResults().entrySet()) {
            String uid = entry.getKey();
            MemberResult res = entry.getValue();

            DocumentReference docRef = db.collection("messes").document(messId)
                    .collection("periods").document(periodId)
                    .collection("results").document(uid);

            Map<String, Object> resData = new HashMap<>();
            resData.put("uid", res.getUid());
            resData.put("name", res.getName());
            resData.put("lunches", res.getLunches());
            resData.put("dinners", res.getDinners());
            resData.put("guests", res.getGuests());
            resData.put("countedUnits", res.getCountedUnits());
            resData.put("mealCostPaisa", res.getMealCostPaisa());
            resData.put("fixedSharePaisa", res.getFixedSharePaisa());
            resData.put("totalDepositsPaisa", res.getTotalDepositsPaisa());
            resData.put("creditsPaisa", res.getCreditsPaisa());
            resData.put("debitsPaisa", res.getDebitsPaisa());
            resData.put("openingBalancePaisa", res.getOpeningBalancePaisa());
            resData.put("closingBalancePaisa", res.getClosingBalancePaisa());

            batch.set(docRef, resData);
        }

        batch.commit().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure(task.getException());
                return;
            }

            // STEP 4: WRITE NEXT PERIOD
            callback.onStepChanged(CloseStep.WRITE_NEXT_PERIOD, "Creating next month's period document...");
            executeStepWriteNextPeriod(mess, period, snapshot, activeCloseRequestId, callback);
        });
    }

    private void executeStepWriteNextPeriod(Mess mess, Period period, PeriodSnapshot snapshot, String activeCloseRequestId, CloseProgressCallback callback) {
        String messId = mess.getMessId();
        String periodId = period.getPeriodId();
        String nextPeriodId = PeriodDates.getNextPeriodId(periodId);

        String nextStartDate = PeriodDates.getFirstDay(nextPeriodId);
        String nextEndDate = PeriodDates.getLastDay(nextPeriodId);

        DocumentReference nextPeriodRef = db.collection("messes").document(messId).collection("periods").document(nextPeriodId);

        Map<String, Object> nextPeriodData = new HashMap<>();
        nextPeriodData.put("periodId", nextPeriodId);
        nextPeriodData.put("status", "open");
        nextPeriodData.put("startDate", nextStartDate);
        nextPeriodData.put("endDate", nextEndDate);
        nextPeriodData.put("openingBalances", snapshot.getNextOpeningBalances());
        nextPeriodData.put("createdAt", FieldValue.serverTimestamp());

        nextPeriodRef.set(nextPeriodData).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure(task.getException());
                return;
            }

            // STEP 5: FINALIZE
            callback.onStepChanged(CloseStep.FINALIZE, "Finalizing period close...");
            executeStepFinalize(mess, period, snapshot, nextPeriodId, activeCloseRequestId, callback);
        });
    }

    private void executeStepFinalize(Mess mess, Period period, PeriodSnapshot snapshot, String nextPeriodId, String activeCloseRequestId, CloseProgressCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        String messId = mess.getMessId();
        String periodId = period.getPeriodId();

        DocumentReference oldPeriodRef = db.collection("messes").document(messId).collection("periods").document(periodId);
        DocumentReference messRef = db.collection("messes").document(messId);

        WriteBatch batch = db.batch();

        Map<String, Object> finalizeUpdates = new HashMap<>();
        finalizeUpdates.put("status", "closed");
        finalizeUpdates.put("closedAt", FieldValue.serverTimestamp());
        finalizeUpdates.put("closedBy", currentUid);
        if (snapshot.getFinalRatePaisa() != null) {
            finalizeUpdates.put("finalRatePaisa", snapshot.getFinalRatePaisa());
        }

        Map<String, Object> totalsMap = new HashMap<>();
        totalsMap.put("totalMeals", snapshot.getTotalMeals());
        totalsMap.put("totalVariablePaisa", snapshot.getTotalVariablePaisa());
        totalsMap.put("totalFixedPaisa", snapshot.getTotalFixedPaisa());
        totalsMap.put("totalDepositsPaisa", snapshot.getTotalDepositsPaisa());
        finalizeUpdates.put("totals", totalsMap);

        Map<String, Object> unallocatedMap = new HashMap<>();
        unallocatedMap.put("unallocatedVariablePaisa", snapshot.getUnallocatedVariablePaisa());
        unallocatedMap.put("unallocatedFixedPaisa", snapshot.getUnallocatedFixedPaisa());
        finalizeUpdates.put("unallocated", unallocatedMap);

        finalizeUpdates.put("nextPeriodId", nextPeriodId);
        finalizeUpdates.put("engineVersion", snapshot.getEngineVersion());

        batch.update(oldPeriodRef, finalizeUpdates);
        batch.update(messRef, "currentPeriodId", nextPeriodId);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(nextPeriodId);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}
