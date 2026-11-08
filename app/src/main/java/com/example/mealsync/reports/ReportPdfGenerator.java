package com.example.mealsync.reports;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.mealsync.util.MoneyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReportPdfGenerator {

    private ReportPdfGenerator() {
    }

    public static File generate(Context context, ReportData report) throws IOException {
        File directory = new File(context.getCacheDir(), "reports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the report directory.");
        }
        String safeMessName = report.messName.replaceAll("[^A-Za-z0-9._-]", "_");
        File file = new File(directory, "MealSync_" + safeMessName + "_"
                + report.openingDate + "_" + report.endingDate + ".pdf");

        PdfDocument document = new PdfDocument();
        try {
            PdfWriter writer = new PdfWriter(document, report);
            writer.section("Report summary");
            writer.paragraph("Generated: " + DateFormat.getDateTimeInstance()
                    .format(new Date(report.generatedAt)));
            writer.paragraph("Date range: " + report.openingDate + " to " + report.endingDate);
            writer.paragraph("Total meals: " + totalMeals(report));
            writer.paragraph("Lunches: " + report.totalLunches
                    + "   Dinners: " + report.totalDinners
                    + "   Guest meals: " + report.totalGuests);
            writer.paragraph("Member meal costs: " + MoneyUtils.formatPaisa(report.totalMealCostPaisa));
            writer.paragraph("Member fixed-expense shares: "
                    + MoneyUtils.formatPaisa(report.totalFixedSharePaisa));
            writer.paragraph("Expenses (excluding reversals): "
                    + MoneyUtils.formatPaisa(report.totalExpensesPaisa));
            writer.paragraph("Approved deposits (excluding reversals): "
                    + MoneyUtils.formatPaisa(report.totalDepositsPaisa));
            writer.paragraph("Member credits: " + MoneyUtils.formatPaisa(report.totalCreditsPaisa));
            writer.paragraph("Member debits: " + MoneyUtils.formatPaisa(report.totalDebitsPaisa));
            long openingBalancePaisa = 0L;
            long closingBalancePaisa = 0L;
            for (ReportData.MemberLine member : report.members) {
                openingBalancePaisa += member.openingBalancePaisa;
                closingBalancePaisa += member.closingBalancePaisa;
            }
            writer.paragraph("Combined opening balance: "
                    + MoneyUtils.formatPaisa(openingBalancePaisa));
            writer.paragraph("Combined closing balance: "
                    + MoneyUtils.formatPaisa(closingBalancePaisa));

            writer.section("Member-wise summary");
            for (ReportData.MemberLine member : report.members) {
                writer.paragraph(member.name + " — Lunches: " + member.lunches
                        + ", dinners: " + member.dinners + ", guests: " + member.guests);
                writer.paragraph("  Meal cost: " + MoneyUtils.formatPaisa(member.mealCostPaisa)
                        + " | Fixed share: " + MoneyUtils.formatPaisa(member.fixedSharePaisa)
                        + " | Deposits: " + MoneyUtils.formatPaisa(member.depositsPaisa));
                writer.paragraph("  Credits: " + MoneyUtils.formatPaisa(member.creditsPaisa)
                        + " | Debits: " + MoneyUtils.formatPaisa(member.debitsPaisa)
                        + " | Opening balance: " + MoneyUtils.formatPaisa(member.openingBalancePaisa)
                        + " | Closing balance: " + MoneyUtils.formatPaisa(member.closingBalancePaisa));
            }

            writer.section("Expenses");
            if (report.expenses.isEmpty()) {
                writer.paragraph("No expenses in the selected date range.");
            } else {
                for (ReportData.ExpenseLine expense : report.expenses) {
                    writer.paragraph(expense.date + " | " + expense.description
                            + (expense.category.isEmpty() ? "" : " (" + expense.category + ")")
                            + " | " + MoneyUtils.formatPaisa(expense.amountPaisa)
                            + (expense.reversed ? " | REVERSED" : ""));
                }
            }

            writer.section("Deposits");
            if (report.deposits.isEmpty()) {
                writer.paragraph("No deposits in the selected date range.");
            } else {
                for (ReportData.DepositLine deposit : report.deposits) {
                    writer.paragraph(deposit.date + " | " + deposit.memberName
                            + " | " + MoneyUtils.formatPaisa(deposit.amountPaisa)
                            + " | " + deposit.status
                            + (deposit.description.isEmpty() ? "" : " | " + deposit.description)
                            + (deposit.reversed ? " | REVERSED" : ""));
                }
            }
            writer.finish();
            try (FileOutputStream output = new FileOutputStream(file)) {
                document.writeTo(output);
            }
        } finally {
            document.close();
        }
        return file;
    }

    private static long totalMeals(ReportData report) {
        return report.totalLunches + report.totalDinners + report.totalGuests;
    }

    private static final class PdfWriter {
        private static final int PAGE_WIDTH = 595;
        private static final int PAGE_HEIGHT = 842;
        private static final float LEFT = 44f;
        private static final float RIGHT = 551f;
        private static final float BOTTOM = 790f;

        private final PdfDocument document;
        private final ReportData report;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private PdfDocument.Page page;
        private Canvas canvas;
        private int pageNumber;
        private float y;

        PdfWriter(PdfDocument document, ReportData report) {
            this.document = document;
            this.report = report;
            newPage();
        }

        void section(String title) {
            ensureSpace(34);
            y += 14;
            paint.setTextSize(15f);
            paint.setFakeBoldText(true);
            canvas.drawText(title, LEFT, y, paint);
            paint.setFakeBoldText(false);
            y += 19;
            paint.setStrokeWidth(0.7f);
            canvas.drawLine(LEFT, y - 5, RIGHT, y - 5, paint);
        }

        void paragraph(String text) {
            paint.setTextSize(10.5f);
            paint.setFakeBoldText(false);
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) > RIGHT - LEFT && line.length() > 0) {
                    drawLine(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (line.length() > 0) drawLine(line.toString());
            y += 4;
        }

        void finish() {
            finishPage();
        }

        private void drawLine(String text) {
            ensureSpace(18);
            canvas.drawText(text, LEFT, y, paint);
            y += 15;
        }

        private void ensureSpace(float height) {
            if (y + height > BOTTOM) newPage();
        }

        private void newPage() {
            if (page != null) finishPage();
            pageNumber++;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            paint.setColor(0xFF17212B);
            paint.setTextSize(18f);
            paint.setFakeBoldText(true);
            canvas.drawText(report.messName, LEFT, 46, paint);
            paint.setTextSize(10f);
            paint.setFakeBoldText(false);
            canvas.drawText("MealSync report | " + report.openingDate + " to "
                    + report.endingDate, LEFT, 65, paint);
            paint.setStrokeWidth(1f);
            canvas.drawLine(LEFT, 76, RIGHT, 76, paint);
            y = 98;
        }

        private void finishPage() {
            paint.setTextSize(9f);
            paint.setColor(0xFF64748B);
            canvas.drawText("Page " + pageNumber, RIGHT - 35, PAGE_HEIGHT - 24, paint);
            document.finishPage(page);
            page = null;
        }
    }
}
