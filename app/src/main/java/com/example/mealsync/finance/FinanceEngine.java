package com.example.mealsync.finance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FinanceEngine {

    private FinanceEngine() {
    }

    public static FinanceResult calculate(FinanceInput input) {
        if (input == null) {
            throw new IllegalArgumentException("FinanceInput cannot be null");
        }

        List<EngineMember> members = input.getMembers() != null ? input.getMembers() : Collections.emptyList();
        List<EngineMeal> meals = input.getMeals() != null ? input.getMeals() : Collections.emptyList();
        List<EngineExpense> expenses = input.getExpenses() != null ? input.getExpenses() : Collections.emptyList();
        List<EngineDeposit> deposits = input.getDeposits() != null ? input.getDeposits() : Collections.emptyList();
        List<EngineAdjustment> adjustments = input.getAdjustments() != null ? input.getAdjustments() : Collections.emptyList();
        Map<String, Long> openingBalances = input.getOpeningBalances() != null ? input.getOpeningBalances() : Collections.emptyMap();

        // 1. Reversals
        Set<String> reversedExpenseIds = new HashSet<>();
        Set<String> reversedDepositIds = new HashSet<>();
        for (EngineAdjustment adj : adjustments) {
            if ("reversal".equalsIgnoreCase(adj.getType())) {
                if ("expense".equalsIgnoreCase(adj.getTargetType()) && adj.getTargetId() != null) {
                    reversedExpenseIds.add(adj.getTargetId());
                } else if ("deposit".equalsIgnoreCase(adj.getTargetType()) && adj.getTargetId() != null) {
                    reversedDepositIds.add(adj.getTargetId());
                }
            }
        }

        // 2. Counted Meal Units
        Map<String, Integer> lunchesMap = new HashMap<>();
        Map<String, Integer> dinnersMap = new HashMap<>();
        Map<String, Integer> guestsMap = new HashMap<>();
        for (EngineMeal meal : meals) {
            String uid = meal.getUid();
            lunchesMap.put(uid, lunchesMap.getOrDefault(uid, 0) + meal.getLunches());
            dinnersMap.put(uid, dinnersMap.getOrDefault(uid, 0) + meal.getDinners());
            guestsMap.put(uid, guestsMap.getOrDefault(uid, 0) + meal.getGuests());
        }

        Map<String, Long> countedUnitsMap = new HashMap<>();
        long totalCountedUnits = 0;
        for (EngineMember m : members) {
            String uid = m.getUid();
            int l = lunchesMap.getOrDefault(uid, 0);
            int d = dinnersMap.getOrDefault(uid, 0);
            int g = guestsMap.getOrDefault(uid, 0);
            long units = ((long) l + d + g) * 100L;
            countedUnitsMap.put(uid, units);
            totalCountedUnits += units;
        }

        // 3. Fixed meal rate and expenses
        long totalVariablePaisa = 0;
        long totalFixedPaisa = 0;
        for (EngineExpense e : expenses) {
            if (!reversedExpenseIds.contains(e.getId())
                    && "fixed".equalsIgnoreCase(e.getType())) {
                totalFixedPaisa += e.getAmountPaisa();
            }
        }

        boolean rateAvailable = input.getMealRatePaisa() > 0;
        double ratePaisaPerMeal = rateAvailable ? input.getMealRatePaisa() : -1.0;

        Map<String, Long> mealCostMap = new HashMap<>();
        for (EngineMember m : members) {
            long mealsForMember = countedUnitsMap.getOrDefault(m.getUid(), 0L) / 100L;
            long mealCost = rateAvailable ? mealsForMember * input.getMealRatePaisa() : 0L;
            mealCostMap.put(m.getUid(), mealCost);
        }

        // 4. Fixed Expenses
        Map<String, Long> fixedShareMap = new HashMap<>();
        if (totalFixedPaisa > 0) {
            if ("MEMBER_DAYS".equalsIgnoreCase(input.getFixedSplitMode())) {
                long periodStart = input.getPeriodStartTimestamp();
                long periodEnd = input.getPeriodEndTimestamp();

                Map<String, Long> memberDaysMap = new HashMap<>();
                long totalDays = 0;

                for (EngineMember m : members) {
                    if (m.isActive() && m.isIncludeInFixed()) {
                        long start = m.getJoinedAt() != null ? Math.max(periodStart, m.getJoinedAt()) : periodStart;
                        long end = m.getLeftAt() != null ? Math.min(periodEnd, m.getLeftAt()) : periodEnd;
                        long days = Math.max(1, (end - start) / 86400_000L);
                        memberDaysMap.put(m.getUid(), days);
                        totalDays += days;
                    } else {
                        memberDaysMap.put(m.getUid(), 0L);
                    }
                }

                if (totalDays > 0) {
                    long totalBase = 0;
                    Map<String, Long> baseShare = new HashMap<>();
                    Map<String, Long> remainders = new HashMap<>();

                    for (EngineMember m : members) {
                        String uid = m.getUid();
                        long days = memberDaysMap.getOrDefault(uid, 0L);
                        long base = (totalFixedPaisa * days) / totalDays;
                        long rem = (totalFixedPaisa * days) % totalDays;
                        baseShare.put(uid, base);
                        remainders.put(uid, rem);
                        totalBase += base;
                    }

                    long unallocated = totalFixedPaisa - totalBase;

                    List<EngineMember> sorted = new ArrayList<>(members);
                    Collections.sort(sorted, (m1, m2) -> {
                        long rem1 = remainders.getOrDefault(m1.getUid(), 0L);
                        long rem2 = remainders.getOrDefault(m2.getUid(), 0L);
                        if (rem1 != rem2) {
                            return Long.compare(rem2, rem1);
                        }
                        return m1.getUid().compareTo(m2.getUid());
                    });

                    for (int i = 0; i < sorted.size(); i++) {
                        EngineMember m = sorted.get(i);
                        long share = baseShare.getOrDefault(m.getUid(), 0L);
                        if (i < unallocated && memberDaysMap.getOrDefault(m.getUid(), 0L) > 0) {
                            share += 1;
                        }
                        fixedShareMap.put(m.getUid(), share);
                    }
                }
            } else {
                // EQUAL Mode
                List<EngineMember> eligible = new ArrayList<>();
                for (EngineMember m : members) {
                    if (m.isActive() && m.isIncludeInFixed()) {
                        eligible.add(m);
                    }
                }

                if (!eligible.isEmpty()) {
                    long baseShare = totalFixedPaisa / eligible.size();
                    long remainder = totalFixedPaisa % eligible.size();

                    List<EngineMember> sorted = new ArrayList<>(eligible);
                    Collections.sort(sorted, (m1, m2) -> m1.getUid().compareTo(m2.getUid()));

                    for (int i = 0; i < sorted.size(); i++) {
                        EngineMember m = sorted.get(i);
                        long share = baseShare + (i < remainder ? 1 : 0);
                        fixedShareMap.put(m.getUid(), share);
                    }
                }
            }
        }

        // Fill 0 for members without fixed share
        for (EngineMember m : members) {
            fixedShareMap.putIfAbsent(m.getUid(), 0L);
        }

        // 5. Deposits & Member Adjustments
        Map<String, Long> depositsMap = new HashMap<>();
        for (EngineDeposit d : deposits) {
            if (!reversedDepositIds.contains(d.getId()) && d.getMemberId() != null) {
                depositsMap.put(d.getMemberId(), depositsMap.getOrDefault(d.getMemberId(), 0L) + d.getAmountPaisa());
            }
        }

        Map<String, Long> creditsMap = new HashMap<>();
        Map<String, Long> debitsMap = new HashMap<>();
        for (EngineAdjustment adj : adjustments) {
            if ("member_adjustment".equalsIgnoreCase(adj.getType()) && adj.getMemberId() != null) {
                String uid = adj.getMemberId();
                long amount = adj.getAmountPaisa();
                if (amount > 0) {
                    creditsMap.put(uid, creditsMap.getOrDefault(uid, 0L) + amount);
                } else if (amount < 0) {
                    debitsMap.put(uid, debitsMap.getOrDefault(uid, 0L) + Math.abs(amount));
                }
            }
        }

        // 6. Build Member Results
        Map<String, MemberFinanceResult> memberResults = new LinkedHashMap<>();
        for (EngineMember m : members) {
            String uid = m.getUid();
            int l = lunchesMap.getOrDefault(uid, 0);
            int d = dinnersMap.getOrDefault(uid, 0);
            int g = guestsMap.getOrDefault(uid, 0);
            long units = countedUnitsMap.getOrDefault(uid, 0L);
            long mealCost = mealCostMap.getOrDefault(uid, 0L);
            long fixedShare = fixedShareMap.getOrDefault(uid, 0L);
            long opening = openingBalances.getOrDefault(uid, 0L);
            long dep = depositsMap.getOrDefault(uid, 0L);
            long credits = creditsMap.getOrDefault(uid, 0L);
            long debits = debitsMap.getOrDefault(uid, 0L);

            long netBalance = opening + dep + credits - debits - mealCost - fixedShare;

            MemberFinanceResult mRes = new MemberFinanceResult(
                    uid, l, d, g, units, mealCost, fixedShare, opening, dep, credits, debits, netBalance
            );
            memberResults.put(uid, mRes);
        }

        return new FinanceResult(
                "ESTIMATED",
                totalVariablePaisa,
                totalFixedPaisa,
                totalCountedUnits,
                rateAvailable,
                ratePaisaPerMeal,
                memberResults
        );
    }
}
