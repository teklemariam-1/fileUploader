package com.example.sqlgeneratory.util;

import com.example.sqlgeneratory.exception.SQLGenerationException;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class ZipUtils {
    public static void createZipArchive(Map<String,String> dbNamesqlQueriesMap , OutputStream outputStream, String srNumber) throws SQLGenerationException {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, String> entry : dbNamesqlQueriesMap.entrySet()) {
                String sqlQuery = entry.getValue();
                String dbName = entry.getKey();
                String fileName = dbName + "-" + "SR_" + srNumber  + ".sql";
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(sqlQuery.getBytes());
                zipOut.closeEntry();
            }

        } catch (IOException e) {
            throw new SQLGenerationException("Error creating zip archive.", e);
        }
    }
}

