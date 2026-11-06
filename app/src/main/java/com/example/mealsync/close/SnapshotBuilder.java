package com.example.mealsync.close;

import com.example.mealsync.finance.EngineMember;
import com.example.mealsync.finance.FinanceResult;
import com.example.mealsync.finance.MemberFinanceResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotBuilder {

    private SnapshotBuilder() {
    }

    public static PeriodSnapshot build(String periodId,
                                       FinanceResult financeResult,
                                       List<EngineMember> members,
                                       Map<String, String> memberNamesMap) {
        if (financeResult == null) {
            throw new IllegalArgumentException("FinanceResult cannot be null");
        }

        Long finalRatePaisa = financeResult.isRateAvailable()
                ? Math.round(financeResult.getRatePaisaPerMeal())
                : null;

        long unallocatedVariable = financeResult.isRateAvailable() ? 0L : financeResult.getTotalVariablePaisa();

        // Check if any fixed expense was allocated
        long sumAllocatedFixed = 0;
        long totalDeposits = 0;
        long totalMeals = financeResult.getTotalCountedUnits() / 100;

        Map<String, EngineMember> memberMap = new HashMap<>();
        if (members != null) {
            for (EngineMember m : members) {
                memberMap.put(m.getUid(), m);
            }
        }

        Map<String, MemberResult> memberResults = new LinkedHashMap<>();
        Map<String, Long> nextOpeningBalances = new LinkedHashMap<>();

        for (Map.Entry<String, MemberFinanceResult> entry : financeResult.getMemberResults().entrySet()) {
            String uid = entry.getKey();
            MemberFinanceResult mRes = entry.getValue();

            String name = memberNamesMap != null && memberNamesMap.containsKey(uid)
                    ? memberNamesMap.get(uid)
                    : "Member";

            sumAllocatedFixed += mRes.getFixedSharePaisa();
            totalDeposits += mRes.getTotalDepositsPaisa();

            long closingBalance = mRes.getNetBalancePaisa();

            MemberResult result = new MemberResult(
                    uid,
                    name,
                    mRes.getLunches(),
                    mRes.getDinners(),
                    mRes.getGuests(),
                    mRes.getCountedUnits(),
                    mRes.getMealCostPaisa(),
                    mRes.getFixedSharePaisa(),
                    mRes.getTotalDepositsPaisa(),
                    mRes.getCreditsPaisa(),
                    mRes.getDebitsPaisa(),
                    mRes.getOpeningBalancePaisa(),
                    closingBalance
            );

            memberResults.put(uid, result);

            EngineMember engineMem = memberMap.get(uid);
            boolean active = engineMem == null || engineMem.isActive();

            // Next opening balances: keep active members, and inactive members if non-zero
            if (active || closingBalance != 0) {
                nextOpeningBalances.put(uid, closingBalance);
            }
        }

        long unallocatedFixed = financeResult.getTotalFixedPaisa() - sumAllocatedFixed;

        return new PeriodSnapshot(
                periodId,
                finalRatePaisa,
                totalMeals,
                financeResult.getTotalVariablePaisa(),
                financeResult.getTotalFixedPaisa(),
                totalDeposits,
                unallocatedVariable,
                unallocatedFixed,
                "1.0",
                memberResults,
                nextOpeningBalances
        );
    }
}
