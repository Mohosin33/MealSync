package com.example.mealsync.reports;

import com.example.mealsync.data.models.Adjustment;
import com.example.mealsync.data.models.Deposit;
import com.example.mealsync.data.models.Expense;
import com.example.mealsync.data.models.Meal;
import com.example.mealsync.data.models.Member;
import com.example.mealsync.data.models.Mess;
import com.example.mealsync.data.models.Period;
import com.example.mealsync.data.repository.FinanceRepository;
import com.example.mealsync.finance.FinanceResult;
import com.example.mealsync.finance.MemberFinanceResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ReportRepository {

    public interface Callback {
        void onSuccess(ReportData report);
        void onFailure(Exception error);
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final FirebaseFirestore db;
    private final FinanceRepository financeRepository;

    public ReportRepository() {
        this(FirebaseFirestore.getInstance(), new FinanceRepository());
    }

    ReportRepository(FirebaseFirestore db, FinanceRepository financeRepository) {
        this.db = db;
        this.financeRepository = financeRepository;
    }

    public void generate(Mess mess, String openingDate, String endingDate, Callback callback) {
        if (mess == null || mess.getMessId() == null || openingDate == null || endingDate == null) {
            callback.onFailure(new IllegalArgumentException("Report details are incomplete."));
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                ReportData report = buildReport(mess, LocalDate.parse(openingDate, DATE_FORMAT),
                        LocalDate.parse(endingDate, DATE_FORMAT));
                callback.onSuccess(report);
            } catch (Exception error) {
                callback.onFailure(error);
            }
        }, "mealsync-report");
        worker.start();
    }

    private ReportData buildReport(Mess mess, LocalDate start, LocalDate end)
            throws ExecutionException, InterruptedException {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Ending date cannot be earlier than opening date.");
        }

        ZoneId zoneId = zoneIdFor(mess);
        String messId = mess.getMessId();
        var messRef = db.collection("messes").document(messId);
        QuerySnapshot currentMemberSnapshot = Tasks.await(messRef.collection("members").get());
        Map<String, Member> membersByUid = new HashMap<>();
        Map<String, String> memberNames = new HashMap<>();
        for (DocumentSnapshot document : currentMemberSnapshot.getDocuments()) {
            Member member = document.toObject(Member.class);
            if (member != null && member.getUid() != null) {
                membersByUid.put(member.getUid(), member);
                memberNames.put(member.getUid(), safeName(member.getName()));
            }
        }
        Map<String, Member> currentMembersByUid = new HashMap<>(membersByUid);

        ReportData report = new ReportData();
        report.messName = mess.getName() != null ? mess.getName() : "MealSync";
        report.openingDate = start.format(DATE_FORMAT);
        report.endingDate = end.format(DATE_FORMAT);
        report.generatedAt = System.currentTimeMillis();
        Map<String, ReportData.MemberLine> memberLines = new HashMap<>();

        YearMonth month = YearMonth.from(start);
        YearMonth lastMonth = YearMonth.from(end);
        while (!month.isAfter(lastMonth)) {
            String periodId = month.toString();
            Period period = getPeriod(messRef.collection("periods").document(periodId).get());
            if (period == null) {
                period = new Period(periodId, "open", month.atDay(1).format(DATE_FORMAT),
                        month.atEndOfMonth().format(DATE_FORMAT), Collections.emptyMap());
            }
            if (period.getStartDate() == null) {
                period.setStartDate(month.atDay(1).format(DATE_FORMAT));
            }
            if (period.getEndDate() == null) {
                period.setEndDate(month.atEndOfMonth().format(DATE_FORMAT));
            }
            if (period.getOpeningBalances() == null) {
                period.setOpeningBalances(Collections.emptyMap());
            }

            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();
            List<Task<QuerySnapshot>> tasks = new ArrayList<>();
            tasks.add(messRef.collection("meals")
                    .whereGreaterThanOrEqualTo("date", monthStart.format(DATE_FORMAT))
                    .whereLessThanOrEqualTo("date", monthEnd.format(DATE_FORMAT)).get());
            tasks.add(messRef.collection("expenses").whereEqualTo("periodId", periodId).get());
            tasks.add(messRef.collection("deposits").whereEqualTo("periodId", periodId).get());
            tasks.add(messRef.collection("adjustments").whereEqualTo("periodId", periodId).get());
            Task<List<QuerySnapshot>> allTasks = Tasks.whenAllSuccess(tasks);
            List<QuerySnapshot> snapshots = Tasks.await(allTasks);
            List<Meal> allMeals = toModels(snapshots.get(0), Meal.class);
            List<Expense> allExpenses = toModels(snapshots.get(1), Expense.class);
            List<Deposit> allDeposits = toModels(snapshots.get(2), Deposit.class);
            List<Adjustment> allAdjustments = toModels(snapshots.get(3), Adjustment.class);

            QuerySnapshot oldResults = Tasks.await(messRef.collection("periods")
                    .document(periodId).collection("results").get());
            boolean useArchivedMembers = "closed".equalsIgnoreCase(period.getStatus())
                    && !oldResults.isEmpty();
            Map<String, Member> periodMembers = useArchivedMembers
                    ? new HashMap<>() : new HashMap<>(currentMembersByUid);
            for (DocumentSnapshot result : oldResults.getDocuments()) {
                String uid = result.getString("uid");
                if (uid == null) uid = result.getId();
                String name = result.getString("name");
                memberNames.putIfAbsent(uid, safeName(name));
                Member archivedMember = new Member(uid, safeName(name), "", "member", 0L);
                membersByUid.putIfAbsent(uid, archivedMember);
                if (useArchivedMembers) {
                    periodMembers.put(uid, archivedMember);
                }
            }
            if (!useArchivedMembers) {
                for (String uid : period.getOpeningBalances().keySet()) {
                    Member member = new Member(uid, memberNames.getOrDefault(uid, "Member"),
                            "", "member", 0L);
                    membersByUid.putIfAbsent(uid, member);
                    periodMembers.putIfAbsent(uid, member);
                }
            }

            LocalDate rangeStart = start.isAfter(monthStart) ? start : monthStart;
            LocalDate rangeEnd = end.isBefore(monthEnd) ? end : monthEnd;
            List<Meal> rangeMeals = filterMeals(allMeals, rangeStart, rangeEnd);
            List<Expense> rangeExpenses = filterExpenses(allExpenses, rangeStart, rangeEnd);
            List<Deposit> rangeDeposits = filterDeposits(allDeposits, rangeStart, rangeEnd, zoneId);
            Set<String> reversedExpenses = reversedIds(allAdjustments, "expense", rangeEnd, zoneId);
            Set<String> reversedDeposits = reversedIds(allAdjustments, "deposit", rangeEnd, zoneId);

            for (Meal meal : rangeMeals) {
                ReportData.MemberLine line = lineFor(memberLines, memberNames, meal.getUid());
                if (line != null) {
                    if (meal.isLunch()) line.lunches++;
                    if (meal.isDinner()) line.dinners++;
                    line.guests += meal.getGuests();
                }
                if (meal.isLunch()) report.totalLunches++;
                if (meal.isDinner()) report.totalDinners++;
                report.totalGuests += meal.getGuests();
            }
            for (Expense expense : rangeExpenses) {
                ReportData.ExpenseLine line = new ReportData.ExpenseLine();
                line.date = expense.getExpenseDate();
                line.description = safeText(expense.getDescription(), "Expense");
                line.category = safeText(expense.getCategory(), "");
                line.amountPaisa = expense.getAmountPaisa();
                line.reversed = reversedExpenses.contains(expense.getId());
                report.expenses.add(line);
                if (!line.reversed) report.totalExpensesPaisa += line.amountPaisa;
            }
            for (Deposit deposit : rangeDeposits) {
                ReportData.DepositLine line = new ReportData.DepositLine();
                line.date = localDate(deposit.getCreatedAt(), zoneId);
                line.memberName = memberNames.getOrDefault(deposit.getMemberId(), "Member");
                line.description = safeText(deposit.getDescription(), "");
                line.amountPaisa = deposit.getAmountPaisa();
                line.status = safeText(deposit.getStatus(), "approved");
                line.reversed = reversedDeposits.contains(deposit.getId());
                report.deposits.add(line);
                if (!line.reversed && "approved".equalsIgnoreCase(line.status)) {
                    report.totalDepositsPaisa += line.amountPaisa;
                }
            }

            List<Member> engineMembers = new ArrayList<>(periodMembers.values());
            Period calculationPeriod = new Period(periodId, period.getStatus(), period.getStartDate(),
                    period.getEndDate(), period.getOpeningBalances());
            Mess calculationMess = copyMess(mess, period.getFinalRatePaisa());
            LocalDate beforeEnd = rangeStart.minusDays(1);
            FinanceResult before = calculateThrough(calculationMess, calculationPeriod, engineMembers,
                    allMeals, allExpenses, allDeposits, allAdjustments, beforeEnd, zoneId);
            FinanceResult through = calculateThrough(calculationMess, calculationPeriod, engineMembers,
                    allMeals, allExpenses, allDeposits, allAdjustments, rangeEnd, zoneId);
            accumulateFinance(report, memberLines, memberNames, before, through);

            if (month.equals(YearMonth.from(start))) {
                setBalances(memberLines, memberNames, before, true);
            }
            if (month.equals(lastMonth)) {
                setBalances(memberLines, memberNames, through, false);
            }
            month = month.plusMonths(1);
        }

        report.members.addAll(memberLines.values());
        Collections.sort(report.members, (left, right) -> left.name.compareToIgnoreCase(right.name));
        Collections.sort(report.expenses, (left, right) -> safeText(right.date, "")
                .compareTo(safeText(left.date, "")));
        Collections.sort(report.deposits, (left, right) -> safeText(right.date, "")
                .compareTo(safeText(left.date, "")));
        return report;
    }

    private Period getPeriod(Task<DocumentSnapshot> task)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = Tasks.await(task);
        return snapshot != null && snapshot.exists() ? snapshot.toObject(Period.class) : null;
    }

    private FinanceResult calculateThrough(Mess mess, Period period, List<Member> members,
                                           List<Meal> meals, List<Expense> expenses,
                                           List<Deposit> deposits, List<Adjustment> adjustments,
                                           LocalDate through, ZoneId zoneId) {
        if (through.isBefore(LocalDate.parse(period.getStartDate(), DATE_FORMAT))) {
            return financeRepository.calculateLive(mess, period, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), members, Collections.emptyList());
        }
        List<Meal> filteredMeals = new ArrayList<>();
        for (Meal meal : meals) {
            if (meal.getDate() != null && !LocalDate.parse(meal.getDate(), DATE_FORMAT).isAfter(through)) {
                filteredMeals.add(meal);
            }
        }
        List<Expense> filteredExpenses = new ArrayList<>();
        for (Expense expense : expenses) {
            if (expense.getExpenseDate() != null
                    && !LocalDate.parse(expense.getExpenseDate(), DATE_FORMAT).isAfter(through)) {
                filteredExpenses.add(expense);
            }
        }
        List<Deposit> filteredDeposits = new ArrayList<>();
        for (Deposit deposit : deposits) {
            LocalDate date = dateFor(deposit.getCreatedAt(), zoneId);
            if (date != null && !date.isAfter(through)) filteredDeposits.add(deposit);
        }
        List<Adjustment> filteredAdjustments = new ArrayList<>();
        for (Adjustment adjustment : adjustments) {
            LocalDate date = dateFor(adjustment.getCreatedAt(), zoneId);
            if (date != null && !date.isAfter(through)) filteredAdjustments.add(adjustment);
        }
        return financeRepository.calculateLive(mess, period, filteredExpenses, filteredDeposits,
                filteredAdjustments, members, filteredMeals);
    }

    private void accumulateFinance(ReportData report,
                                   Map<String, ReportData.MemberLine> lines,
                                   Map<String, String> names,
                                   FinanceResult before, FinanceResult through) {
        for (Map.Entry<String, MemberFinanceResult> entry : through.getMemberResults().entrySet()) {
            String uid = entry.getKey();
            MemberFinanceResult now = entry.getValue();
            MemberFinanceResult prior = before.getMemberResults().get(uid);
            ReportData.MemberLine line = lineFor(lines, names, uid);
            if (line == null) continue;
            line.mealCostPaisa += difference(now.getMealCostPaisa(), prior, MemberFinanceResult::getMealCostPaisa);
            line.fixedSharePaisa += difference(now.getFixedSharePaisa(), prior, MemberFinanceResult::getFixedSharePaisa);
            line.depositsPaisa += difference(now.getTotalDepositsPaisa(), prior, MemberFinanceResult::getTotalDepositsPaisa);
            line.creditsPaisa += difference(now.getCreditsPaisa(), prior, MemberFinanceResult::getCreditsPaisa);
            line.debitsPaisa += difference(now.getDebitsPaisa(), prior, MemberFinanceResult::getDebitsPaisa);
            report.totalMealCostPaisa += difference(now.getMealCostPaisa(), prior, MemberFinanceResult::getMealCostPaisa);
            report.totalFixedSharePaisa += difference(now.getFixedSharePaisa(), prior,
                    MemberFinanceResult::getFixedSharePaisa);
            report.totalCreditsPaisa += difference(now.getCreditsPaisa(), prior, MemberFinanceResult::getCreditsPaisa);
            report.totalDebitsPaisa += difference(now.getDebitsPaisa(), prior, MemberFinanceResult::getDebitsPaisa);
        }
    }

    private interface Amount {
        long get(MemberFinanceResult result);
    }

    private long difference(long value, MemberFinanceResult previous, Amount amount) {
        return value - (previous != null ? amount.get(previous) : 0L);
    }

    private void setBalances(Map<String, ReportData.MemberLine> lines, Map<String, String> names,
                             FinanceResult result, boolean opening) {
        for (Map.Entry<String, MemberFinanceResult> entry : result.getMemberResults().entrySet()) {
            ReportData.MemberLine line = lineFor(lines, names, entry.getKey());
            if (line != null) {
                long balance = entry.getValue().getNetBalancePaisa();
                if (opening) line.openingBalancePaisa = balance;
                else line.closingBalancePaisa = balance;
            }
        }
    }

    private List<Meal> filterMeals(List<Meal> items, LocalDate start, LocalDate end) {
        List<Meal> result = new ArrayList<>();
        for (Meal item : items) {
            if (item.getDate() != null) {
                LocalDate date = LocalDate.parse(item.getDate(), DATE_FORMAT);
                if (!date.isBefore(start) && !date.isAfter(end)) result.add(item);
            }
        }
        return result;
    }

    private List<Expense> filterExpenses(List<Expense> items, LocalDate start, LocalDate end) {
        List<Expense> result = new ArrayList<>();
        for (Expense item : items) {
            if (item.getExpenseDate() != null) {
                LocalDate date = LocalDate.parse(item.getExpenseDate(), DATE_FORMAT);
                if (!date.isBefore(start) && !date.isAfter(end)) result.add(item);
            }
        }
        return result;
    }

    private List<Deposit> filterDeposits(List<Deposit> items, LocalDate start, LocalDate end,
                                         ZoneId zoneId) {
        List<Deposit> result = new ArrayList<>();
        for (Deposit item : items) {
            LocalDate date = dateFor(item.getCreatedAt(), zoneId);
            if (date != null && !date.isBefore(start) && !date.isAfter(end)) result.add(item);
        }
        return result;
    }

    private Set<String> reversedIds(List<Adjustment> adjustments, String targetType,
                                    LocalDate through, ZoneId zoneId) {
        Set<String> ids = new HashSet<>();
        for (Adjustment adjustment : adjustments) {
            LocalDate date = dateFor(adjustment.getCreatedAt(), zoneId);
            if ("reversal".equalsIgnoreCase(adjustment.getType())
                    && targetType.equalsIgnoreCase(adjustment.getTargetType())
                    && adjustment.getTargetId() != null && date != null && !date.isAfter(through)) {
                ids.add(adjustment.getTargetId());
            }
        }
        return ids;
    }

    private <T> List<T> toModels(QuerySnapshot snapshot, Class<T> modelClass) {
        List<T> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            T model = document.toObject(modelClass);
            if (model != null) result.add(model);
        }
        return result;
    }

    private ReportData.MemberLine lineFor(Map<String, ReportData.MemberLine> lines,
                                          Map<String, String> names, String uid) {
        if (uid == null) return null;
        return lines.computeIfAbsent(uid, key -> {
            ReportData.MemberLine line = new ReportData.MemberLine();
            line.uid = key;
            line.name = names.getOrDefault(key, "Member");
            return line;
        });
    }

    private String localDate(Date date, ZoneId zoneId) {
        LocalDate localDate = dateFor(date, zoneId);
        return localDate != null ? localDate.format(DATE_FORMAT) : "";
    }

    private LocalDate dateFor(Date date, ZoneId zoneId) {
        return date != null ? date.toInstant().atZone(zoneId).toLocalDate() : null;
    }

    private ZoneId zoneIdFor(Mess mess) {
        try {
            return ZoneId.of(mess.getTimezone());
        } catch (Exception error) {
            return ZoneId.of("Asia/Dhaka");
        }
    }

    private Mess copyMess(Mess source, Long periodRate) {
        Mess copy = new Mess();
        copy.setMessId(source.getMessId());
        copy.setName(source.getName());
        copy.setTimezone(source.getTimezone());
        copy.setMealRatePaisa(periodRate != null ? periodRate : source.getMealRatePaisa());
        copy.setGuestWeightPercent(source.getGuestWeightPercent());
        copy.setFixedSplitMode(source.getFixedSplitMode());
        return copy;
    }

    private String safeName(String name) {
        return name != null && !name.trim().isEmpty() ? name.trim() : "Member";
    }

    private String safeText(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
