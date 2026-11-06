package com.example.mealsync.close;

public class MemberResult {
    private final String uid;
    private final String name;
    private final int lunches;
    private final int dinners;
    private final int guests;
    private final long countedUnits;
    private final long mealCostPaisa;
    private final long fixedSharePaisa;
    private final long totalDepositsPaisa;
    private final long creditsPaisa;
    private final long debitsPaisa;
    private final long openingBalancePaisa;
    private final long closingBalancePaisa;

    public MemberResult(String uid, String name, int lunches, int dinners, int guests,
                        long countedUnits, long mealCostPaisa, long fixedSharePaisa,
                        long totalDepositsPaisa, long creditsPaisa, long debitsPaisa,
                        long openingBalancePaisa, long closingBalancePaisa) {
        this.uid = uid;
        this.name = name;
        this.lunches = lunches;
        this.dinners = dinners;
        this.guests = guests;
        this.countedUnits = countedUnits;
        this.mealCostPaisa = mealCostPaisa;
        this.fixedSharePaisa = fixedSharePaisa;
        this.totalDepositsPaisa = totalDepositsPaisa;
        this.creditsPaisa = creditsPaisa;
        this.debitsPaisa = debitsPaisa;
        this.openingBalancePaisa = openingBalancePaisa;
        this.closingBalancePaisa = closingBalancePaisa;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
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

    public long getTotalDepositsPaisa() {
        return totalDepositsPaisa;
    }

    public long getCreditsPaisa() {
        return creditsPaisa;
    }

    public long getDebitsPaisa() {
        return debitsPaisa;
    }

    public long getOpeningBalancePaisa() {
        return openingBalancePaisa;
    }

    public long getClosingBalancePaisa() {
        return closingBalancePaisa;
    }
}
