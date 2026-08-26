package com.xy6f.fiva_assistant.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExcelExportUtil {

    public static byte[] exportTransactions(List<?> transactions, String[] headers, String sheetName) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            int rowNum = 1;
            for (Object obj : transactions) {
                Row row = sheet.createRow(rowNum++);
                if (obj instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    int col = 0;
                    for (Object val : map.values()) {
                        Cell cell = row.createCell(col++);
                        setCellValue(cell, val);
                    }
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] exportReport(Map<String, Object> report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 利润表
            Sheet profitSheet = workbook.createSheet("利润表");
            createReportSheet(profitSheet, report);
            
            // 月度趋势
            Sheet trendSheet = workbook.createSheet("月度趋势");
            createTrendSheet(trendSheet, (List<Map<String, Object>>) report.get("monthlyTrend"));
            
            for (int i = 0; i < trendSheet.getLastRowNum(); i++) {
                trendSheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void createReportSheet(Sheet sheet, Map<String, Object> report) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // 利润表数据
        Map<String, Object> profit = (Map<String, Object>) report.get("profitStatement");
        int rowNum = 0;
        
        rowNum = createRow(sheet, rowNum, "项目", "金额", headerStyle);
        rowNum = createRow(sheet, rowNum, "营业收入", profit.get("totalIncome"), null);
        rowNum = createRow(sheet, rowNum, "减：营业费用", profit.get("totalExpense"), null);
        rowNum = createRow(sheet, rowNum, "毛利润", profit.get("grossProfit"), null);
        
        rowNum++;
        Map<String, Object> tax = (Map<String, Object>) report.get("taxSummary");
        rowNum = createRow(sheet, rowNum, "税费项目", "金额", headerStyle);
        rowNum = createRow(sheet, rowNum, "增值税(13%)", tax.get("estimatedVat"), null);
        rowNum = createRow(sheet, rowNum, "附加税(12%)", tax.get("surcharge"), null);
        rowNum = createRow(sheet, rowNum, "企业所得税(5%)", tax.get("incomeTax"), null);
        rowNum = createRow(sheet, rowNum, "税费合计", tax.get("totalTaxBurden"), null);
        
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createTrendSheet(Sheet sheet, List<Map<String, Object>> trend) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"月份", "收入", "支出"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        int rowNum = 1;
        for (Map<String, Object> month : trend) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(month.get("month")));
            setCellValue(row.createCell(1), month.get("income"));
            setCellValue(row.createCell(2), month.get("expense"));
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static int createRow(Sheet sheet, int rowNum, String col1, Object col2, CellStyle headerStyle) {
        Row row = sheet.createRow(rowNum++);
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(col1);
        if (headerStyle != null) {
            cell1.setCellStyle(headerStyle);
        }
        Cell cell2 = row.createCell(1);
        setCellValue(cell2, col2);
        return rowNum;
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof LocalDate) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
