package com.example.mealsync.reports;

import java.util.ArrayList;
import java.util.List;

public class ReportData {
    public String messName;
    public String openingDate;
    public String endingDate;
    public long generatedAt;
    public long totalLunches;
    public long totalDinners;
    public long totalGuests;
    public long totalMealCostPaisa;
    public long totalFixedSharePaisa;
    public long totalExpensesPaisa;
    public long totalDepositsPaisa;
    public long totalCreditsPaisa;
    public long totalDebitsPaisa;
    public final List<MemberLine> members = new ArrayList<>();
    public final List<ExpenseLine> expenses = new ArrayList<>();
    public final List<DepositLine> deposits = new ArrayList<>();

    public static class MemberLine {
        public String uid;
        public String name;
        public long lunches;
        public long dinners;
        public long guests;
        public long mealCostPaisa;
        public long fixedSharePaisa;
        public long depositsPaisa;
        public long creditsPaisa;
        public long debitsPaisa;
        public long openingBalancePaisa;
        public long closingBalancePaisa;
    }

    public static class ExpenseLine {
        public String date;
        public String description;
        public String category;
        public long amountPaisa;
        public boolean reversed;
    }

    public static class DepositLine {
        public String date;
        public String memberName;
        public String description;
        public long amountPaisa;
        public String status;
        public boolean reversed;
    }
}
