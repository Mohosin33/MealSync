package com.example.mealsync.close;

import com.example.mealsync.finance.EngineMeal;
import com.example.mealsync.finance.EngineMember;
import com.example.mealsync.finance.EngineExpense;
import com.example.mealsync.finance.EngineDeposit;
import com.example.mealsync.finance.FinanceEngine;
import com.example.mealsync.finance.FinanceInput;
import com.example.mealsync.finance.FinanceResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PeriodCloseLogicTest {

    @Test
    public void testPeriodDatesFebruaryLeapAndNonLeap() {
        assertEquals("2024-02-01", PeriodDates.getFirstDay("2024-02"));
        assertEquals("2024-02-29", PeriodDates.getLastDay("2024-02")); // Leap year 2024

        assertEquals("2025-02-01", PeriodDates.getFirstDay("2025-02"));
        assertEquals("2025-02-28", PeriodDates.getLastDay("2025-02")); // Non-leap year 2025
    }

    @Test
    public void testPeriodDatesDecemberRollover() {
        assertEquals("2025-12-01", PeriodDates.getFirstDay("2025-12"));
        assertEquals("2025-12-31", PeriodDates.getLastDay("2025-12"));
        assertEquals("2026-01", PeriodDates.getNextPeriodId("2025-12")); // Rollover to Jan 2026
    }

    @Test
    public void testCanClose() {
        assertFalse(PeriodDates.canClose("2025-05-30", "2025-05-31")); // Before -> False
        assertTrue(PeriodDates.canClose("2025-05-31", "2025-05-31"));  // On -> True
        assertTrue(PeriodDates.canClose("2025-06-01", "2025-05-31"));  // After -> True
    }

    @Test
    public void testConservationRuleAndCarryForward() {
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);

        EngineMeal mealA = new EngineMeal("u1", 2, 1, 0); // 300 units
        EngineMeal mealB = new EngineMeal("u2", 1, 2, 0); // 300 units

        EngineExpense expVar = new EngineExpense("e1", 12000L, "variable"); // 120 Taka
        EngineExpense expFix = new EngineExpense("e2", 6000L, "fixed");    // 60 Taka
        EngineDeposit depA = new EngineDeposit("d1", "u1", 10000L);          // 100 Taka

        Map<String, Long> opening = new HashMap<>();
        opening.put("u1", 1000L);
        opening.put("u2", -500L);

        Map<String, String> names = new HashMap<>();
        names.put("u1", "Alice");
        names.put("u2", "Bob");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Arrays.asList(mealA, mealB),
                Arrays.asList(expVar, expFix),
                Collections.singletonList(depA),
                Collections.emptyList(),
                opening,
                100,
                "EQUAL",
                0L, 0L,
                2000L
        );

        FinanceResult finRes = FinanceEngine.calculate(input);
        PeriodSnapshot snapshot = SnapshotBuilder.build("2025-05", finRes, Arrays.asList(mA, mB), names);

        // Meal charges equal the fixed rate multiplied by each member's meal count.
        long sumMealCost = 0;
        long sumFixedShare = 0;
        long sumOpening = 0;
        long sumClosing = 0;
        long sumDeposits = 0;

        for (MemberResult mRes : snapshot.getMemberResults().values()) {
            sumMealCost += mRes.getMealCostPaisa();
            sumFixedShare += mRes.getFixedSharePaisa();
            sumOpening += mRes.getOpeningBalancePaisa();
            sumClosing += mRes.getClosingBalancePaisa();
            sumDeposits += mRes.getTotalDepositsPaisa();
        }

        assertEquals(12000L, sumMealCost);
        assertEquals(finRes.getTotalFixedPaisa(), sumFixedShare);

        // Closing total equals opening balances and deposits less meal charges and fixed expenses.
        long expectedClosingTotal = sumOpening + sumDeposits - sumMealCost - finRes.getTotalFixedPaisa();
        assertEquals(expectedClosingTotal, sumClosing);

        // Carry-forward equals closing balances
        assertEquals(snapshot.getMemberResults().get("u1").getClosingBalancePaisa(), (long) snapshot.getNextOpeningBalances().get("u1"));
        assertEquals(snapshot.getMemberResults().get("u2").getClosingBalancePaisa(), (long) snapshot.getNextOpeningBalances().get("u2"));
    }

    @Test
    public void testNoMealsAndNoConfiguredRate() {
        EngineMember mA = new EngineMember("u1", true, true);
        EngineExpense expVar = new EngineExpense("e1", 15000L, "variable");

        FinanceInput input = new FinanceInput(
                Collections.singletonList(mA),
                Collections.emptyList(), // No meals
                Collections.singletonList(expVar),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L
        );

        FinanceResult finRes = FinanceEngine.calculate(input);
        PeriodSnapshot snapshot = SnapshotBuilder.build("2025-05", finRes, Collections.singletonList(mA), Collections.emptyMap());

        assertNull(snapshot.getFinalRatePaisa());
        assertEquals(0L, snapshot.getUnallocatedVariablePaisa());
    }

    @Test
    public void testInactiveMemberWithNonZeroBalanceCarriedForward() {
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", false, true); // Inactive member
        EngineMember mC = new EngineMember("u3", false, true); // Inactive with zero balance

        Map<String, Long> opening = new HashMap<>();
        opening.put("u1", 5000L);
        opening.put("u2", -2000L); // Non-zero balance
        opening.put("u3", 0L);     // Zero balance

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB, mC),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                opening,
                100,
                "EQUAL",
                0L, 0L
        );

        FinanceResult finRes = FinanceEngine.calculate(input);
        PeriodSnapshot snapshot = SnapshotBuilder.build("2025-05", finRes, Arrays.asList(mA, mB, mC), Collections.emptyMap());

        assertTrue(snapshot.getNextOpeningBalances().containsKey("u1"));
        assertTrue(snapshot.getNextOpeningBalances().containsKey("u2")); // Kept because non-zero!
        assertFalse(snapshot.getNextOpeningBalances().containsKey("u3")); // Omitted because inactive AND zero!
    }

    @Test
    public void testStateMachineTransitions() {
        CloseAction act1 = CloseStateMachine.nextAction("open", null);
        assertEquals(CloseStep.LOCK, act1.getStep());
        assertFalse(act1.isAlreadyDone());

        CloseAction act2 = CloseStateMachine.nextAction("closing", "req-123");
        assertEquals(CloseStep.CALCULATE, act2.getStep());
        assertEquals("req-123", act2.getCloseRequestId());

        CloseAction act3 = CloseStateMachine.nextAction("closed", "req-123");
        assertEquals(CloseStep.DONE, act3.getStep());
        assertTrue(act3.isAlreadyDone());
    }

    // Fake CloseStore for Failure-Injection Tests
    static class FakeCloseStore {
        String periodStatus = "open";
        String closeRequestId = null;
        PeriodSnapshot snapshot = null;
        boolean resultsWritten = false;
        boolean nextPeriodWritten = false;

        public void stepLock(String reqId) throws Exception {
            if ("closed".equals(periodStatus)) return;
            if ("closing".equals(periodStatus)) {
                return; // Resume
            }
            periodStatus = "closing";
            closeRequestId = reqId;
        }

        public void stepCalculate(PeriodSnapshot snap) throws Exception {
            this.snapshot = snap;
        }

        public void stepWriteResults() throws Exception {
            this.resultsWritten = true;
        }

        public void stepWriteNextPeriod() throws Exception {
            this.nextPeriodWritten = true;
        }

        public void stepFinalize() throws Exception {
            this.periodStatus = "closed";
        }
    }

    @Test
    public void testFailureInjectionAndResumption() {
        String reqId = "test-uuid-123";
        EngineMember mA = new EngineMember("u1", true, true);
        EngineExpense exp = new EngineExpense("e1", 10000L, "variable");
        EngineMeal meal = new EngineMeal("u1", 2, 0, 0);

        FinanceInput input = new FinanceInput(
                Collections.singletonList(mA),
                Collections.singletonList(meal),
                Collections.singletonList(exp),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L
        );
        FinanceResult finRes = FinanceEngine.calculate(input);
        PeriodSnapshot snap = SnapshotBuilder.build("2025-05", finRes, Collections.singletonList(mA), Collections.emptyMap());

        // Simulate failure at each step
        for (int failAtStep = 1; failAtStep <= 5; failAtStep++) {
            FakeCloseStore store = new FakeCloseStore();

            try {
                // Step 1: LOCK
                if (failAtStep == 1) throw new RuntimeException("Simulated lock failure");
                store.stepLock(reqId);

                // Step 2: CALCULATE
                if (failAtStep == 2) throw new RuntimeException("Simulated calculate failure");
                store.stepCalculate(snap);

                // Step 3: WRITE_RESULTS
                if (failAtStep == 3) throw new RuntimeException("Simulated results failure");
                store.stepWriteResults();

                // Step 4: WRITE_NEXT_PERIOD
                if (failAtStep == 4) throw new RuntimeException("Simulated next period failure");
                store.stepWriteNextPeriod();

                // Step 5: FINALIZE
                if (failAtStep == 5) throw new RuntimeException("Simulated finalize failure");
                store.stepFinalize();
            } catch (Exception e) {
                // Retry flow using store state
            }

            // Retry execution from current state
            try {
                store.stepLock(reqId);
                store.stepCalculate(snap);
                store.stepWriteResults();
                store.stepWriteNextPeriod();
                store.stepFinalize();
            } catch (Exception e) {
            }

            assertEquals("closed", store.periodStatus);
            assertEquals(reqId, store.closeRequestId);
            assertTrue(store.resultsWritten);
            assertTrue(store.nextPeriodWritten);
            assertNotNull(store.snapshot);
            assertEquals(snap.getFinalRatePaisa(), store.snapshot.getFinalRatePaisa());
        }
    }
}
