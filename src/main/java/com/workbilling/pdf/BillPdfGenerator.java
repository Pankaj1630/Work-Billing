package com.workbilling.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import com.workbilling.model.Bill;
import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.util.AppPaths;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class BillPdfGenerator {

    private static final Font TITLE_FONT =
            new Font(Font.HELVETICA, 16, Font.BOLD);

    private static final Font HEADER_FONT =
            new Font(Font.HELVETICA, 11, Font.BOLD);

    private static final Font NORMAL_FONT =
            new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font ITALIC_FONT =
            new Font(Font.HELVETICA, 10, Font.ITALIC);

    private static final Font BOLD_FONT =
            new Font(Font.HELVETICA, 10, Font.BOLD);

    private static final Font GRAND_TOTAL_FONT =
            new Font(Font.HELVETICA, 12, Font.BOLD);
            public void generate(Bill bill, Path outputPath) throws DocumentException, IOException {

    Files.createDirectories(outputPath.getParent());

    Document document = new Document(PageSize.A4, 36, 36, 48, 36);
       PdfWriter writer =
        PdfWriter.getInstance(document, Files.newOutputStream(outputPath));

         writer.setPageEvent(new PageNumberEvent());
    document.open();

    // ===========================
    // Header
    // ===========================

    PdfPTable headerTable = new PdfPTable(new float[]{2f, 1f});
    headerTable.setWidthPercentage(100);

    // Left Side
    PdfPCell leftCell = new PdfPCell();
    leftCell.setBorder(Rectangle.NO_BORDER);

    Paragraph title = new Paragraph("Virendra Vishwakarma", TITLE_FONT);
    title.setSpacingAfter(4);
    leftCell.addElement(title);

    Paragraph phone = new Paragraph("Phone No: +91 9137707072", NORMAL_FONT);
    leftCell.addElement(phone);
    Paragraph profile = new Paragraph("(All Kinds of Carpentry work,Plumber,\n"+
                                        "Painting and Civil Contractor)", ITALIC_FONT);   
    leftCell.addElement(profile);
    // Right Side
    PdfPCell rightCell = new PdfPCell();
    rightCell.setBorder(Rectangle.NO_BORDER);

    Paragraph address = new Paragraph(
            "Address: Floor-405, Plot-256\n" +
            "Bhargav Residency\n" +
            "Sector-24, Ulwe\n" +
            "Navi Mumbai, Maharashtra - 410206",
            NORMAL_FONT);

    address.setAlignment(Element.ALIGN_RIGHT);
    rightCell.addElement(address);

    headerTable.addCell(leftCell);
    headerTable.addCell(rightCell);

    document.add(headerTable);

    // Horizontal Line
    LineSeparator line = new LineSeparator();
    line.setLineWidth(1f);
    document.add(line);

    document.add(Chunk.NEWLINE);

    // ===========================
    // Bill Details
    // ===========================

    document.add(new Paragraph(
            "Company : " + bill.getCompanyName(),
            HEADER_FONT));

    document.add(new Paragraph(
            "Bill Number : " + bill.getBillNumber(),
            NORMAL_FONT));

    LocalDate generatedDate = bill.getGeneratedAt() != null
            ? bill.getGeneratedAt().toLocalDate()
            : LocalDate.now();

    document.add(new Paragraph(
            "Generated Date : " +
                    generatedDate.format(AppPaths.DISPLAY_DATE),
            NORMAL_FONT));

    document.add(Chunk.NEWLINE);

    // ===========================
    // Work Entries
    // ===========================

bill.getWorkEntries().stream()
            .sorted(java.util.Comparator.comparing(WorkEntry::getWorkDate))
            .forEach(entry -> {
                try {
                    addDateSection(document, entry);

                    document.add(new Paragraph(
                            "------------------------------------------------------------",
                            NORMAL_FONT));

                    document.add(Chunk.NEWLINE);
                } catch (DocumentException e) {
                    throw new RuntimeException(e);
                }
            });

    // ===========================
    // Grand Total
    // ===========================

     document.add(new Paragraph(
                "______________________________________________________________________________________________",
                NORMAL_FONT));
    Paragraph grandTotal = new Paragraph(
            "GRAND TOTAL : " +
                    AppPaths.formatCurrency(bill.getGrandTotal())+" Rs.",
            GRAND_TOTAL_FONT);

    grandTotal.setAlignment(Element.ALIGN_CENTER);
    grandTotal.setSpacingBefore(10);

    document.add(grandTotal);
    document.add(new Paragraph(
                "______________________________________________________________________________________________",
                NORMAL_FONT));
    document.close();
    
 }
 private void addDateSection(Document document, WorkEntry entry) throws DocumentException {

    Paragraph dateHeader = new Paragraph(
            "Date : " + entry.getWorkDate().format(AppPaths.DISPLAY_DATE),
            HEADER_FONT);
    dateHeader.setSpacingAfter(6);
    document.add(dateHeader);

    int areaNo = 1;

    for (WorkArea area : entry.getAreas()) {

        // Area Heading
        Paragraph areaHeader = new Paragraph(
                areaNo + ". " + area.getName(),
                BOLD_FONT);
        areaHeader.setSpacingBefore(1);
        areaHeader.setSpacingAfter(1);
        document.add(areaHeader);


        // Table
        PdfPTable table = new PdfPTable(
                new float[]{0.6f, 3.4f, 1f, 1f, 1.3f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(1);
        table.setSpacingAfter(0);

        // Header Row
        addHeaderCell(table, "Sr.");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Sq.Ft");
        addHeaderCell(table, "Nos");
        addHeaderCell(table, "Rate");
        addHeaderCell(table, "Amount");

        int itemNo = 1;

        for (WorkItem item : area.getItems()) {

            addCell(table, String.valueOf(itemNo++), Element.ALIGN_CENTER);
            addCell(table, item.getDescription(), Element.ALIGN_LEFT);
            addCell(table, item.getSqFtDisplay(), Element.ALIGN_CENTER);
            addCell(table, item.getNosDisplay(), Element.ALIGN_CENTER);
            addCell(table, item.isManualAmount() ? "NA" : AppPaths.formatCurrency(item.getRate()), Element.ALIGN_RIGHT);
            addCell(table, AppPaths.formatCurrency(item.getAmount()), Element.ALIGN_RIGHT);
        }

        document.add(table);

        Paragraph areaTotal = new Paragraph(
                "Area Total : " + AppPaths.formatCurrency(area.getTotal()),
                BOLD_FONT);

        areaTotal.setAlignment(Element.ALIGN_RIGHT);
        areaTotal.setSpacingAfter(3);

        document.add(areaTotal);

        areaNo++;
    }

    Paragraph dateTotal = new Paragraph(
            "Date Total : " + AppPaths.formatCurrency(entry.getDateTotal()),
            HEADER_FONT);

    dateTotal.setAlignment(Element.ALIGN_RIGHT);
    dateTotal.setSpacingBefore(2);
    dateTotal.setSpacingAfter(4);

    document.add(dateTotal);
}
private void addHeaderCell(PdfPTable table, String text) {

    PdfPCell cell = new PdfPCell(new Phrase(text, BOLD_FONT));
    cell.setBackgroundColor(new Color(230, 230, 230));
    cell.setPadding(1f);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

    table.addCell(cell);
}

private void addCell(PdfPTable table, String text, int alignment) {

    PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
    cell.setPaddingTop(1);
    cell.setPaddingBottom(1);
    cell.setPaddingLeft(3);
    cell.setPaddingRight(3);
    cell.setHorizontalAlignment(alignment);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

    table.addCell(cell);
}
private String formatNumber(double value) {

    if (value == Math.floor(value)) {
        return String.format("%.0f", value);
    }

    return String.format("%.2f", value);
}
}
