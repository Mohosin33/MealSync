package com.example.mealsync.finance;

import java.util.Map;

public class FinanceResult {
    private final String label; // "ESTIMATED"
    private final long totalVariablePaisa;
    private final long totalFixedPaisa;
    private final long totalCountedUnits;
    private final boolean rateAvailable;
    private final double ratePaisaPerMeal;
    private final Map<String, MemberFinanceResult> memberResults;

    public FinanceResult(String label, long totalVariablePaisa, long totalFixedPaisa,
                         long totalCountedUnits, boolean rateAvailable, double ratePaisaPerMeal,
                         Map<String, MemberFinanceResult> memberResults) {
        this.label = label;
        this.totalVariablePaisa = totalVariablePaisa;
        this.totalFixedPaisa = totalFixedPaisa;
        this.totalCountedUnits = totalCountedUnits;
        this.rateAvailable = rateAvailable;
        this.ratePaisaPerMeal = ratePaisaPerMeal;
        this.memberResults = memberResults;
    }

    public String getLabel() {
        return label;
    }

    public long getTotalVariablePaisa() {
        return totalVariablePaisa;
    }

    public long getTotalFixedPaisa() {
        return totalFixedPaisa;
    }

    public long getTotalCountedUnits() {
        return totalCountedUnits;
    }

    public boolean isRateAvailable() {
        return rateAvailable;
    }

    public double getRatePaisaPerMeal() {
        return ratePaisaPerMeal;
    }

    public Map<String, MemberFinanceResult> getMemberResults() {
        return memberResults;
    }
}
