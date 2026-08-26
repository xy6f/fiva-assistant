package com.xy6f.fiva_assistant.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class PdfExportUtil {

    // 支持中文的字体路径
    private static final String FONT_PATH = "STSong-Light";
    private static final String ENCODING = "UniGB-UCS2-H";

    public static byte[] exportReport(Map<String, Object> report) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // 标题字体
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 51, 102));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(255, 255, 255));
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        // 标题
        Paragraph title = new Paragraph("财务报表", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 报告期间
        String period = String.valueOf(report.get("period"));
        Paragraph periodPara = new Paragraph("报告期间: " + ("ALL".equals(period) ? "全部" : period), normalFont);
        periodPara.setSpacingAfter(15);
        document.add(periodPara);

        // 利润表
        Map<String, Object> profit = (Map<String, Object>) report.get("profitStatement");
        addSection(document, "利润表", headerFont, normalFont, 
            new String[]{"项目", "金额"}, 
            new Object[][]{
                {"营业收入", profit.get("totalIncome")},
                {"减：营业费用", profit.get("totalExpense")},
                {"毛利润", profit.get("grossProfit")}
            });

        // 税费汇总
        Map<String, Object> tax = (Map<String, Object>) report.get("taxSummary");
        addSection(document, "税费汇总", headerFont, normalFont,
            new String[]{"税费项目", "金额"},
            new Object[][]{
                {"增值税(13%)", tax.get("estimatedVat")},
                {"附加税(12%)", tax.get("surcharge")},
                {"企业所得税(5%)", tax.get("incomeTax")},
                {"税费合计", tax.get("totalTaxBurden")}
            });

        // 月度趋势
        List<Map<String, Object>> trend = (List<Map<String, Object>>) report.get("monthlyTrend");
        if (trend != null && !trend.isEmpty()) {
            addTrendSection(document, "月度趋势", headerFont, normalFont, trend);
        }

        document.close();
        return out.toByteArray();
    }

    private static void addSection(Document document, String title, Font headerFont, Font normalFont,
                                   String[] headers, Object[][] data) throws DocumentException {
        // 小节标题
        Paragraph sectionTitle = new Paragraph(title, headerFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // 表格
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);

        // 表头
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(51, 102, 153));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // 数据行
        for (Object[] row : data) {
            for (int i = 0; i < row.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(formatValue(row[i]), normalFont));
                cell.setPadding(6);
                if (i == row.length - 1) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                table.addCell(cell);
            }
        }

        document.add(table);
    }

    private static void addTrendSection(Document document, String title, Font headerFont, Font normalFont,
                                        List<Map<String, Object>> trend) throws DocumentException {
        Paragraph sectionTitle = new Paragraph(title, headerFont);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        // 表头
        String[] headers = {"月份", "收入", "支出"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(51, 102, 153));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // 数据
        for (Map<String, Object> month : trend) {
            PdfPCell cell1 = new PdfPCell(new Phrase(String.valueOf(month.get("month")), normalFont));
            cell1.setPadding(6);
            table.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase(formatValue(month.get("income")), normalFont));
            cell2.setPadding(6);
            cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell2);

            PdfPCell cell3 = new PdfPCell(new Phrase(formatValue(month.get("expense")), normalFont));
            cell3.setPadding(6);
            cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell3);
        }

        document.add(table);
    }

    private static String formatValue(Object value) {
        if (value == null) return "0.00";
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2, java.math.RoundingMode.HALF_UP).toString();
        }
        if (value instanceof Number) {
            return String.format("%.2f", ((Number) value).doubleValue());
        }
        return value.toString();
    }
}
