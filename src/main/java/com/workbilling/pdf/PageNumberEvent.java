package com.workbilling.pdf;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class PageNumberEvent extends PdfPageEventHelper {

    private static final Font PAGE_FONT =
            new Font(Font.HELVETICA, 9, Font.NORMAL);

    @Override
    public void onEndPage(PdfWriter writer,
                          com.lowagie.text.Document document) {

        PdfContentByte cb = writer.getDirectContent();

        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_CENTER,
                new Phrase(String.valueOf(writer.getPageNumber()), PAGE_FONT),
                (document.left() + document.right()) / 2,
                document.bottom() - 15,
                0
        );
    }
}