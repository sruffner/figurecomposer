package com.srscicomp.fc.fig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.DefaultFontMapper.BaseFontParameters;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.UnicodeSubset;
import com.srscicomp.common.util.NeverOccursException;
import com.srscicomp.common.util.Utilities;

/**
 * Provides support for exporting FigureComposer figures to PDF via the ITextPDF library.
 * 
 * <p>NOTE: As of Dec 6, 2022 -- <b>PDFSupport</b> uses {@link PdfGraphics2DEx} under the hood, rather than the original
 * {@link com.itextpdf.awt.PdfGraphics2D PdfGraphics2D} from the ITextPDF library. <b>PdfGraphics2DEx</b> supports
 * rendering superscripted characters within an attributed string. In addition, if any string to be rendered includes at
 * least one character without a corresponding glyph in the PDF base font, it will render the entire string using the
 * AWT font-supplied glyph vectors instead. This offers better results compared to the font substitution mechanism
 * implemented via {@link #doFontSubstitutionIfNeeded(String, Font)}. That method is now deprecated and is a "no-op" (so
 * that we don't have to change the code throughout FC's source code).</p>
 * 
 * @author sruffner
 *
 */
public class PDFSupport 
{
   /**
    * Is the specified graphics context customized for exporting a <i>FypML</i> figure to a Portable Document Format 
    * (PDF) file? Some graphic nodes in <i>FypML</i> -- particularly any node that renders text -- behave differently
    * when drawing to the PDF graphics context versus the normal Java2D context.
    * 
    * @param g2 The graphics context to test.
    * @return True if the context is an instance of the custom context prepared by {@link #exportFigure()} in order to
    * use the existing render infrastructure to export a <i>FypML</i> figure to a PDF document.
    */
   public static boolean isPDFGraphics(Graphics2D g2) { return(g2 instanceof PdfGraphics2DEx); }
   
   /**
    * Get the application-wide singleton object providing Portable Document Format (PDF)-related support functions. The 
    * singleton instance is initialized on the first call to this method. Since initializations may take an indefinite
    * period of time, this method should be invoked during application start-up.
    * 
    * @return The PDF support object. 
    */
   public static PDFSupport getInstance()
   {
      if(singleton == null) singleton = new PDFSupport();
      return(singleton);
   }
   
   /**
    * Export a renderable object tree to a single-page PDF document using the specified page layout.
    * 
    * @param root The root of a renderable object tree. For a FypML figure, this is the root node of the FGraphicModel.
    * @param pgFmt The page layout.
    * @param dst Destination file path. It must be reachable. An extension of ".pdf" is added to the path if needed.
    * @return Null if operation is successful, else an error message.
    */
   public String exportFigure(RootRenderable root, PageFormat pgFmt, File dst)
   {
      boolean dstOK = (dst != null);
      if(dstOK)
      {
         File dir = dst.getParentFile();
         dstOK = dir != null && dir.isDirectory();
      }
      if(dstOK)
      {
         String fn = dst.getName();
         dstOK = !fn.isEmpty();
         if(dstOK && !fn.toLowerCase().endsWith(".pdf")) dst = new File(dst.getAbsolutePath() + ".pdf");
      }
      if(!dstOK) return("Destination file path invalid");
      
      // get paper size and margins for the single-page PDF document
      float w = 8.5f * 72;
      float h = 11 * 72;
      float left = 36;
      float right = 36;
      float top = 36;
      float bot = 36;
      if(pgFmt != null)
      {
         w = (float) pgFmt.getWidth();
         h = (float) pgFmt.getHeight();
         left = (float) pgFmt.getImageableX();
         right = w - (float) (pgFmt.getImageableX() + pgFmt.getImageableWidth());
         top = (float) pgFmt.getImageableY();
         bot = h - (float) (pgFmt.getImageableY() + pgFmt.getImageableHeight());
      }
      
      // create the PDF document and prepare the writer
      Document pdfDoc = null;
      Graphics2D g2 = null;
      String emsg = null;
      try
      {
         pdfDoc = new Document(new RectangleReadOnly(w,h), left, right, top, bot);
         PdfWriter writer = PdfWriter.getInstance(pdfDoc, new FileOutputStream(dst));
         pdfDoc.open();
         
         // create a Graphics2D clipped to printable area inside margins, w/ origin at BL corner of printable area and
         // Y-axis increasing upwards rather than downwards
         PdfContentByte cb = writer.getDirectContent();
         g2 = new PdfGraphics2DEx(cb, w, h, fontMapper);
         g2.translate(left, h-bot);
         g2.scale(1, -1);
         g2.clipRect(0, 0, (int) pgFmt.getImageableWidth(), (int) pgFmt.getImageableHeight());
          
         // now rescale to milli-inches
         g2.scale(72.0/1000.0, 72.0/1000.0);
         
         // translate origin from bottom-left corner of imageable area to bottom-left corner of the rendered objectj
         // tree within that area
         Point2D figOri = root.getPrintLocationMI();
         g2.translate(figOri.getX(), figOri.getY());
         
         // render the figure into the PDF graphics context
         root.render(g2, null);
      }
      catch(Exception e)
      {
         emsg = "Export-to-PDF failed: " + e.getMessage();
      }
      finally
      {
         if(g2 != null) g2.dispose();
         if(pdfDoc != null) pdfDoc.close();
      }
      
      return(emsg);
   }
   
   /**
    * <b>This method is deprecated and no longer has any effect, always returning null. Instead of doing font 
    * substitution, the underlying graphics context that generates a PDF document will render the AWT font-supplied
    * glyph vector representing any string containing one or more characters not available in the mapped PDF font.</b>
    * 
    * <p>This changes a {@link String String} to an {@link AttributedString AttributedString} if the
    * original string contains any characters that don't exist in the physical PDF font mapped to the specified AWT 
    * font. In this scenario, each such character is attached to a substitute AWT font matched to a built-in physical 
    * font -- either Symbol or a Type 1 Latin font -- that can render it. The idea here is to ensure that any text 
    * (containing only characters supported by FypML, of course) in a FypML figure is fully rendered upon export to 
    * PDF.</p>
    * 
    * @param s The string to test.
    * @param awtFont The AWT font in which the string is to be drawn. If null, a default physical PDF font is chosen.
    * @return Null if the mapped physical font can draw all the characters in the string. Otherwise, method forms an
    * attributed string that draws any missing characters in a substitute font.
    */
   public AttributedString doFontSubstitutionIfNeeded(String s, Font awtFont)
   {
      return(null);
      /*
      if(awtFont == null || s == null || s.isEmpty()) return(null);
      
      BaseFont pdfFont = fontMapper.awtToPdf(awtFont);

      List<Integer> missingChars = null;
      for(int i=0; i<s.length(); i++)
      {
         if(!pdfFont.charExists(s.charAt(i)))
         {
            if(missingChars == null) missingChars = new ArrayList<Integer>();
            missingChars.add(new Integer(i));
         }
      }
      if(missingChars == null) return(null);
      
      AttributedString as = new AttributedString(s);
      as.addAttribute(TextAttribute.FONT, awtFont);
      
      BaseFont pdfSubstFont = fontMapper.getSubstituteBuiltInFont(awtFont);
      if(pdfSubstFont.getPostscriptFontName().toLowerCase().contains("symbol"))
      {
         // for the silly case in which user chooses to use "Symbol" as the AWT font. We need a Latin font to handle
         // most supported characters!
         String name = awtFont.getFontName().toLowerCase(Locale.ENGLISH);
         String logFont = Font.SANS_SERIF;
         if(name.indexOf("courier") >= 0) logFont = Font.MONOSPACED;
         else if(name.indexOf("times") >= 0) logFont = Font.SERIF;
         pdfSubstFont = fontMapper.getSubstituteBuiltInFont(new Font(logFont, awtFont.getSize(), awtFont.getStyle()));
      }
      BaseFont pdfSymFont = fontMapper.getBuiltInSymbolFont();
      
      Font awtSubstFont = fontMapper.pdfToAwt(pdfSubstFont, awtFont.getSize());
      Font awtSymFont = fontMapper.pdfToAwt(pdfSymFont, awtFont.getSize());
      
      for(Integer pos : missingChars)
      {
         char c = s.charAt(pos);
         as.addAttribute(TextAttribute.FONT, pdfSubstFont.charExists(c) ? awtSubstFont : awtSymFont, pos, pos+1);
      }
      
      return(as);
      */
   }
   
   /**
    * Returns the names of all AWT fonts for which a suitable substitute PDF font was found.
    */
   public List<String> getMappedFonts()
   {
      List<String> out = new ArrayList<String>();
      out.addAll(this.fontMapper.getMapper().keySet());
      return out;
   }
   
   /**
    * Prints to STDOUT the current mappings of AWT fonts on the host to available PDF fonts.
    */
   public void showFontMappings()
   {
      List<String> fontFaces = this.getMappedFonts();
      System.out.println("Current AWT --> PDF font mappings (N=" + fontFaces.size() + "):");
      
      Collections.sort(fontFaces);
      for(String face : fontFaces)
      {
         BaseFontParameters bfp = this.fontMapper.getMapper().get(face);
         System.out.print(face + "   -->  " + bfp.fontName + " (" + bfp.encoding +")");
         
         try 
         {
            BaseFont bf = BaseFont.createFont(bfp.fontName, BaseFont.IDENTITY_H, bfp.embedded, BaseFont.NOT_CACHED, 
                  bfp.ttfAfm, bfp.pfb);
            
            // some minimum checks to confirm iText library can handle this font reasonably well. At least on Mac OSX,
            // any parsable font marked as "direct text to byte" or "font specific" did not work correctly. Also verify
            // that, at the very least, all of the printable ASCII character set is available in the font. If the 
            // physical font does not pass these tests, remove it from the font mapper so we don't test it again.
            if(bf != null)
            {
               boolean direct = bf.isDirectTextToByte();
               boolean fontSpecific = bf.isFontSpecific();
               int nOk = 0;
               for(int c = 0x20; c <= 0x7E; c++) 
               {
                  if(bf.charExists(c)) ++nOk;
               }
               System.out.println("... direct=" + direct + "; fontSpecific=" + fontSpecific + 
                     "; printableASCII=" + nOk + "/95");
               
            }
         }
         catch(Exception e) {
            System.out.println(" ... Unable to create PDF font.");
         }
      }
   }
   

   /**
    * For test/development only.
    * 
    * @param args no arguments needed
    * @throws IOException
    * @throws DocumentException
    */
   public static void main(String[] args) throws IOException, DocumentException 
   {
      System.out.print("Initializing PDF support...");
      long tStart = System.currentTimeMillis();
      PDFSupport pdfSupport = PDFSupport.getInstance();
      System.out.println("(dur = " + (System.currentTimeMillis()-tStart) + " ms)\n");
       
      System.out.println(
            "Choose: 1 = fonts test, 2 = simple page, 3 = supported chars, 4 = export to PDF");
      System.out.print(">> ");
       
      Scanner scanIn = new Scanner(System.in);
      String choice = scanIn.nextLine().trim();

      if(choice.equals("3"))
         System.out.print("Specify destination directory >> ");
      else
         System.out.print("Specify destination or source file path >>");
      String path = scanIn.nextLine().trim();
      
      tStart = System.currentTimeMillis();
      if(choice.equals("1")) 
         pdfSupport.testFonts(new File(path));
      else if(choice.equals("2")) 
         pdfSupport.testGraphics(new File(path));
      else if(choice.equals("3"))
      {
         System.out.print("Specify font family name >> ");
         String family = scanIn.nextLine().trim();
         System.out.print("Specify style [b=bold, i=italic, I=bolditalic; else plain] >> ");
         String style = scanIn.nextLine().trim();
         FontStyle fs = FontStyle.PLAIN;
         if("b".equals(style)) fs = FontStyle.BOLD;
         else if("i".equals(style)) fs = FontStyle.ITALIC;
         else if("I".equals(style)) fs = FontStyle.BOLDITALIC;
         tStart = System.currentTimeMillis();
         pdfSupport.testAllCharsEx(path, family, fs);
      }
      else if(choice.equals("4"))
         pdfSupport.testExport(new File(path));
      else
      {
         System.out.println("Sorry, [" + choice + "] is not a valid choice.");
      }
       
      scanIn.close();
      System.out.println("Total elapsed time (ms) = " + (System.currentTimeMillis()-tStart));
   }
    
   /** 
    * A test fixture that enumerates the mapping of Java AWT fonts to physical fonts that are installed on the host and 
    * supported by <code>PDFSupport</code>. The results are written to the file specified. Any unsupported font is
    * replaced by a substitute font from among the built-in standard Type 1 fonts.
    * 
    * @param dst File to which results are written (plain-text).
    * @throws IOException 
    * @throws DocumentException
    */
   private void testFonts(File dst) throws IOException, DocumentException
   {
      PrintStream out = new PrintStream(new FileOutputStream(dst));
      out.println("Fonts discovered by iText (n=" + fontMapper.getNumFontsMapped() + "):");
      Set<String> keys = fontMapper.getMapper().keySet();
      List<String> sortedKeys = new ArrayList<String>(keys);
      Collections.sort(sortedKeys);
      for(String name : sortedKeys) out.println("   " + name);

      out.println();
      out.println("PDF font aliases:");
      sortedKeys = new ArrayList<String>(fontMapper.getAliases().keySet());
      Collections.sort(sortedKeys);
      for(String name : sortedKeys) out.println("   " + name + "  -->  " + fontMapper.getAliases().get(name));
      
      out.println();
      out.println("AWT-to-PDF font mappings:");
      
      UnicodeSubset allChars = FGraphicModel.getSupportedCharacterSet();

      LocalFontEnvironment.initialize();
      String[] families = LocalFontEnvironment.getInstalledFontFamilies();
      for(String family : families)
      {
         out.println(family + ":");
         Font[] variants = LocalFontEnvironment.getInstalledVariants(family);
         if(variants == null) out.println("   ??? No variants found ???");
         else for(int i=0; i<variants.length; i++)
         {
            BaseFont pdfFont = fontMapper.awtToPdf(variants[i]);
            String substituted = fontMapper.wasSubstituted(variants[i]) ? "SUB" : "   ";
            String encoding = pdfFont.getEncoding();
            
            Iterator<Character> iterC = allChars.getCharIterator();
            int nValid = 0;
            while(iterC.hasNext())
            {
               Character c = iterC.next();
               if(Character.isWhitespace(c) || pdfFont.charExists(c)) ++nValid;
            }
            out.println(String.format("   %40s ==> %40s %s %3d  [%s]", 
                  variants[i].getFontName(), pdfFont.getPostscriptFontName(), substituted, nValid, encoding));
         }
      }
      
      out.println();
      out.println("Total mappings after check: " + fontMapper.getNumFontsMapped());
      out.flush();
      out.close();
   }

   /**
    * A test fixture that uses {@link com.itextpdf.awt.PdfGraphics2D} to draw a simple graphic and some text to a
    * single-page PDF.
    * 
    * @param dst The destination PDF file path
    * @throws IOException
    * @throws DocumentException
    */
   private void testGraphics(File dst) throws IOException, DocumentException
   {
      Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
      PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dst));
      document.open();
       
      // create a Graphics2D clipped to printable area inside margins, w/ origin at BL corner of printable area and
      // Y-axis increasing upwards rather than downwards
      PdfContentByte cb = writer.getDirectContent();
      Graphics2D g2 = new PdfGraphics2DEx(cb, 612, 792, fontMapper);
      g2.translate(36, 792-36);
      g2.scale(1, -1);
      g2.clipRect(0, 0, 540, 720);
       
      // now rescale to milli-inches and see if I can get everything to work...
      g2.scale(72.0/1000.0, 72.0/1000.0);
      int w = 7500;
      int h = 10000;

      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(10));
      g2.drawRect(5, 5, w-10, h-10);
      g2.drawLine(w/2, 5, w/2, h-5);
      g2.drawLine(5, h/3, w-5, h/3);
      g2.drawLine(5, 2*h/3, w-5, 2*h/3);
       
      double fontSzMI = 20 * Measure.PT2IN * 1000.0;
      PainterStyle style = BasicPainterStyle.createBasicPainterStyle(
            BasicPainterStyle.getFontForPainter("Comic Sans MS", GenericFont.SANSERIF, FontStyle.BOLD, fontSzMI), 
            10, null, Color.BLACK, Color.RED);
       
      ShapePainter painter = new ShapePainter();
      painter.setPaintedShape(Marker.CIRCLE);
      painter.setFilled(true);
      painter.setSize(1000);
      painter.setStyle(style);
      List<Point2D> locs = new ArrayList<Point2D>(1);
      locs.add(new Point2D.Double(w/2,h/2));
      painter.setLocationProducer(locs);
      painter.render(g2, null);
       
      Point2D textLoc = new Point2D.Double(w/2, h/2 + 520);
      StringPainter textPainter = new StringPainter();
      textPainter.setStyle(style);
      textPainter.setAlignment(TextAlign.CENTERED, TextAlign.TRAILING);
      textPainter.setTextAndLocation("Pear", textLoc);
      textPainter.render(g2, null);

      textLoc.setLocation(w/2, h/3);
      textPainter.setAlignment(TextAlign.LEADING, TextAlign.TRAILING);
      textPainter.setTextAndLocation("Left and bottom aligned", textLoc);
      textPainter.render(g2, null);
       
      textPainter.setAlignment(TextAlign.TRAILING, TextAlign.LEADING);
      textPainter.setTextAndLocation("Right and top aligned", textLoc);
      textPainter.render(g2, null);
       
      textLoc.setLocation(w/2, 2*h/3);
      textPainter.setRotation(-45);
      textPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      textPainter.setTextAndLocation("Centered, rotated 45deg", textLoc);
      textPainter.render(g2, null);

      g2.dispose();
       
      document.close();
   }

   /**
    * A test fixture that prepares a PDF document displaying all FC-supported Unicode characters using the specified 
    * font family and style. 
    * 
    * Each character that lacks a glyph code in the mapped PDF font -- and which is rendered instead by drawing and 
    * filling the glyph vector supplied by the corresponding AWT font (AWT fonts are better at covering all of the
    * Unicode characters in FC's supported character set than ITextPDF's BaseFont class) -- is rendered in RED in the 
    * PDF. A summary line at the bottom of the document reports the number of characters -- other than control or 
    * whitespace characters -- that exist in the mapped PDF font and how many were displayable in the AWT font.
    * 
    * @param dirPath Full path to the desired directory to which the PDF file should be written. Must exist. The 
    * generated PDF will have the file name "{font-family}-{style}.pdf"
    * @param fam Desired font family name.
    * @param fs Desired font style.
    * @throws IOException
    * @throws DocumentException
    */
   private void testAllCharsEx(String dirPath, String fam, FontStyle fs) throws IOException, DocumentException
   {
      File dir = new File(dirPath);
      if(!dir.isDirectory())
      {
         System.out.println("  ! Directory does not exist");
         return;
      }
      if(fs == null) fs = FontStyle.PLAIN;
      
      String fileName = fam + "-" + fs.toString() + ".pdf";
      File f = new File(dirPath, fileName);
      
      Document document = new Document(PageSize.LEGAL, 36, 36, 36, 36);
      PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(f));
      document.open();
      
      // painter styles for drawing all the character codes: supported are in black text, unsupported in red text
      double fontSzMI = 10 * Measure.PT2IN * 1000.0;
      PainterStyle style = BasicPainterStyle.createBasicPainterStyle(
            BasicPainterStyle.getFontForPainter(fam, GenericFont.SANSERIF, fs, fontSzMI), 
            10, null, Color.BLACK, Color.BLACK);
      Font awtFont = style.getFont();

      PainterStyle redStyle = BasicPainterStyle.createBasicPainterStyle(
            BasicPainterStyle.getFontForPainter(fam, GenericFont.SANSERIF, fs, fontSzMI), 
            10, null, Color.BLACK, Color.RED);
      
      // get the PDF font that will be used for the AWT font so that we can test existence of each char glyph
      BaseFont pdfFont = fontMapper.awtToPdf(awtFont);
      
      // create a Graphics2D clipped to printable area inside margins, w/ origin at BL corner of printable area and
      // Y-axis increasing upwards rather than downwards. NOTE: Legal page size with 0.5-in margins all around
      PdfContentByte cb = writer.getDirectContent();
      Graphics2D g2 = new PdfGraphics2DEx(cb, 612, 1008, fontMapper);
      g2.translate(36, 1008-36);
      g2.scale(1, -1);
      g2.clipRect(0, 0, 540, 936);
      
      // now rescale to milli-inches 
      g2.scale(72.0/1000.0, 72.0/1000.0);

      // draw title in monospace font, bold 14pt
      SingleStringPainter ssPainter = new SingleStringPainter();
      ssPainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
            BasicPainterStyle.getFontForPainter("Courier", GenericFont.MONOSPACE, FontStyle.BOLD, 
                  12*Measure.PT2IN*1000.0), 10, null, Color.BLACK, Color.BLACK));
      ssPainter.setAlignment(TextAlign.CENTERED, TextAlign.TRAILING);
      Point2D textLoc = new Point2D.Double(3500, 12700);
      ssPainter.setTextAndLocation("FC-Supported Characters in " + fam + "-" + fs.toString(), textLoc);
      ssPainter.render(g2, null);
      
      // draw a centered text line under title that displays the PS name of the physical font actually used
      textLoc.setLocation(3500, 12750 - 1.5 * 12*Measure.PT2IN*1000.0);
      ssPainter.setTextAndLocation(String.format("(AWT font = %s, PDF font = %s)", awtFont.getFontName(), 
            pdfFont.getPostscriptFontName()), textLoc);
      ssPainter.render(g2, null);
      
      // now set up painter for drawing character codes in the requested font.
      ssPainter.setStyle(style);
      ssPainter.setAlignment(TextAlign.LEADING, TextAlign.TRAILING);
      
      // start near the top of the page, but below the title
      double lineSz = 1.5 * fontSzMI;
      double x = 0;
      double y = 12000;
      textLoc.setLocation(x,y);
      
      UnicodeSubset allChars = FGraphicModel.getSupportedCharacterSet();
      Iterator<Character> iterC = allChars.getCharIterator();
      int nAwt = 0;  // total # of characters that AWT font can display
      int nPdf = 0;  // total # of characters for which PDF font has an installed glyph
      while(iterC.hasNext())
      {
         Character c = iterC.next();
         boolean iso = Character.isISOControl(c) || Character.isWhitespace(c);
         String s = null;
         if(iso)
            s = String.format("u%04x=  ", (int) c);
         else
            s = String.format("u%04x= %s", (int) c, c.toString());
         
         if(!iso)
         {
            if(pdfFont.charExists(c)) ++nPdf;
            if(awtFont.canDisplay(c)) ++nAwt;
         }
         
         ssPainter.setStyle(iso || pdfFont.charExists(c) ? style : redStyle);
         ssPainter.setTextAndLocation(s, textLoc);
         ssPainter.render(g2, null);
         
         x += 1250;
         if(x > 7000)
         {
            x = 0;
            y -= lineSz;
         }
         textLoc.setLocation(x,y);
      }
      
      // report how many characters were handled by the PDF font 
      x=0;
      y -= lineSz * 2;
      textLoc.setLocation(x, y);
      ssPainter.setStyle(style);
      ssPainter.setTextAndLocation(
            String.format("==> #Printable characters supported by PDF, AWT: %d, %d ", nPdf, nAwt), textLoc);
      ssPainter.render(g2, null);
      
      g2.dispose();
      
      document.close();
   }

   /**
    * A test fixture that reads in a FypML figure from file and exports it as a single-page PDF to the same file, with
    * with the extension changed to ".pdf".
    * 
    * @param figFile The FypML file defining the figure to be exported to PDF.
    */
   private void testExport(File figFile)
   {
      // load the figure
      StringBuffer eBuf = new StringBuffer();
      FGraphicModel fgm = FGModelSchemaConverter.fromXML(figFile, eBuf);
      if(fgm == null)
      {
         System.out.println("Failed to load figure: " + eBuf.toString());
         return;
      }
      
      // standard legal size page in portrait orientation with 0.5-in margins all around.
      PageFormat pgFmt = new PageFormat();
      Paper paper = pgFmt.getPaper();
      paper.setSize(8.5*72, 14*72);
      paper.setImageableArea(0.5*72, 0.5*72, 7.5*72, 13*72);
      pgFmt.setPaper(paper);
      
      // destination file path is same a source file path, with extension changed to ".pdf"
      String path = figFile.getAbsolutePath();
      int idx = path.lastIndexOf('.');
      File dst = new File(path.substring(0, idx) + ".pdf");
      
      String emsg = exportFigure(fgm.getCurrentRootRenderable(), pgFmt, dst);
      if(emsg == null) System.out.println("Figure exported to " + dst.getAbsolutePath());
      else System.out.println(emsg);
   }

   
   /** The singleton instance. */
   private static PDFSupport singleton = null;
    
   /** Private constuctor. Use {@link #getInstance()}. */
   private PDFSupport() 
   {
      // initialize font mapper: load all supported fonts found in the typical OS-specific installation directories.
      fontMapper = new SubstFontMapper();
   }

   /** 
    * Maps Java AWT fonts to best-match physical fonts successfully loaded from host system. Only Type1, TrueType or 
    * OpenType fonts that permit font-embedding will be supported. If an AWT font does not have a matching supported
    * PDF font, the mapper returns a substitute font rather than throwing an exception.
    */
   private SubstFontMapper fontMapper = null;
    
   private class SubstFontMapper extends DefaultFontMapper
   {
      /**
       * Construct the font mapper and pre-load it with any physical fonts located in host OS-specific font file 
       * installation directories. This merely maps font names to the font files; it does not actually load the 
       * physical fonts. 
       */
      SubstFontMapper() 
      { 
         List<File> fontDirs = new ArrayList<File>();
         if(Utilities.isWindows())
         {
            String windir = System.getenv("windir");
            String fileSep = System.getProperty("file.separator");
            if(windir != null && fileSep != null)
            {
               fontDirs.add(new File(System.getProperty("java.home") + fileSep + "lib" + fileSep + "fonts"));
               fontDirs.add(new File(windir + fileSep + "fonts"));
            }
         }
         else if(Utilities.isMacOS())
         {
            fontDirs.add(new File(System.getProperty("java.home") + "/lib/fonts"));
            fontDirs.add(new File("/Library/Fonts"));
            fontDirs.add(new File("/System/Library/Fonts"));
            fontDirs.add(new File("/System/Library/Fonts/Supplemental"));
         }
         else
         {
            fontDirs.add(new File(System.getProperty("java.home") + "/lib/fonts"));
            fontDirs.add(new File("/usr/share/X11/fonts"));
            fontDirs.add(new File("/usr/X/lib/X11/fonts"));
            fontDirs.add(new File("/usr/openwin/lib/X11/fonts"));
            fontDirs.add(new File("/usr/share/fonts"));
            fontDirs.add(new File("/usr/X11R6/lib/X11/fonts"));
         }
       
         for(File dir : fontDirs) if(dir != null && dir.isDirectory())
         {
            File files[] = dir.listFiles();
            for(File f : files) insertFile(f);
         }
      }

      /**
       * Overridden so that it returns a substitute font whenever the physical PDF font file that matches the AWT font 
       * is not loaded successfully, has a license restriction that prohibits font embedding, has a font-specific
       * character encoding (which the iText library cannot handle; Webdings is an example), or cannot render all 95 
       * printable ASCII characters in the Unicode range 0x20-0x7E.
       * 
       * <p>In addition, we try the <b>BaseFont.IDENTITY_H</b> encoding for any TrueType fonts created so that we can 
       * access any character glyph available in the font file. However, many of the TrueType Unicode-compatible fonts 
       * that IText "successfully" loads are tagged as "direct-text-to-byte". These seem to be missing (or IText cannot
       * read it) an explicit character map, referred to as "cmap31" in iText's implementation of a TrueTypeFont, which 
       * maps a Unicode character code point to the corresponding glyph index. Without this, IText cannot use the font 
       * in a PDF document.</p>
       * 
       * <p>During testing in Nov 2022, I found that, by explicitly creating the "direct-text-to-byte" font with the 
       * character encoding <b>BaseFont.CP1252</b> instead of <b>BaseFont.IDENTITY_H</b>, the resulting PDF font could 
       * be used in a PDF document and still covered over 60% of the Unicode characters officially supported by FC. 
       * Thus, if a font created with the <b>IDENTITY_H</b> encoding is "direct-text-to-byte", we'll try again to create
       * it with the CP1252 encoding.</p>
       * 
       * <p>If a matching PDF font cannot be found that passes the tests described above, a substitute font (one of the
       * 14 built-in fonts) is provided (and the problem font is removed from the underlying AWT-to-PDF font map).</p>
       * 
       * <p><b>Dealing with fake italic AWT fonts.</b> See helper method fixDerivedItalicFace() for details.</p>
       * 
       * @param font The AWT font. If null, method returns the built-in Type1 Helvetica font in CP1252 encoding.
       */
      @Override public BaseFont awtToPdf(Font font)
      {
         // if no AWT font provided, then use default Type 1 Helvetica font
         if(font == null)
         {
            BaseFont bf = null;
            try { bf = BaseFont.createFont(); } catch(Exception e) { throw new NeverOccursException(e); }
            return(bf);
         }
         
         BaseFont bf = null;
         try 
         {
            BaseFontParameters p = getBaseFontParameters( fixDerivedFaceName(font) );
            if(p != null) bf = BaseFont.createFont(p.fontName, BaseFont.IDENTITY_H, p.embedded, BaseFont.NOT_CACHED, 
                  p.ttfAfm, p.pfb);
            
            // we can't handle fonts with font-specific encoding
            if(bf != null && bf.isFontSpecific())
               bf = null;
            
            // try CP1252 encoding with Unicode TT fonts that are direct text to byte, which IText can't handle
            if((bf != null) && bf.isDirectTextToByte())
               bf = BaseFont.createFont(p.fontName, BaseFont.CP1252, p.embedded, BaseFont.NOT_CACHED, p.ttfAfm, p.pfb);
            
            // finally, require that loaded font be able to render all printable ASCII characters
            if(bf != null)
            {
               boolean valid = true;
               for(int c = 0x20; valid && c <= 0x7E; c++) valid = bf.charExists(c);
               
               if(!valid)
                  bf = null;
            } 
         }
         catch(Exception e) { bf = null; }
          
         // if no matching PDF font found that satisfies our limitations, then drop the AWT font from our font mapper
         // (which is populated at startup), then provide a substitute built-in PDF font (and remember that the AWT 
         // font was assigned to a substitute PDF font).
         if(bf == null)
         {
            getMapper().remove(font.getFontName());
            bf = getSubstituteBuiltInFont(font);
            substitutedFonts.add(font.getFontName());
         }
         
         return(bf);
      }

      /**
       * Helper method for <b>awtToPdf()</b>.
       * 
       * <p>The AWT font infrastructure will synthesize an italic or bold-italic font if it cannot find a physical font 
       * face within the specified font family that implements the italic or bold-italic style. In some cases, AWT fails
       * to find the suitable font face b/c the font face name lacks the typical keywords ('bold', 'italic', 'inclined',
       * etc). In other cases, the font family simply lacks style variants. When AWT synthesizes an italic variant (bold
       * variants are NOT synthesized), the font face name has the word 'Derived' in it. Obviously, since this does not 
       * correspond to any real physical font, <p>awtToPdf()</b> will substitute it with a built-in physical font.</p>
       * 
       * <p>If the specified font's face name (<b>getFontName()</b>) lacks the word 'Derived', that face name is
       * returned unchanged. Otherwise:
       * <ol>
       * <li>Get all installed variants for the AWT font family. If none found, return the face name unchanged.</li>
       * <li>Examine the names of the variants (and their aliases) for the keywords that indicate style, and return the
       * variant name that best matches the font style (either italic or bold-italic).</li>
       * </ol>
       * It is important to examine the aliases (made available through the underlying iText library) -- which AWT
       * apparently does not do -- as these often contain the critical keywords.</p>
       * 
       * @param awtFont The AWT font.
       * @return The physical font face name to use, determined in the manner described.
       */
      private String fixDerivedFaceName(Font awtFont)
      {
         // we only change face name when font is italic (or bolditalic) and 'Derived" is in the original font face name
         String faceName = awtFont.getFontName();
         if(!(awtFont.isItalic() && faceName.contains("Derived"))) return faceName;

         Font[] variants = LocalFontEnvironment.getInstalledVariants(awtFont.getFamily());
         if(variants == null) return faceName;
         if(variants.length == 1) return variants[0].getFontName();

            
         HashMap<String, String> aliases = fontMapper.getAliases();
         List<String> italicFaces = new ArrayList<String>();
         for(Font f : variants)
         {
             faceName = f.getFontName();
             if(LocalFontEnvironment.isItalicFace(faceName) || LocalFontEnvironment.isItalicFace(aliases.get(faceName)))
                italicFaces.add(faceName);
         }

         if(italicFaces.size() == 0)
            return variants[0].getFontName();
         else if(!awtFont.isBold())
            return italicFaces.get(0);
         else
         {
            for(String face : italicFaces)
               if(LocalFontEnvironment.isBoldFace(face))
                  return face;
            return italicFaces.get(0);
         }
      }
      
      @SuppressWarnings("unused")
      boolean isMapped(Font font) 
      { 
         BaseFontParameters bfp = (font == null) ? null : getBaseFontParameters(font.getFontName());
         return(bfp != null);
      }

      int getNumFontsMapped() { return(getMapper().size()); }
      
      /**
       * If the specified AWT font could not be a mapped to a supported physical font, {@link #awtToPdf(Font)} will
       * automatically provide a substitute font. Call this method to check whether or not a substitute was provided for
       * the specified AWT font. If a physical font has not yet been requested for the given AWT font, then the method
       * will return false. The intent is to use this method to check whether or not a constructed PDF used any such
       * substitute fonts.
       * 
       * @param font An AWT font.
       * @return
       */
      boolean wasSubstituted(Font font) { return(font != null && substitutedFonts.contains(font.getFontName())); }
      
      /**
       * Get the built-in Type 1 font that can substitute for the specified AWT font.
       * @param font An AWT font.
       * @return The built-in font that best matches (style-wise) the AWT font.
       */
      private BaseFont getSubstituteBuiltInFont(Font font)
      {
         if(font == null) return(null);
         
         // handle logical font names, plus any fonts that were not mapped. We use one of the built-in standard fonts,
         // so this should always work. Finally, if the ever-present Symbol font was requested but was not mapped to a
         // physical font, use the built-in Type1 Symbol font
         String fontKey = null;
         String name = font.getName().toLowerCase(Locale.ENGLISH);
         boolean isItalic = font.isItalic();
         boolean isBold = font.isBold();
         boolean useMono = name.equals("dialoginput") || name.equals("monospaced");
         boolean useSerif = name.equals("serif");
         boolean useSansSerif = name.equals("dialog") || name.equals("sansserif");
         if(!(useMono || useSerif || useSansSerif))
         {
            // an unmapped font that is not a logical font. Check for key words in font name that indicate font style, 
            // since isBold() and isItalic() may not work for font face variants. Use a sans-serif substitute unless the
            // font name contains keywords that suggest a monospaced or serif font.
            name = font.getFontName().toLowerCase(Locale.ENGLISH);
            isItalic = isItalic || name.indexOf("italic") >= 0 || name.indexOf("oblique") >= 0 ||
                  name.indexOf("inclined") >= 0;
            isBold = isBold || name.indexOf("bold") >= 0;
            
            if(name.indexOf("courier") >= 0) useMono = true;
            else if(name.indexOf("times") >= 0) useSerif = true;
            else useSansSerif = true;
         }

         if(useMono) 
         {
            if(isItalic) fontKey = isBold ? BaseFont.COURIER_BOLDOBLIQUE : BaseFont.COURIER_OBLIQUE;
            else fontKey = isBold ? BaseFont.COURIER_BOLD : BaseFont.COURIER;
         } 
         else if(useSerif) 
         {
            if(isItalic) fontKey = isBold ? BaseFont.TIMES_BOLDITALIC : BaseFont.TIMES_ITALIC;
            else fontKey = isBold ? BaseFont.TIMES_BOLD : BaseFont.TIMES_ROMAN;
         } 
         else
         {
            if(isItalic) fontKey = isBold ? BaseFont.HELVETICA_BOLDOBLIQUE : BaseFont.HELVETICA_OBLIQUE;
            else fontKey = isBold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA;
         }
          
         // special case: Symbol font. This will have a different encoding than the other substitute fonts
         if(font.getFontName().toLowerCase(Locale.ENGLISH).equals("symbol"))
            fontKey = BaseFont.SYMBOL;
         
         BaseFont bf = null;
         try 
         { 
            bf = BaseFont.createFont(fontKey, fontKey==BaseFont.SYMBOL ? "symbol" : BaseFont.CP1252, false); 
         }
         catch(Exception e) { throw new NeverOccursException(e); }
         
         return(bf);
      }
      
      /** 
       * Get the built-in Type 1 Adobe Symbol font.
       * @return The Symbol font.
       */
      @SuppressWarnings("unused")
      private BaseFont getBuiltInSymbolFont()
      {
         BaseFont bf = null;
         try {  bf = BaseFont.createFont(BaseFont.SYMBOL, "symbol", false); }
         catch(Exception e) { throw new NeverOccursException(e); }
         return(bf);
      }
      
      /** A substitute font was provided for any AWT font name found in this hash. */
      private HashSet<String> substitutedFonts = new HashSet<String>();
   }
}
