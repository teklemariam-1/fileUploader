package com.example.sqlgeneratory.service;

import com.example.sqlgeneratory.exception.SQLGenerationException;
import com.example.sqlgeneratory.util.ZipUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class SQLGenerationService {

    public byte[] generateSQL(MultipartFile excelFile, String srNumber) throws SQLGenerationException {
        try (Workbook workbook = new XSSFWorkbook(excelFile.getInputStream());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheet("NOS-DBC");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'NOS-DBC' not found in the Excel file.");
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null || !validateHeader(headerRow)) {
                throw new IllegalArgumentException("Invalid header row. Expected column headers: dbName, name, last name, studentId");
            }

            Map<String, List<String>> dbToIdsMap = new HashMap<>();

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                Cell dbNameCell = row.getCell(0);
                Cell idCell = row.getCell(3);

                if (dbNameCell == null || idCell == null) {
                    continue;
                }

                String dbName = getCellValueAsString(dbNameCell,evaluator);
                String id = getCellValueAsString(idCell,evaluator);

                if (dbName.isEmpty() || id.isEmpty()) {
                    continue;
                }

                dbToIdsMap.putIfAbsent(dbName, new ArrayList<>());
                dbToIdsMap.get(dbName).add(id);
            }

            Map<String,String> dbNamesqlQueriesMap = new HashMap<>();
            String dbName = null;
            for (Map.Entry<String, List<String>> entry : dbToIdsMap.entrySet()) {
                dbName = entry.getKey();
                List<String> ids = entry.getValue();
                String idList = String.join(", ", ids);

                String formattedQuery = String.format("SELECT * FROM student WHERE id IN (%s);\n\n" +
                        "UPDATE student SET status = 'PASS' WHERE id IN (%s);\n\n" +
                        "SELECT * FROM student WHERE id IN (%s);", idList, idList, idList);
                dbNamesqlQueriesMap.put(dbName, formattedQuery);


            }

            // Create a zip archive containing all the SQL queries
            ZipUtils.createZipArchive(dbNamesqlQueriesMap, outputStream, srNumber);


            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SQLGenerationException("Error generating SQL files.", e);
        }
    }

    private static boolean validateHeader(Row headerRow) {
        String[] expectedHeaders = {"dbName", "name", "last name", "studentId"};
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !cell.getStringCellValue().equals(expectedHeaders[i])) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());

        } else if (cell.getCellType() == CellType.FORMULA) {
            CellValue cellValue = evaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case STRING:
                    return cellValue.getStringValue();
                case NUMERIC:
                    return String.valueOf((int) cellValue.getNumberValue());
            }

        }
        return "";
    }
}
