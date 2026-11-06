package com.example.mealsync.finance;

import java.util.List;
import java.util.Map;

public class FinanceInput {
    private final List<EngineMember> members;
    private final List<EngineMeal> meals;
    private final List<EngineExpense> expenses;
    private final List<EngineDeposit> deposits;
    private final List<EngineAdjustment> adjustments;
    private final Map<String, Long> openingBalances;
    private final int guestWeightPercent;
    private final long mealRatePaisa;
    private final String fixedSplitMode; // "EQUAL" or "MEMBER_DAYS"
    private final long periodStartTimestamp;
    private final long periodEndTimestamp;

    public FinanceInput(List<EngineMember> members,
                        List<EngineMeal> meals,
                        List<EngineExpense> expenses,
                        List<EngineDeposit> deposits,
                        List<EngineAdjustment> adjustments,
                        Map<String, Long> openingBalances,
                        int guestWeightPercent,
                        String fixedSplitMode,
                        long periodStartTimestamp,
                        long periodEndTimestamp) {
        this(members, meals, expenses, deposits, adjustments, openingBalances, guestWeightPercent,
                fixedSplitMode, periodStartTimestamp, periodEndTimestamp, 0L);
    }

    public FinanceInput(List<EngineMember> members,
                        List<EngineMeal> meals,
                        List<EngineExpense> expenses,
                        List<EngineDeposit> deposits,
                        List<EngineAdjustment> adjustments,
                        Map<String, Long> openingBalances,
                        int guestWeightPercent,
                        String fixedSplitMode,
                        long periodStartTimestamp,
                        long periodEndTimestamp,
                        long mealRatePaisa) {
        this.members = members;
        this.meals = meals;
        this.expenses = expenses;
        this.deposits = deposits;
        this.adjustments = adjustments;
        this.openingBalances = openingBalances;
        this.guestWeightPercent = guestWeightPercent;
        this.mealRatePaisa = mealRatePaisa;
        this.fixedSplitMode = fixedSplitMode;
        this.periodStartTimestamp = periodStartTimestamp;
        this.periodEndTimestamp = periodEndTimestamp;
    }

    public List<EngineMember> getMembers() {
        return members;
    }

    public List<EngineMeal> getMeals() {
        return meals;
    }

    public List<EngineExpense> getExpenses() {
        return expenses;
    }

    public List<EngineDeposit> getDeposits() {
        return deposits;
    }

    public List<EngineAdjustment> getAdjustments() {
        return adjustments;
    }

    public Map<String, Long> getOpeningBalances() {
        return openingBalances;
    }

    public int getGuestWeightPercent() {
        return guestWeightPercent;
    }

    public long getMealRatePaisa() {
        return mealRatePaisa;
    }

    public String getFixedSplitMode() {
        return fixedSplitMode;
    }

    public long getPeriodStartTimestamp() {
        return periodStartTimestamp;
    }

    public long getPeriodEndTimestamp() {
        return periodEndTimestamp;
    }
}
