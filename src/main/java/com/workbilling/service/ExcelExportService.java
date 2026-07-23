package com.workbilling.service;

import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.util.AppPaths;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportService {

    public Path exportWorkEntries(List<WorkEntry> entries) throws IOException {
        Path exportDir = AppPaths.getAppDataDir().resolve("Exports");
        Files.createDirectories(exportDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = exportDir.resolve("work_entries_" + timestamp + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Work Entries");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] columns = {"Date", "Company", "Status", "Area", "Description", "Sq.Ft", "Nos", "Rate", "Amount"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (WorkEntry entry : entries) {
                for (WorkArea area : entry.getAreas()) {
                    for (WorkItem item : area.getItems()) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(entry.getWorkDate().format(AppPaths.DISPLAY_DATE));
                        row.createCell(1).setCellValue(entry.getCompanyName());
                        row.createCell(2).setCellValue(entry.getStatus().name());
                        row.createCell(3).setCellValue(area.getName());
                        row.createCell(4).setCellValue(item.getDescription());
                        if (item.hasSqFt()) {
                            row.createCell(5).setCellValue(item.getSqFt());
                        } else {
                            row.createCell(5).setCellValue("NA");
                        }
                        row.createCell(6).setCellValue(item.getNos());
                        row.createCell(7).setCellValue(item.getRate());
                        row.createCell(8).setCellValue(item.getAmount());
                    }
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
        return file;
    }
}
