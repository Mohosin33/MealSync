package com.example.mealsync.finance;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FinanceEngineTest {

    @Test
    public void testCaseA() {
        // A has 2 meals and B has 4 meals at 50 Taka per meal.
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);

        EngineMeal mealA = new EngineMeal("u1", 2, 0, 0);
        EngineMeal mealB = new EngineMeal("u2", 4, 0, 0);

        EngineExpense expVar = new EngineExpense("e1", 30000L, "variable");
        EngineExpense expFix = new EngineExpense("e2", 60000L, "fixed");
        EngineDeposit depA = new EngineDeposit("d1", "u1", 10000L);

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Arrays.asList(mealA, mealB),
                Arrays.asList(expVar, expFix),
                Collections.singletonList(depA),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertTrue(result.isRateAvailable());
        assertEquals(5000.0, result.getRatePaisaPerMeal(), 0.001);

        MemberFinanceResult resA = result.getMemberResults().get("u1");
        MemberFinanceResult resB = result.getMemberResults().get("u2");

        assertEquals(10000L, resA.getMealCostPaisa());
        assertEquals(30000L, resA.getFixedSharePaisa());
        assertEquals(-30000L, resA.getNetBalancePaisa());

        assertEquals(20000L, resB.getMealCostPaisa());
        assertEquals(30000L, resB.getFixedSharePaisa());
        assertEquals(-50000L, resB.getNetBalancePaisa());
    }

    @Test
    public void configuredMealRateDoesNotDependOnExpenses() {
        EngineMember member = new EngineMember("u1", true, true);
        EngineMeal meals = new EngineMeal("u1", 2, 0, 0);
        EngineExpense variable = new EngineExpense("variable", 10000L, "variable");
        EngineExpense fixed = new EngineExpense("fixed", 90000L, "fixed");

        FinanceResult beforeUpdate = FinanceEngine.calculate(new FinanceInput(
                Collections.singletonList(member),
                Collections.singletonList(meals),
                Arrays.asList(variable, fixed),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        ));

        FinanceResult afterUpdate = FinanceEngine.calculate(new FinanceInput(
                Collections.singletonList(member),
                Collections.singletonList(meals),
                Arrays.asList(new EngineExpense("variable", 20000L, "variable"), fixed),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        ));

        assertEquals(5000.0, beforeUpdate.getRatePaisaPerMeal(), 0.001);
        assertEquals(5000.0, afterUpdate.getRatePaisaPerMeal(), 0.001);
        assertEquals(10000L, beforeUpdate.getMemberResults().get("u1").getMealCostPaisa());
        assertEquals(10000L, afterUpdate.getMemberResults().get("u1").getMealCostPaisa());
    }

    @Test
    public void testCaseB() {
        // Each member meal and guest meal is charged at the fixed 100 Taka rate.
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);

        EngineMeal mealA = new EngineMeal("u1", 2, 0, 1); // 2 lunches + 1 guest
        EngineMeal mealB = new EngineMeal("u2", 4, 0, 0);

        EngineExpense expVar = new EngineExpense("e1", 30000L, "variable");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Arrays.asList(mealA, mealB),
                Collections.singletonList(expVar),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                50,
                "EQUAL",
                0L, 0L,
                10000L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        MemberFinanceResult resA = result.getMemberResults().get("u1");
        MemberFinanceResult resB = result.getMemberResults().get("u2");

        assertEquals(30000L, resA.getMealCostPaisa());
        assertEquals(40000L, resB.getMealCostPaisa());
        assertEquals(70000L, resA.getMealCostPaisa() + resB.getMealCostPaisa());
    }

    @Test
    public void testConfiguredRateIsAvailableWithoutMeals() {
        // The fixed rate remains available even when no meals have been logged.
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);
        EngineExpense expVar = new EngineExpense("e1", 10000L, "variable");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Collections.emptyList(),
                Collections.singletonList(expVar),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertTrue(result.isRateAvailable());
        assertEquals(0L, result.getMemberResults().get("u1").getMealCostPaisa());
        assertEquals(0L, result.getMemberResults().get("u2").getMealCostPaisa());
    }

    @Test
    public void testCaseD() {
        // Reversing legacy variable expenses does not affect the configured rate.
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);

        EngineMeal mealA = new EngineMeal("u1", 2, 0, 0);
        EngineMeal mealB = new EngineMeal("u2", 4, 0, 0);

        EngineExpense expVar = new EngineExpense("e1", 30000L, "variable");
        EngineAdjustment rev1 = EngineAdjustment.createReversal("r1", "expense", "e1");
        EngineAdjustment rev2 = EngineAdjustment.createReversal("r2", "expense", "e1");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Arrays.asList(mealA, mealB),
                Collections.singletonList(expVar),
                Collections.emptyList(),
                Arrays.asList(rev1, rev2),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertEquals(0L, result.getTotalVariablePaisa());
        assertEquals(10000L, result.getMemberResults().get("u1").getMealCostPaisa());
        assertEquals(20000L, result.getMemberResults().get("u2").getMealCostPaisa());
    }

    @Test
    public void testCaseE() {
        // Member B with includeInFixed = false
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, false);

        EngineExpense expFix = new EngineExpense("e1", 60000L, "fixed");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Collections.emptyList(),
                Collections.singletonList(expFix),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L,
                5000L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertEquals(60000L, result.getMemberResults().get("u1").getFixedSharePaisa());
        assertEquals(0L, result.getMemberResults().get("u2").getFixedSharePaisa());
    }

    @Test
    public void testCaseF() {
        // 10000 paisa fixed expense split 3 ways
        EngineMember m1 = new EngineMember("u1", true, true);
        EngineMember m2 = new EngineMember("u2", true, true);
        EngineMember m3 = new EngineMember("u3", true, true);

        EngineExpense expFix = new EngineExpense("e1", 10000L, "fixed");

        FinanceInput input = new FinanceInput(
                Arrays.asList(m1, m2, m3),
                Collections.emptyList(),
                Collections.singletonList(expFix),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        long f1 = result.getMemberResults().get("u1").getFixedSharePaisa();
        long f2 = result.getMemberResults().get("u2").getFixedSharePaisa();
        long f3 = result.getMemberResults().get("u3").getFixedSharePaisa();

        assertEquals(10000L, f1 + f2 + f3);
        assertEquals(3334L, f1);
        assertEquals(3333L, f2);
        assertEquals(3333L, f3);
    }

    @Test
    public void testCaseG() {
        // Opening balance of -5000 paisa
        EngineMember m1 = new EngineMember("u1", true, true);
        Map<String, Long> opening = new HashMap<>();
        opening.put("u1", -5000L);

        FinanceInput input = new FinanceInput(
                Collections.singletonList(m1),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                opening,
                100,
                "EQUAL",
                0L, 0L
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertEquals(-5000L, result.getMemberResults().get("u1").getOpeningBalancePaisa());
        assertEquals(-5000L, result.getMemberResults().get("u1").getNetBalancePaisa());
    }

    @Test
    public void testCaseH() {
        // Determinism: Same input run twice
        EngineMember mA = new EngineMember("u1", true, true);
        EngineMember mB = new EngineMember("u2", true, true);

        EngineMeal mealA = new EngineMeal("u1", 2, 1, 0);
        EngineMeal mealB = new EngineMeal("u2", 1, 2, 1);

        EngineExpense expVar = new EngineExpense("e1", 12345L, "variable");

        FinanceInput input = new FinanceInput(
                Arrays.asList(mA, mB),
                Arrays.asList(mealA, mealB),
                Collections.singletonList(expVar),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "EQUAL",
                0L, 0L
        );

        FinanceResult res1 = FinanceEngine.calculate(input);
        FinanceResult res2 = FinanceEngine.calculate(input);

        assertEquals(res1.getRatePaisaPerMeal(), res2.getRatePaisaPerMeal(), 0.0001);
        assertEquals(res1.getMemberResults().get("u1").getNetBalancePaisa(), res2.getMemberResults().get("u1").getNetBalancePaisa());
        assertEquals(res1.getMemberResults().get("u2").getNetBalancePaisa(), res2.getMemberResults().get("u2").getNetBalancePaisa());
    }

    @Test
    public void testMemberDaysMode() {
        long start = 1000000L;
        long end = start + (10L * 86400000L);

        EngineMember m1 = new EngineMember("u1", true, true, start, end);
        EngineMember m2 = new EngineMember("u2", true, true, start + (5L * 86400000L), end);

        EngineExpense exp = new EngineExpense("e1", 15000L, "fixed");

        FinanceInput input = new FinanceInput(
                Arrays.asList(m1, m2),
                Collections.emptyList(),
                Collections.singletonList(exp),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                100,
                "MEMBER_DAYS",
                start, end
        );

        FinanceResult result = FinanceEngine.calculate(input);

        assertEquals(10000L, result.getMemberResults().get("u1").getFixedSharePaisa());
        assertEquals(5000L, result.getMemberResults().get("u2").getFixedSharePaisa());
    }
}
