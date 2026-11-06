package com.example.mealsync.close;

import java.util.Map;

public class PeriodSnapshot {
    private final String periodId;
    private final Long finalRatePaisa; // null if unavailable
    private final long totalMeals;
    private final long totalVariablePaisa;
    private final long totalFixedPaisa;
    private final long totalDepositsPaisa;
    private final long unallocatedVariablePaisa;
    private final long unallocatedFixedPaisa;
    private final String engineVersion;
    private final Map<String, MemberResult> memberResults;
    private final Map<String, Long> nextOpeningBalances;

    public PeriodSnapshot(String periodId, Long finalRatePaisa, long totalMeals,
                          long totalVariablePaisa, long totalFixedPaisa, long totalDepositsPaisa,
                          long unallocatedVariablePaisa, long unallocatedFixedPaisa,
                          String engineVersion, Map<String, MemberResult> memberResults,
                          Map<String, Long> nextOpeningBalances) {
        this.periodId = periodId;
        this.finalRatePaisa = finalRatePaisa;
        this.totalMeals = totalMeals;
        this.totalVariablePaisa = totalVariablePaisa;
        this.totalFixedPaisa = totalFixedPaisa;
        this.totalDepositsPaisa = totalDepositsPaisa;
        this.unallocatedVariablePaisa = unallocatedVariablePaisa;
        this.unallocatedFixedPaisa = unallocatedFixedPaisa;
        this.engineVersion = engineVersion;
        this.memberResults = memberResults;
        this.nextOpeningBalances = nextOpeningBalances;
    }

    public String getPeriodId() {
        return periodId;
    }

    public Long getFinalRatePaisa() {
        return finalRatePaisa;
    }

    public long getTotalMeals() {
        return totalMeals;
    }

    public long getTotalVariablePaisa() {
        return totalVariablePaisa;
    }

    public long getTotalFixedPaisa() {
        return totalFixedPaisa;
    }

    public long getTotalDepositsPaisa() {
        return totalDepositsPaisa;
    }

    public long getUnallocatedVariablePaisa() {
        return unallocatedVariablePaisa;
    }

    public long getUnallocatedFixedPaisa() {
        return unallocatedFixedPaisa;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public Map<String, MemberResult> getMemberResults() {
        return memberResults;
    }

    public Map<String, Long> getNextOpeningBalances() {
        return nextOpeningBalances;
    }
}
