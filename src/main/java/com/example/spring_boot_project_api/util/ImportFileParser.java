package com.example.spring_boot_project_api.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Shared CSV / XLSX / XLS parsing used by import features.
 *
 * <p>Every file is converted into rows of trimmed string fields. The first record is assumed to
 * be a header and is kept (callers skip it); blank records are skipped. Callers map the columns
 * to their own DTOs, so the parser stays domain-agnostic.
 */
public final class ImportFileParser {

    private ImportFileParser() {
    }

    /**
     * Parses CSV, XLSX or XLS content into list of records (each record is a list of fields).
     *
     * @throws IOException if a spreadsheet file cannot be read as a workbook
     */
    public static List<List<String>> parseFileToRecords(byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            return List.of();
        }
        if (isSpreadsheetFormat(content)) {
            return parseSpreadsheetToRecords(content);
        }
        return parseCsvToRecords(content);
    }

    /** Detects the binary signature of XLSX (ZIP "PK") or XLS (OLE2 D0CF11E0). */
    public static boolean isSpreadsheetFormat(byte[] content) {
        if (content.length < 4) {
            return false;
        }
        int b0 = content[0] & 0xff;
        int b1 = content[1] & 0xff;
        if (b0 == 0x50 && b1 == 0x4b) {
            return true;
        }
        return content.length >= 8 && b0 == 0xd0 && b1 == 0xcf
                && (content[2] & 0xff) == 0x11 && (content[3] & 0xff) == 0xe0
                && (content[4] & 0xff) == 0xa1 && (content[5] & 0xff) == 0xb1
                && (content[6] & 0xff) == 0x1a && (content[7] & 0xff) == 0xe1;
    }

    /** Parses a plain-text CSV file (UTF-8, optional BOM, quoted fields, CRLF/LF/CR). */
    public static List<List<String>> parseCsvToRecords(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }

        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                record.add(field.toString().trim());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(field.toString().trim());
                field.setLength(0);
                if (!isBlankRecord(record)) {
                    records.add(record);
                }
                record = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        record.add(field.toString().trim());
        if (!isBlankRecord(record)) {
            records.add(record);
        }
        return records;
    }

    private static List<List<String>> parseSpreadsheetToRecords(byte[] content) throws IOException {
        try (InputStream in = new ByteArrayInputStream(content);
                Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<List<String>> records = new ArrayList<>();
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlankSpreadsheetRow(row)) {
                    continue;
                }
                List<String> record = new ArrayList<>();
                short last = row.getLastCellNum();
                for (int c = 0; c < last; c++) {
                    record.add(cellToString(row.getCell(c)));
                }
                records.add(record);
            }
            return records;
        }
    }

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        String value;
        switch (type) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double d = cell.getNumericCellValue();
                    value = d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case STRING:
                value = cell.getStringCellValue();
                break;
            default:
                value = null;
        }
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlankSpreadsheetRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < 8; i++) {
            if (cellToString(row.getCell(i)) != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlankRecord(List<String> record) {
        for (String value : record) {
            if (value != null && !value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Returns the field at {@code index} trimmed and truncated to {@code maxLength}, or null. */
    public static String stringField(List<String> fields, int index, int maxLength) {
        if (index >= fields.size()) {
            return null;
        }
        String value = fields.get(index);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /** Returns the field at {@code index} as an Integer, or null if missing/invalid. */
    public static Integer integerField(List<String> fields, int index) {
        if (index >= fields.size()) {
            return null;
        }
        String value = fields.get(index);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}