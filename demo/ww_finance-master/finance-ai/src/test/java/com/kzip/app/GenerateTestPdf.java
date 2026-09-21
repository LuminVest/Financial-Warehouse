package com.kzip.app;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

/**
 * 生成一个带目录（书签）的测试 PDF
 * 用于测试 ParagraphPdfDocumentReader
 */
public class GenerateTestPdf {

    public static void main(String[] args) throws Exception {
        PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        PDDocument doc = new PDDocument();
        PDDocumentOutline outline = new PDDocumentOutline();
        doc.getDocumentCatalog().setDocumentOutline(outline);

        // 第1页：封面
        PDPage page1 = new PDPage(PDRectangle.A4);
        doc.addPage(page1);
        PDPageContentStream cs1 = new PDPageContentStream(doc, page1);
        cs1.beginText();
        cs1.setFont(helveticaBold, 24);
        cs1.newLineAtOffset(100, 700);
        cs1.showText("WangWang Finance FAQ");
        cs1.setFont(helvetica, 12);
        cs1.newLineAtOffset(0, -40);
        cs1.showText("Frequently Asked Questions");
        cs1.endText();
        cs1.close();

        // 第2页：还款方式（加书签）
        PDPage page2 = new PDPage(PDRectangle.A4);
        doc.addPage(page2);
        PDPageContentStream cs2 = new PDPageContentStream(doc, page2);
        cs2.beginText();
        cs2.setFont(helveticaBold, 18);
        cs2.newLineAtOffset(72, 750);
        cs2.showText("Repayment Methods");
        cs2.setFont(helvetica, 12);
        cs2.newLineAtOffset(0, -40);
        cs2.showText("1. Equal Principal and Interest");
        cs2.newLineAtOffset(20, -20);
        cs2.showText("Monthly payment stays the same.");
        cs2.newLineAtOffset(-20, -30);
        cs2.showText("2. Interest First");
        cs2.newLineAtOffset(20, -20);
        cs2.showText("Pay interest only each month.");
        cs2.newLineAtOffset(-20, -30);
        cs2.showText("3. Drawdown Anytime");
        cs2.newLineAtOffset(20, -20);
        cs2.showText("Borrow and repay anytime.");
        cs2.endText();
        cs2.close();

        PDOutlineItem bookmark1 = new PDOutlineItem();
        bookmark1.setTitle("Repayment Methods");
        bookmark1.setDestination(page2);
        outline.addLast(bookmark1);

        // 第3页：法人条件（加书签）
        PDPage page3 = new PDPage(PDRectangle.A4);
        doc.addPage(page3);
        PDPageContentStream cs3 = new PDPageContentStream(doc, page3);
        cs3.beginText();
        cs3.setFont(helveticaBold, 18);
        cs3.newLineAtOffset(72, 750);
        cs3.showText("Legal Person Requirements");
        cs3.setFont(helvetica, 12);
        cs3.newLineAtOffset(0, -40);
        cs3.showText("- Age between 18 and 65 years old");
        cs3.newLineAtOffset(0, -20);
        cs3.showText("- Good credit history");
        cs3.newLineAtOffset(0, -20);
        cs3.showText("- Registered in China for at least 1 year");
        cs3.newLineAtOffset(0, -20);
        cs3.showText("- No major tax violations");
        cs3.endText();
        cs3.close();

        PDOutlineItem bookmark2 = new PDOutlineItem();
        bookmark2.setTitle("Legal Person Requirements");
        bookmark2.setDestination(page3);
        outline.addLast(bookmark2);

        // 第4页：审批时间（加书签）
        PDPage page4 = new PDPage(PDRectangle.A4);
        doc.addPage(page4);
        PDPageContentStream cs4 = new PDPageContentStream(doc, page4);
        cs4.beginText();
        cs4.setFont(helveticaBold, 18);
        cs4.newLineAtOffset(72, 750);
        cs4.showText("Approval Time");
        cs4.setFont(helvetica, 12);
        cs4.newLineAtOffset(0, -40);
        cs4.showText("- Business loan: 1-3 working days");
        cs4.newLineAtOffset(0, -20);
        cs4.showText("- Invoice loan: same day approval");
        cs4.newLineAtOffset(0, -20);
        cs4.showText("- Tax invoice loan: 1-2 working days");
        cs4.endText();
        cs4.close();

        PDOutlineItem bookmark3 = new PDOutlineItem();
        bookmark3.setTitle("Approval Time");
        bookmark3.setDestination(page4);
        outline.addLast(bookmark3);

        // 设置书签可见
        outline.openNode();

        // 保存
        doc.save("D:/financial-warehouse/demo/ww_finance-master/test_with_toc.pdf");
        doc.close();

        System.out.println("Generated test_with_toc.pdf with bookmarks (TOC)");
    }
}
