package com.example.mealsync.finance;

public class MemberFinanceResult {
    private final String uid;
    private final int lunches;
    private final int dinners;
    private final int guests;
    private final long countedUnits;
    private final long mealCostPaisa;
    private final long fixedSharePaisa;
    private final long openingBalancePaisa;
    private final long totalDepositsPaisa;
    private final long creditsPaisa;
    private final long debitsPaisa;
    private final long netBalancePaisa;

    public MemberFinanceResult(String uid, int lunches, int dinners, int guests,
                               long countedUnits, long mealCostPaisa, long fixedSharePaisa,
                               long openingBalancePaisa, long totalDepositsPaisa,
                               long creditsPaisa, long debitsPaisa, long netBalancePaisa) {
        this.uid = uid;
        this.lunches = lunches;
        this.dinners = dinners;
        this.guests = guests;
        this.countedUnits = countedUnits;
        this.mealCostPaisa = mealCostPaisa;
        this.fixedSharePaisa = fixedSharePaisa;
        this.openingBalancePaisa = openingBalancePaisa;
        this.totalDepositsPaisa = totalDepositsPaisa;
        this.creditsPaisa = creditsPaisa;
        this.debitsPaisa = debitsPaisa;
        this.netBalancePaisa = netBalancePaisa;
    }

    public String getUid() {
        return uid;
    }

    public int getLunches() {
        return lunches;
    }

    public int getDinners() {
        return dinners;
    }

    public int getGuests() {
        return guests;
    }

    public long getCountedUnits() {
        return countedUnits;
    }

    public long getMealCostPaisa() {
        return mealCostPaisa;
    }

    public long getFixedSharePaisa() {
        return fixedSharePaisa;
    }

    public long getOpeningBalancePaisa() {
        return openingBalancePaisa;
    }

    public long getTotalDepositsPaisa() {
        return totalDepositsPaisa;
    }

    public long getCreditsPaisa() {
        return creditsPaisa;
    }

    public long getDebitsPaisa() {
        return debitsPaisa;
    }

    public long getNetBalancePaisa() {
        return netBalancePaisa;
    }
}
