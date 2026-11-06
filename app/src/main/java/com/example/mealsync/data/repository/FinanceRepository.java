package com.example.mealsync.data.repository;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinanceRepository {

    private final FirebaseFirestore db;

    public FinanceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public FinanceRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public FinanceResult calculateLive(Mess mess,
                                       Period period,
                                       List<Expense> expenses,
                                       List<Deposit> deposits,
                                       List<Adjustment> adjustments,
                                       List<Member> members,
                                       List<Meal> meals) {
        List<EngineMember> engineMembers = new ArrayList<>();
        if (members != null) {
            for (Member m : members) {
                engineMembers.add(new EngineMember(m.getUid(), true, m.isIncludeInFixed(), m.getJoinedAt(), null));
            }
        }

        Map<String, int[]> memberMealCounts = new HashMap<>(); // uid -> [lunches, dinners, guests]
        if (meals != null) {
            for (Meal meal : meals) {
                if (meal != null && meal.getUid() != null) {
                    String uid = meal.getUid();
                    int[] counts = memberMealCounts.computeIfAbsent(uid, k -> new int[]{0, 0, 0});
                    if (meal.isLunch()) counts[0]++;
                    if (meal.isDinner()) counts[1]++;
                    counts[2] += meal.getGuests();
                }
            }
        }

        List<EngineMeal> engineMeals = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : memberMealCounts.entrySet()) {
            int[] c = entry.getValue();
            engineMeals.add(new EngineMeal(entry.getKey(), c[0], c[1], c[2]));
        }

        List<EngineExpense> engineExpenses = new ArrayList<>();
        if (expenses != null) {
            for (Expense e : expenses) {
                engineExpenses.add(new EngineExpense(e.getId(), e.getAmountPaisa(), e.getType()));
            }
        }

        List<EngineDeposit> engineDeposits = new ArrayList<>();
        if (deposits != null) {
            for (Deposit d : deposits) {
                if ("approved".equalsIgnoreCase(d.getStatus())) {
                    engineDeposits.add(new EngineDeposit(d.getId(), d.getMemberId(), d.getAmountPaisa()));
                }
            }
        }

        List<EngineAdjustment> engineAdjustments = new ArrayList<>();
        if (adjustments != null) {
            for (Adjustment a : adjustments) {
                if ("reversal".equalsIgnoreCase(a.getType())) {
                    engineAdjustments.add(EngineAdjustment.createReversal(a.getId(), a.getTargetType(), a.getTargetId()));
                } else if ("member_adjustment".equalsIgnoreCase(a.getType())) {
                    engineAdjustments.add(EngineAdjustment.createMemberAdjustment(a.getId(), a.getMemberId(), a.getAmountPaisa()));
                }
            }
        }

        Map<String, Long> openingBalances = (period != null && period.getOpeningBalances() != null)
                ? period.getOpeningBalances() : new HashMap<>();

        int guestWeight = mess != null ? mess.getGuestWeightPercent() : 100;
        FinanceInput input = new FinanceInput(
                engineMembers,
                engineMeals,
                engineExpenses,
                engineDeposits,
                engineAdjustments,
                openingBalances,
                guestWeight,
                "EQUAL",
                0L, 0L,
                mess != null ? mess.getMealRatePaisa() : 0L
        );

        return FinanceEngine.calculate(input);
    }
}
