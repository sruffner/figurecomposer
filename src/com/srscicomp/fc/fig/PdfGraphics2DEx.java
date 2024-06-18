package com.srscicomp.fc.fig;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.awt.geom.PolylineShape;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ByteBuffer;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfPatternPainter;
import com.itextpdf.text.pdf.PdfShading;
import com.itextpdf.text.pdf.PdfShadingPattern;

/**
 * A modified version of the <i>ITextPDF</i> class <b>PdfGraphics2D</b> that addresses several shortcomings in text
 * text rendering.
 * <ul>
 * <li>Added support for the {@link TextAttribute#SUPERSCRIPT} attribute in {@link 
 * #drawString(AttributedCharacterIterator, float, float)}.</li>
 * <li>Disabled simulation of bold or italic style when the system lacks an installed bold or italic variant. In some
 * cases the simulation leads to unreadable output, so I decided to just disable simulation altogether. It is the user's
 * responsibility to select fonts that include the necessary style variants.</li>
 * <li><i>Use AWT glyphs instead of doing font substitution.</i> Most PDF fonts -- as implemented by <i>ITextPDF</i> 
 * class {@link BaseFont} -- do not have a glyph for every character in FigureComposer's supported character set. That 
 * class also lacks the sophistication built into Java's AWT fonts, which can display a great many more characters 
 * because (i) they're better at reading the physical font file and locating all available character glyphs, and (ii) 
 * they're often composite fonts under the hood, with logical fallback fonts to cover additional Unicode characters not 
 * covered by the primary font. FigureComposer's PDF support dealt with the "missing glyphs" issue by implementing "font
 * substitution", but this has never worked will. An alternative is to draw and fill the glyph vector for supplied by the
 * AWT font. This makes the PDF larger because you're inserting low-level drawing commands for each character rendered
 * in this manner, but it results in PDFs that generally look more like what is rendered onscreen in FC. It also may
 * increase the time it takes to generate the PDF, particularly if it contains a lot of text.</li>
 * </ul>
 * 
 * <p>NOTE: I was unable to subclass {@link com.itextpdf.awt.PdfGraphics2D} because the create() method in that class 
 * explicitly creates a PdfGraphics2D object, and I can't override that method bc it initializes a bunch of private 
 * class members.</p>
 * 
 * @author sruffner
 */
public class PdfGraphics2DEx extends Graphics2D {

    private static final int FILL = 1;
    private static final int STROKE = 2;
    private static final int CLIP = 3;
    private BasicStroke strokeOne = new BasicStroke(1);

    private static final AffineTransform IDENTITY = new AffineTransform();

    protected Font font;
    protected BaseFont baseFont;
    protected float fontSize;
    protected AffineTransform transform;
    protected Paint paint;
    protected Color background;
    protected float width;
    protected float height;

    protected Area clip;

    protected final RenderingHints rhints = new RenderingHints(null);

    protected Stroke stroke;
    protected Stroke originalStroke;

    protected PdfContentByte cb;

    /** Storage for BaseFont objects created. */
    protected final HashMap<String, BaseFont> baseFonts = new HashMap<>();

    protected boolean disposeCalled = false;

    protected FontMapper fontMapper;

    private static final class Kid {
        final int pos;
        final PdfGraphics2DEx graphics;
        Kid(int pos, PdfGraphics2DEx graphics) {
            this.pos = pos;
            this.graphics = graphics;
        }
    }
    private ArrayList<Kid> kids;

    private boolean kid = false;

    private Graphics2D dg2;

    private Stroke oldStroke;
    private Paint paintFill;
    private Paint paintStroke;

    private MediaTracker mediaTracker;

    // Added by Jurij Bilas
    protected boolean underline;          // indicates if the font style is underlined
    // Added by Peter Severin
    /** @since 5.0.3 */
    protected boolean strikethrough;

    protected PdfGState[] fillGState;
    protected PdfGState[] strokeGState;
    protected int currentFillGState = 255;
    protected int currentStrokeGState = 255;

    public static final int AFM_DIVISOR = 1000; // used to calculate coordinates

    private boolean convertImagesToJPEG = false;
    private float jpegQuality = .95f;

   // Added by Alexej Suchov
   private float alpha;

   // Added by Alexej Suchov
   private Composite composite;

   // Added by Alexej Suchov
   private Paint realPaint;
   
   /**
    * Method that creates a Graphics2D object.
    * Contributed by Peter Harvey: he moved code from the constructor to a separate method
    * @since 5.0.2
    */
   private Graphics2D getDG2() {
      if (dg2 == null) {
         dg2 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB).createGraphics();      
         dg2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         setRenderingHint(HyperLinkKey.KEY_INSTANCE, HyperLinkKey.VALUE_HYPERLINKKEY_OFF);
      }
      return dg2;
   }
   
   private PdfGraphics2DEx() {}
   
    public PdfGraphics2DEx(PdfContentByte cb, final float width, final float height) {
      this(cb, width, height, null, false, 0);
    }

    public PdfGraphics2DEx(PdfContentByte cb, final float width, final float height, final FontMapper fontMapper) {
      this(cb, width, height, fontMapper, false, 0);
    }

    
    /**
     * Constructor for PDFGraphics2DEx.
     */
    public PdfGraphics2DEx(PdfContentByte cb, float width, float height, FontMapper fontMapper, boolean convertImagesToJPEG, float quality) {
        super();
        this.fillGState = new PdfGState[256];
        this.strokeGState = new PdfGState[256];
        this.convertImagesToJPEG = convertImagesToJPEG;
        this.jpegQuality = quality;
        this.transform = new AffineTransform();
        this.fontMapper = (fontMapper == null) ? new DefaultFontMapper() : fontMapper;

        paint = Color.black;
        background = Color.white;
        setFont(new Font("sanserif", Font.PLAIN, 12));
        this.cb = cb;
        cb.saveState();
        this.width = width;
        this.height = height;
        clip = new Area(new Rectangle2D.Float(0, 0, width, height));
        clip(clip);
        originalStroke = stroke = oldStroke = strokeOne;
        setStrokeDiff(stroke, null);
        cb.saveState();
    }

    /**
     * @see Graphics2D#draw(Shape)
     */
    @Override
    public void draw(Shape s) {
        followPath(s, STROKE);
    }

    /**
     * @see Graphics2D#drawImage(Image, AffineTransform, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return drawImage(img, null, xform, null, obs);
    }

    /**
     * @see Graphics2D#drawImage(BufferedImage, BufferedImageOp, int, int)
     */
    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        BufferedImage result = img;
        if (op != null) {
            result = op.createCompatibleDestImage(img, img.getColorModel());
            result = op.filter(img, result);
        }
        drawImage(result, x, y, null);
    }

    /**
     * @see Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)
     */
    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        BufferedImage image;
        if (img instanceof BufferedImage) {
            image = (BufferedImage)img;
        } else {
            ColorModel cm = img.getColorModel();
            int width = img.getWidth();
            int height = img.getHeight();
            WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            Hashtable<String, Object> properties = new Hashtable<>();
            String[] keys = img.getPropertyNames();
            if (keys!=null) {
                for (String key : keys) {
                    properties.put(key, img.getProperty(key));
                }
            }
            BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
            img.copyData(raster);
            image=result;
        }
        drawImage(image, xform, null);
    }

    /**
     * @see Graphics2D#drawRenderableImage(RenderableImage, AffineTransform)
     */
    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        drawRenderedImage(img.createDefaultRendering(), xform);
    }

    /**
     * @see Graphics#drawString(String, int, int)
     */
    @Override
    public void drawString(String s, int x, int y) {
        drawString(s, (float)x, (float)y);
    }

    /**
     * Calculates position and/or stroke thickness depending on the font size
     * @param d value to be converted
     * @param i font size
     * @return position and/or stroke thickness depending on the font size
     */
    public static double asPoints(double d, int i) {
        return d * i / AFM_DIVISOR;
    }
    /**
     * This routine goes through the attributes and sets the font
     * before calling the actual string drawing routine
     * @param iter Attributed character iterator.
     */
    @SuppressWarnings("unchecked")
    protected void doAttributes(AttributedCharacterIterator iter) {
        underline = false;
        strikethrough = false;
        for (AttributedCharacterIterator.Attribute attribute: iter.getAttributes().keySet()) {
            if (!(attribute instanceof TextAttribute))
                continue;
            TextAttribute textattribute = (TextAttribute)attribute;
            if(textattribute.equals(TextAttribute.FONT)) {
                Font font = (Font)iter.getAttributes().get(textattribute);
                setFont(font);
            }
            else if(textattribute.equals(TextAttribute.UNDERLINE)) {
                if(iter.getAttributes().get(textattribute) == TextAttribute.UNDERLINE_ON)
                    underline = true;
            }
            else if(textattribute.equals(TextAttribute.STRIKETHROUGH)) {
               if(iter.getAttributes().get(textattribute) == TextAttribute.STRIKETHROUGH_ON)
                  strikethrough = true;
            }
            else if(textattribute.equals(TextAttribute.SIZE)) {
                Object obj = iter.getAttributes().get(textattribute);
                if(obj instanceof Integer) {
                    int i = (Integer) obj;
                    setFont(getFont().deriveFont(getFont().getStyle(), i));
                }
                else if(obj instanceof Float) {
                    float f = (Float) obj;
                    setFont(getFont().deriveFont(getFont().getStyle(), f));
                }
            }
            else if(textattribute.equals(TextAttribute.FOREGROUND)) {
                setColor((Color) iter.getAttributes().get(textattribute));
            }
            else if(textattribute.equals(TextAttribute.FAMILY)) {
              Font font = getFont();
              Map<TextAttribute, Object> fontAttributes = (Map<TextAttribute, Object>) font.getAttributes();
              fontAttributes.put(TextAttribute.FAMILY, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
            else if(textattribute.equals(TextAttribute.POSTURE)) {
              Font font = getFont();
              Map<TextAttribute, Object> fontAttributes = (Map<TextAttribute, Object>) font.getAttributes();
              fontAttributes.put(TextAttribute.POSTURE, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
            else if(textattribute.equals(TextAttribute.WEIGHT)) {
              Font font = getFont();
              Map<TextAttribute, Object> fontAttributes = (Map<TextAttribute, Object>) font.getAttributes();
              fontAttributes.put(TextAttribute.WEIGHT, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
        }
    }

    /**
     * Modified from the original <b>PdfGraphics2D</b> implementation:
     * <ol>
     * <li>Disabled simulation of bold and italic fonts. Output doesn't look good for some fonts, sometimes even
     * unreadable.</li>
     * <li>If ANY character in the specified string has no corresponding glyphy in the PDF <b>BaseFont</b>, then the
     * string is drawn using the AWT font's glyph vectors instead. Typical AWT fonts are composite fonts under the hood 
     * that have logical fallback fonts to cover more Unicode characters than a single physical font.</li>
     * </ol>
     */
    @Override public void drawString(String s, float x, float y) 
    {
       if(s.isEmpty())
          return;
       setFillPaint();
        
       // if the BaseFont can't do any ONE of the characters in the string, use AWT font's glyphs instead for the
       // entire string.
       boolean useGlyphs = false;
       for(int i=0; (!useGlyphs) && i<s.length(); i++)
          useGlyphs = !baseFont.charExists(s.charAt(i));
           
       if(useGlyphs)
       {
          drawGlyphVector(this.font.layoutGlyphVector(getFontRenderContext(), s.toCharArray(), 0, s.length(), 
                Font.LAYOUT_LEFT_TO_RIGHT), x, y);
          // Use the following line to compile in JDK 1.3
          // drawGlyphVector(this.font.createGlyphVector(getFontRenderContext(), s), x, y);
       }
       else 
       {
          boolean restoreTextRenderingMode = false;
          AffineTransform at = getTransform();
          AffineTransform at2 = getTransform();
          at2.translate(x, y);
          at2.concatenate(font.getTransform());
          setTransform(at2);
          AffineTransform inverse = this.normalizeMatrix();
          AffineTransform flipper = AffineTransform.getScaleInstance(1,-1);
          inverse.concatenate(flipper);
          double[] mx = new double[6];
          inverse.getMatrix(mx);
          cb.beginText();
          cb.setFontAndSize(baseFont, fontSize);

          cb.setTextMatrix((float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]);
          Float fontTextAttributeWidth = (Float)font.getAttributes().get(TextAttribute.WIDTH);
          fontTextAttributeWidth = 
                (fontTextAttributeWidth == null) ? TextAttribute.WIDTH_REGULAR : fontTextAttributeWidth;
          if(!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth))
             cb.setHorizontalScaling(100.0f / fontTextAttributeWidth);

          double width = 0;
          if(font.getSize2D() > 0)
          {
             float scale = 1000 / font.getSize2D();
             Font derivedFont = font.deriveFont(AffineTransform.getScaleInstance(scale, scale));
             width = derivedFont.getStringBounds(s, getFontRenderContext()).getWidth();
             if(derivedFont.isTransformed())
                width /= scale;
          }
          // if the hyperlink flag is set add an action to the text
          Object url = getRenderingHint(HyperLinkKey.KEY_INSTANCE);
          if(url != null && !url.equals(HyperLinkKey.VALUE_HYPERLINKKEY_OFF))
          {
             float scale = 1000 / font.getSize2D();
             Font derivedFont = font.deriveFont(AffineTransform.getScaleInstance(scale, scale));
             double height = derivedFont.getStringBounds(s, getFontRenderContext()).getHeight();
             if(derivedFont.isTransformed())
                height /= scale;
             double leftX = cb.getXTLM();
             double leftY = cb.getYTLM();
             PdfAction action = new PdfAction(url.toString());
             cb.setAction(action, (float)leftX, (float)leftY, (float)(leftX+width), (float)(leftY+height));
          }
          if(s.length() > 1) 
          {
             float adv = ((float)width - baseFont.getWidthPoint(s, fontSize)) / (s.length() - 1);
             cb.setCharacterSpacing(adv);
          }
          cb.showText(s);
          if (s.length() > 1)
          {
             cb.setCharacterSpacing(0);
          }
          if(!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth))
             cb.setHorizontalScaling(100);

          cb.endText();
          setTransform(at);
          if(underline)
          {
              // These two are supposed to be taken from the .AFM file
              //int UnderlinePosition = -100;
              int UnderlineThickness = 50;
              //
              double d = asPoints(UnderlineThickness, (int)fontSize);
              Stroke savedStroke = originalStroke;
              setStroke(new BasicStroke((float)d));
              y = (float)(y + asPoints(UnderlineThickness, (int)fontSize));
              Line2D line = new Line2D.Double(x, y, width+x, y);
              draw(line);
              setStroke(savedStroke);
          }
          if(strikethrough)
          {
             // These two are supposed to be taken from the .AFM file
             int StrikethroughThickness = 50;
             int StrikethroughPosition = 350;
             //
             double d = asPoints(StrikethroughThickness, (int)fontSize);
             double p = asPoints(StrikethroughPosition, (int)fontSize);
             Stroke savedStroke = originalStroke;
             setStroke(new BasicStroke((float)d));
             y = (float)(y + asPoints(StrikethroughThickness, (int)fontSize));
             Line2D line = new Line2D.Double(x, y-p, width+x, y-p);
             draw(line);
             setStroke(savedStroke);
          }
       }
    }

    /**
     * @see Graphics#drawString(AttributedCharacterIterator, int, int)
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float)x, (float)y);
    }


    /**
     * Modified from original <b>PdfGraphics2D</b> implementation to support superscripted text. 
     * 
     */
    @Override public void drawString(AttributedCharacterIterator iter, float x, float y)
    {
       boolean isSub = false;
       boolean isSuper = false;
       StringBuilder stringbuffer = new StringBuilder(iter.getEndIndex());
       for(char c = iter.first(); c != '\uFFFF'; c = iter.next())
       {
          if(iter.getIndex() == iter.getRunStart())
          {
             if(stringbuffer.length() > 0)
             {
                // before drawing the string, adjust font size and vertical text position if superscript or subscript
                // is in effect.
                float yDelta = 0f;
                Font currFont = getFont();
                if(isSub || isSuper)
                {
                   yDelta = (isSub ? 0.33f : -0.40f) * currFont.getSize2D();
                   setFont(currFont.deriveFont(currFont.getStyle(), currFont.getSize2D() * 0.58f));
                }
                

                drawString(stringbuffer.toString(), x, y+yDelta);

                FontMetrics fontmetrics = getFontMetrics();
                x = (float)(x + fontmetrics.getStringBounds(stringbuffer.toString(), this).getWidth());
                stringbuffer.delete(0, stringbuffer.length());
                
                // after drawing string, restore the nominal font if we adjusted it for superscript or subscript run
                if(isSub || isSuper)
                   setFont(currFont);
             }
             doAttributes(iter);
             
             // check to see if next text run is superscript or subscript
             isSuper = isSub = false;
             for (AttributedCharacterIterator.Attribute attribute: iter.getAttributes().keySet())
             {
                if (!(attribute instanceof TextAttribute))
                   continue;
                TextAttribute textAttr = (TextAttribute)attribute;
                if(textAttr.equals(TextAttribute.SUPERSCRIPT))
                {
                   Integer value = (Integer) (iter.getAttributes().get(textAttr));
                   if(value.equals(TextAttribute.SUPERSCRIPT_SUPER))
                      isSuper = true;
                   else if(value.equals(TextAttribute.SUPERSCRIPT_SUB))
                      isSub = true;
                }
             }
          }
          stringbuffer.append(c);
       }

       // handle the final like-styled character sequence in the same manner as the previous ones
       if(stringbuffer.length() > 0)
       {
          float yDelta = 0f;
          Font currFont = getFont();
          if(isSub || isSuper)
          {
             yDelta = (isSub ? 0.33f : -0.40f) * currFont.getSize2D();
             setFont(currFont.deriveFont(currFont.getStyle(), currFont.getSize2D() * 0.58f));
          }
          
          drawString(stringbuffer.toString(), x, y+yDelta);
          
          if(isSub || isSuper)
             setFont(currFont);
       }
       
       underline = false;
       strikethrough = false;
    }

    /**
     * @see Graphics2D#drawGlyphVector(GlyphVector, float, float)
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        Shape s = g.getOutline(x, y);
        fill(s);
    }

    /**
     * @see Graphics2D#fill(Shape)
     */
    @Override
    public void fill(Shape s) {
        followPath(s, FILL);
    }

    /**
     * @see Graphics2D#hit(Rectangle, Shape, boolean)
     */
    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        if (onStroke) {
            s = stroke.createStrokedShape(s);
        }
        s = transform.createTransformedShape(s);
        Area area = new Area(s);
        if (clip != null)
            area.intersect(clip);
        return area.intersects(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @see Graphics2D#getDeviceConfiguration()
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return getDG2().getDeviceConfiguration();
    }

    /**
    * Method contributed by Alexej Suchov
     * @see Graphics2D#setComposite(Composite)
     */
    @Override
    public void setComposite(Composite comp) {

      if (comp instanceof AlphaComposite) {

         AlphaComposite composite = (AlphaComposite) comp;

         if (composite.getRule() == 3) {

            alpha = composite.getAlpha();
            this.composite = composite;

            if (realPaint != null && realPaint instanceof Color) {

               Color c = (Color) realPaint;
               paint = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                     (int) (c.getAlpha() * alpha));
            }
            return;
         }
      }

      this.composite = comp;
      alpha = 1.0F;

    }

    /**
    * Method contributed by Alexej Suchov
     * @see Graphics2D#setPaint(Paint)
     */
    @Override
    public void setPaint(Paint paint) {
        if (paint == null)
            return;
        this.paint = paint;
      realPaint = paint;

      if (composite instanceof AlphaComposite && paint instanceof Color) {

         AlphaComposite co = (AlphaComposite) composite;

         if (co.getRule() == 3) {
            Color c = (Color) paint;
            this.paint = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * alpha));
            realPaint = paint;
         }
      }

    }

    private Stroke transformStroke(Stroke stroke) {
        if (!(stroke instanceof BasicStroke))
            return stroke;
        BasicStroke st = (BasicStroke)stroke;
        float scale = (float)Math.sqrt(Math.abs(transform.getDeterminant()));
        float[] dash = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k)
                dash[k] *= scale;
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase() * scale);
    }

    private void setStrokeDiff(Stroke newStroke, Stroke oldStroke) {
        if (newStroke == oldStroke)
            return;
        if (!(newStroke instanceof BasicStroke))
            return;
        BasicStroke nStroke = (BasicStroke)newStroke;
        boolean oldOk = oldStroke instanceof BasicStroke;
        BasicStroke oStroke = null;
        if (oldOk)
            oStroke = (BasicStroke)oldStroke;
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth())
            cb.setLineWidth(nStroke.getLineWidth());
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                cb.setLineCap(0);
                break;
            case BasicStroke.CAP_SQUARE:
                cb.setLineCap(2);
                break;
            default:
                cb.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                cb.setLineJoin(0);
                break;
            case BasicStroke.JOIN_BEVEL:
                cb.setLineJoin(2);
                break;
            default:
                cb.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit())
            cb.setMiterLimit(nStroke.getMiterLimit());
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                }
                else makeDash = !java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray());
            }
            else makeDash = oStroke.getDashArray() != null;
        }
        else {
            makeDash = true;
        }
        if (makeDash) {
            float[] dash = nStroke.getDashArray();
            if (dash == null)
                cb.setLiteral("[]0 d\n");
            else
            {
                cb.setLiteral('[');
                for(float v : dash)
                {
                   cb.setLiteral(v);
                   cb.setLiteral(' ');
                }
                cb.setLiteral(']');
                cb.setLiteral(nStroke.getDashPhase());
                cb.setLiteral(" d\n");
            }
        }
    }

    /**
     * @see Graphics2D#setStroke(Stroke)
     */
    @Override
    public void setStroke(Stroke s) {
        originalStroke = s;
        this.stroke = transformStroke(s);
    }


    /**
     * Sets a rendering hint
     * @param arg0 Rendering hint key.
     * @param arg1 Rendering hint value.
     */
    @Override
    public void setRenderingHint(Key arg0, Object arg1) {
       if (arg1 != null) {
            rhints.put(arg0, arg1);
         } else {
          if (arg0 instanceof HyperLinkKey)
          {
             rhints.put(arg0, HyperLinkKey.VALUE_HYPERLINKKEY_OFF);
          }
          else
          {
             rhints.remove(arg0);
          }
         }
    }

    /**
     * @param arg0 a key
     * @return the rendering hint
     */
    @Override
    public Object getRenderingHint(Key arg0) {
        return rhints.get(arg0);
    }

    /**
     * @see Graphics2D#setRenderingHints(Map)
     */
    @Override
    public void setRenderingHints(Map<?,?> hints) {
        rhints.clear();
        rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#addRenderingHints(Map)
     */
    @Override
    public void addRenderingHints(Map<?,?> hints) {
        rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#getRenderingHints()
     */
    @Override
    public RenderingHints getRenderingHints() {
        return rhints;
    }

    /**
     * @see Graphics#translate(int, int)
     */
    @Override
    public void translate(int x, int y) {
        translate(x, (double)y);
    }

    /**
     * @see Graphics2D#translate(double, double)
     */
    @Override
    public void translate(double tx, double ty) {
        transform.translate(tx,ty);
    }

    /**
     * @see Graphics2D#rotate(double)
     */
    @Override
    public void rotate(double theta) {
        transform.rotate(theta);
    }

    /**
     * @see Graphics2D#rotate(double, double, double)
     */
    @Override
    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    /**
     * @see Graphics2D#scale(double, double)
     */
    @Override
    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#shear(double, double)
     */
    @Override
    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    /**
     * @see Graphics2D#transform(AffineTransform)
     */
    @Override
    public void transform(AffineTransform tx) {
        transform.concatenate(tx);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#setTransform(AffineTransform)
     */
    @Override
    public void setTransform(AffineTransform t) {
        transform = new AffineTransform(t);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#getTransform()
     */
    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    /**
    * Method contributed by Alexej Suchov
     * @see Graphics2D#getPaint()
     */
    @Override
    public Paint getPaint() {
        if (realPaint != null) {
            return realPaint;
        } else {
            return paint;
        }
   }

    /**
     * @see Graphics2D#getComposite()
     */
    @Override
    public Composite getComposite() {
        return composite;
    }

    /**
     * @see Graphics2D#setBackground(Color)
     */
    @Override
    public void setBackground(Color color) {
        background = color;
    }

    /**
     * @see Graphics2D#getBackground()
     */
    @Override
    public Color getBackground() {
        return background;
    }

    /**
     * @see Graphics2D#getStroke()
     */
    @Override
    public Stroke getStroke() {
        return originalStroke;
    }


    /**
     * @see Graphics2D#getFontRenderContext()
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        boolean antialias = RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        boolean fractions = RenderingHints.VALUE_FRACTIONALMETRICS_ON.equals(getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
        return new FontRenderContext(new AffineTransform(), antialias, fractions);
    }

    /**
     * @see Graphics#create()
     */
    @Override
    public Graphics create() {
        PdfGraphics2DEx g2 = new PdfGraphics2DEx();
        g2.rhints.putAll( this.rhints );
        g2.transform = new AffineTransform(this.transform);
        g2.baseFonts.putAll(this.baseFonts);
        g2.fontMapper = this.fontMapper;
        g2.paint = this.paint;
        g2.fillGState = this.fillGState;
        g2.currentFillGState = this.currentFillGState;
        g2.strokeGState = this.strokeGState;
        g2.background = this.background;
        g2.mediaTracker = this.mediaTracker;
        g2.convertImagesToJPEG = this.convertImagesToJPEG;
        g2.jpegQuality = this.jpegQuality;
        g2.setFont(this.font);
        g2.cb = this.cb.getDuplicate();
        g2.cb.saveState();
        g2.width = this.width;
        g2.height = this.height;
        g2.followPath(new Area(new Rectangle2D.Float(0, 0, width, height)), CLIP);
        if (this.clip != null)
            g2.clip = new Area(this.clip);
        g2.composite = composite;
        g2.stroke = stroke;
        g2.originalStroke = originalStroke;
        g2.strokeOne = (BasicStroke)g2.transformStroke(g2.strokeOne);
        g2.oldStroke = g2.strokeOne;
        g2.setStrokeDiff(g2.oldStroke, null);
        g2.cb.saveState();
        if (g2.clip != null)
            g2.followPath(g2.clip, CLIP);
        g2.kid = true;
        if (this.kids == null)
            this.kids = new ArrayList<>();
        this.kids.add(new Kid(cb.getInternalBuffer().size(), g2));
        return g2;
    }

    public PdfContentByte getContent() {
        return this.cb;
    }
    /**
     * @see Graphics#getColor()
     */
    @Override
    public Color getColor() {
        if (paint instanceof Color) {
            return (Color)paint;
        } else {
            return Color.black;
        }
    }

    /**
     * @see Graphics#setColor(Color)
     */
    @Override
    public void setColor(Color color) {
        setPaint(color);
    }

    /**
     * @see Graphics#setPaintMode()
     */
    @Override
    public void setPaintMode() {}

    /**
     * @see Graphics#setXORMode(Color)
     */
    @Override
    public void setXORMode(Color c1) {

    }

    /**
     * @see Graphics#getFont()
     */
    @Override
    public Font getFont() {
        return font;
    }

   /**
     * Sets the current font.
     */
    @Override
    public void setFont(Font f) {
        if (f == null)
            return;
        if (f == font)
            return;
        font = f;
        fontSize = f.getSize2D();
        baseFont = getCachedBaseFont(f);
    }

    private BaseFont getCachedBaseFont(Font f) {
        synchronized (baseFonts) {
            BaseFont bf = baseFonts.get(f.getFontName());
            if (bf == null) {
                bf = fontMapper.awtToPdf(f);
                baseFonts.put(f.getFontName(), bf);
            }
            return bf;
        }
    }

    /**
     * @see Graphics#getFontMetrics(Font)
     */
    @Override
    public FontMetrics getFontMetrics(Font f) {
        return getDG2().getFontMetrics(f);
    }

    /**
     * @see Graphics#getClipBounds()
     */
    @Override
    public Rectangle getClipBounds() {
        if (clip == null)
            return null;
        return getClip().getBounds();
    }

    /**
     * @see Graphics#clipRect(int, int, int, int)
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        Rectangle2D rect = new Rectangle2D.Double(x,y,width,height);
        clip(rect);
    }

    /**
     * @see Graphics#setClip(int, int, int, int)
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        Rectangle2D rect = new Rectangle2D.Double(x,y,width,height);
        setClip(rect);
    }

    /**
     * @see Graphics2D#clip(Shape)
     */
    @Override
    public void clip(Shape s) {
        if (s == null) {
            setClip(null);
            return;
        }
        s = transform.createTransformedShape(s);
        if (clip == null)
            clip = new Area(s);
        else
            clip.intersect(new Area(s));
        followPath(s, CLIP);
    }

    /**
     * @see Graphics#getClip()
     */
    @Override
    public Shape getClip() {
        try {
            return transform.createInverse().createTransformedShape(clip);
        }
        catch (NoninvertibleTransformException e) {
            return null;
        }
    }

    /**
     * @see Graphics#setClip(Shape)
     */
    @Override
    public void setClip(Shape s) {
        cb.restoreState();
        cb.saveState();
        if (s != null)
            s = transform.createTransformedShape(s);
        if (s == null) {
            clip = null;
        }
        else {
            clip = new Area(s);
            followPath(s, CLIP);
        }
        paintFill = paintStroke = null;
        currentFillGState = currentStrokeGState = -1;
        oldStroke = strokeOne;
    }

    /**
     * @see Graphics#copyArea(int, int, int, int, int, int)
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {

    }

    /**
     * @see Graphics#drawLine(int, int, int, int)
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        draw(line);
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void drawRect(int x, int y, int width, int height) {
        draw(new Rectangle(x, y, width, height));
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        fill(new Rectangle(x,y,width,height));
    }

    /**
     * @see Graphics#clearRect(int, int, int, int)
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        Paint temp = paint;
        setPaint(background);
        fillRect(x,y,width,height);
        setPaint(temp);
    }

    /**
     * @see Graphics#drawRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RoundRectangle2D rect = new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight);
        draw(rect);
    }

    /**
     * @see Graphics#fillRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RoundRectangle2D rect = new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight);
        fill(rect);
    }

    /**
     * @see Graphics#drawOval(int, int, int, int)
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        draw(oval);
    }

    /**
     * @see Graphics#fillOval(int, int, int, int)
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        fill(oval);
    }

    /**
     * @see Graphics#drawArc(int, int, int, int, int, int)
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Arc2D arc = new Arc2D.Double(x,y,width,height,startAngle, arcAngle, Arc2D.OPEN);
        draw(arc);

    }

    /**
     * @see Graphics#fillArc(int, int, int, int, int, int)
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Arc2D arc = new Arc2D.Double(x,y,width,height,startAngle, arcAngle, Arc2D.PIE);
        fill(arc);
    }

    /**
     * @see Graphics#drawPolyline(int[], int[], int)
     */
    @Override
    public void drawPolyline(int[] x, int[] y, int nPoints) {
        PolylineShape polyline = new PolylineShape(x, y, nPoints);
        draw(polyline);
    }

    /**
     * @see Graphics#drawPolygon(int[], int[], int)
     */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        draw(poly);
    }

    /**
     * @see Graphics#fillPolygon(int[], int[], int)
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        Polygon poly = new Polygon();
        for (int i = 0; i < nPoints; i++) {
            poly.addPoint(xPoints[i], yPoints[i]);
        }
        fill(poly);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return drawImage(img, x, y, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return drawImage(img, x, y, width, height, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, Color, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        waitForImage(img);
        return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, Color, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        waitForImage(img);
        double scalex = width/(double)img.getWidth(observer);
        double scaley = height/(double)img.getHeight(observer);
        AffineTransform tx = AffineTransform.getTranslateInstance(x,y);
        tx.scale(scalex,scaley);
        return drawImage(img, null, tx, bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int, Color, ImageObserver)
     */
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        waitForImage(img);
        double dwidth = (double)dx2-dx1;
        double dheight = (double)dy2-dy1;
        double swidth = (double)sx2-sx1;
        double sheight = (double)sy2-sy1;

        //if either width or height is 0, then there is nothing to draw
        if (dwidth == 0 || dheight == 0 || swidth == 0 || sheight == 0) return true;

        double scalex = dwidth/swidth;
        double scaley = dheight/sheight;

        double transx = sx1*scalex;
        double transy = sy1*scaley;
        AffineTransform tx = AffineTransform.getTranslateInstance(dx1-transx,dy1-transy);
        tx.scale(scalex,scaley);

        BufferedImage mask = new BufferedImage(img.getWidth(observer), img.getHeight(observer), BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = mask.getGraphics();
        g.fillRect(sx1,sy1, (int)swidth, (int)sheight);
        drawImage(img, mask, tx, null, observer);
        g.dispose();
        return true;
    }

    /**
     * @see Graphics#dispose()
     */
    @Override
    public void dispose() {
        if (kid)
            return;
        if (!disposeCalled) {
            disposeCalled = true;
            cb.restoreState();
            cb.restoreState();
            if (dg2 != null) {
               dg2.dispose();
               dg2 = null;
            }
            if (kids != null) {
                ByteBuffer buf = new ByteBuffer();
                internalDispose(buf);
                ByteBuffer buf2 = cb.getInternalBuffer();
                buf2.reset();
                buf2.append(buf);
            }
        }
    }

    private void internalDispose(ByteBuffer buf) {
        int last = 0;
        int pos;
        ByteBuffer buf2 = cb.getInternalBuffer();
        if (kids != null) {
            for (Kid kid: kids) {
                pos = kid.pos;
                PdfGraphics2DEx g2 = kid.graphics;
                g2.cb.restoreState();
                g2.cb.restoreState();
                buf.append(buf2.getBuffer(), last, pos - last);
                if (g2.dg2 != null) {
                  g2.dg2.dispose();
                  g2.dg2 = null;
                }
                g2.internalDispose(buf);
                last = pos;
            }
        }
        buf.append(buf2.getBuffer(), last, buf2.size() - last);
    }

    ///////////////////////////////////////////////
    //
    //
    //      implementation specific methods
    //
    //


    private void followPath(Shape s, int drawType) {
        if (s==null) return;
        if (drawType==STROKE) {
            if (!(stroke instanceof BasicStroke)) {
                s = stroke.createStrokedShape(s);
                followPath(s, FILL);
                return;
            }
        }
        if (drawType==STROKE) {
            setStrokeDiff(stroke, oldStroke);
            oldStroke = stroke;
            setStrokePaint();
        }
        else if (drawType==FILL)
            setFillPaint();
        PathIterator points;
        int traces = 0;
        if (drawType == CLIP)
            points = s.getPathIterator(IDENTITY);
        else
            points = s.getPathIterator(transform);
        float[] coords = new float[6];
        double[] dcoords = new double[6];
        while(!points.isDone()) {
            ++traces;
            // Added by Peter Harvey (start)
            int segtype = points.currentSegment(dcoords);
            int numpoints = (segtype == PathIterator.SEG_CLOSE ? 0
                  : (segtype == PathIterator.SEG_QUADTO ? 2
                        : (segtype == PathIterator.SEG_CUBICTO ? 3
                              : 1)));
            for (int i = 0; i < numpoints * 2; i++) {
               coords[i] = (float) dcoords[i];
            }
            // Added by Peter Harvey (end)
            normalizeY(coords);
            switch(segtype) {
                case PathIterator.SEG_CLOSE:
                    cb.closePath();
                    break;

                case PathIterator.SEG_CUBICTO:
                    cb.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;

                case PathIterator.SEG_LINETO:
                    cb.lineTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_MOVETO:
                    cb.moveTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_QUADTO:
                    cb.curveTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
            }
            points.next();
        }
        switch (drawType) {
        case FILL:
            if (traces > 0) {
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                    cb.eoFill();
                else
                    cb.fill();
            }
            break;
        case STROKE:
            if (traces > 0)
                cb.stroke();
            break;
        default: //drawType==CLIP
            if (traces == 0)
                cb.rectangle(0, 0, 0, 0);
            if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                cb.eoClip();
            else
                cb.clip();
            cb.newPath();
        }
    }

    private float normalizeY(float y) {
        return this.height - y;
    }

    private void normalizeY(float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    protected AffineTransform normalizeMatrix() {
        double[] mx = new double[6];
        AffineTransform result = AffineTransform.getTranslateInstance(0,0);
        result.getMatrix(mx);
        mx[3]=-1;
        mx[5]=height;
        result = new AffineTransform(mx);
        result.concatenate(transform);
        return result;
    }

    private boolean drawImage(Image img, Image mask, AffineTransform xform, Color bgColor, ImageObserver obs) {
        if (xform==null)
            xform = new AffineTransform();
        else
            xform = new AffineTransform(xform);
        xform.translate(0, img.getHeight(obs));
        xform.scale(img.getWidth(obs), img.getHeight(obs));

        AffineTransform inverse = this.normalizeMatrix();
        AffineTransform flipper = AffineTransform.getScaleInstance(1,-1);
        inverse.concatenate(xform);
        inverse.concatenate(flipper);

        double[] mx = new double[6];
        inverse.getMatrix(mx);
        if (currentFillGState != 255) {
            PdfGState gs = fillGState[255];
            if (gs == null) {
                gs = new PdfGState();
                gs.setFillOpacity(1);
                fillGState[255] = gs;
            }
            cb.setGState(gs);
        }

        try {
            com.itextpdf.text.Image image;
            if(!convertImagesToJPEG){
                image = com.itextpdf.text.Image.getInstance(img, bgColor);
            }
            else{
                BufferedImage scaled = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics2D g3 = scaled.createGraphics();
                g3.drawImage(img, 0, 0, img.getWidth(null), img.getHeight(null), null);
                g3.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(jpegQuality);//Set here your compression rate
                ImageWriter iw = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                iw.setOutput(ios);
                iw.write(null, new IIOImage(scaled, null, null), iwparam);
                iw.dispose();
                ios.close();

                scaled.flush();
                image = com.itextpdf.text.Image.getInstance(baos.toByteArray());

            }
            if (mask!=null) {
                com.itextpdf.text.Image msk = com.itextpdf.text.Image.getInstance(mask, null, true);
                msk.makeMask();
                msk.setInverted(true);
                image.setImageMask(msk);
            }
            cb.addImage(image, (float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]);
            Object url = getRenderingHint(HyperLinkKey.KEY_INSTANCE);
            if (url != null && !url.equals(HyperLinkKey.VALUE_HYPERLINKKEY_OFF)) {
               PdfAction action = new  PdfAction(url.toString());
                cb.setAction(action, (float)mx[4], (float)mx[5], (float)(mx[0]+mx[4]), (float)(mx[3]+mx[5]));
            }
        } catch (Exception ex) {
            return false;
        }
        if (currentFillGState >= 0 && currentFillGState != 255) {
            PdfGState gs = fillGState[currentFillGState];
            cb.setGState(gs);
        }
        return true;
    }

    private boolean checkNewPaint(Paint oldPaint) {
        if (paint == oldPaint)
            return false;
        return !(paint instanceof Color && paint.equals(oldPaint));
    }

    private void setFillPaint() {
        if (checkNewPaint(paintFill)) {
            paintFill = paint;
            setPaint(false, 0, 0, true);
        }
    }

    private void setStrokePaint() {
        if (checkNewPaint(paintStroke)) {
            paintStroke = paint;
            setPaint(false, 0, 0, false);
        }
    }

    private void setPaint(boolean invert, double xoffset, double yoffset, boolean fill) {
        if (paint instanceof Color) {
            Color color = (Color)paint;
            int alpha = color.getAlpha();
            if (fill) {
                if (alpha != currentFillGState) {
                    currentFillGState = alpha;
                    PdfGState gs = fillGState[alpha];
                    if (gs == null) {
                        gs = new PdfGState();
                        gs.setFillOpacity(alpha / 255f);
                        fillGState[alpha] = gs;
                    }
                    cb.setGState(gs);
                }
                cb.setColorFill(new BaseColor(color.getRGB()));
            }
            else {
                if (alpha != currentStrokeGState) {
                    currentStrokeGState = alpha;
                    PdfGState gs = strokeGState[alpha];
                    if (gs == null) {
                        gs = new PdfGState();
                        gs.setStrokeOpacity(alpha / 255f);
                        strokeGState[alpha] = gs;
                    }
                    cb.setGState(gs);
                }
                cb.setColorStroke(new BaseColor(color.getRGB()));
            }
        }
        else if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint)paint;
            Point2D p1 = gp.getPoint1();
            transform.transform(p1, p1);
            Point2D p2 = gp.getPoint2();
            transform.transform(p2, p2);
            Color c1 = gp.getColor1();
            Color c2 = gp.getColor2();
            PdfShading shading = PdfShading.simpleAxial(cb.getPdfWriter(), (float)p1.getX(), normalizeY((float)p1.getY()), (float)p2.getX(), normalizeY((float)p2.getY()), new BaseColor(c1.getRGB()), new BaseColor(c2.getRGB()));
            PdfShadingPattern pat = new PdfShadingPattern(shading);
            if (fill)
                cb.setShadingFill(pat);
            else
                cb.setShadingStroke(pat);
        }
        else if (paint instanceof TexturePaint) {
            try {
                TexturePaint tp = (TexturePaint)paint;
                BufferedImage img = tp.getImage();
                Rectangle2D rect = tp.getAnchorRect();
                com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(img, null);
                PdfPatternPainter pattern = cb.createPattern(image.getWidth(), image.getHeight());
                AffineTransform inverse = this.normalizeMatrix();
                inverse.translate(rect.getX(), rect.getY());
                inverse.scale(rect.getWidth() / image.getWidth(), -rect.getHeight() / image.getHeight());
                double[] mx = new double[6];
                inverse.getMatrix(mx);
                pattern.setPatternMatrix((float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]) ;
                image.setAbsolutePosition(0,0);
                pattern.addImage(image);
                if (fill)
                    cb.setPatternFill(pattern);
                else
                    cb.setPatternStroke(pattern);
            } catch (Exception ex) {
                if (fill)
                    cb.setColorFill(BaseColor.GRAY);
                else
                    cb.setColorStroke(BaseColor.GRAY);
            }
        }
        else {
            try {
                int type = BufferedImage.TYPE_4BYTE_ABGR;
                if (paint.getTransparency() == Transparency.OPAQUE) {
                    type = BufferedImage.TYPE_3BYTE_BGR;
                }
                BufferedImage img = new BufferedImage((int)width, (int)height, type);
                Graphics2D g = (Graphics2D)img.getGraphics();
                g.transform(transform);
                AffineTransform inv = transform.createInverse();
                Shape fillRect = new Rectangle2D.Double(0,0,img.getWidth(),img.getHeight());
                fillRect = inv.createTransformedShape(fillRect);
                g.setPaint(paint);
                g.fill(fillRect);
                if (invert) {
                    AffineTransform tx = new AffineTransform();
                    tx.scale(1,-1);
                    tx.translate(-xoffset,-yoffset);
                    g.drawImage(img,tx,null);
                }
                g.dispose();
                com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(img, null);
                PdfPatternPainter pattern = cb.createPattern(width, height);
                image.setAbsolutePosition(0,0);
                pattern.addImage(image);
                if (fill)
                    cb.setPatternFill(pattern);
                else
                    cb.setPatternStroke(pattern);
            } catch (Exception ex) {
                if (fill)
                    cb.setColorFill(BaseColor.GRAY);
                else
                    cb.setColorStroke(BaseColor.GRAY);
            }
        }
    }

    private synchronized void waitForImage(Image image) {
        if (mediaTracker == null)
            mediaTracker = new MediaTracker(new FakeComponent());
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        }
        catch (InterruptedException e) {
            // empty on purpose
        }
        mediaTracker.removeImage(image);
    }

    static private class FakeComponent extends Component {

      private static final long serialVersionUID = 6450197945596086638L;
    }

    /**
     * @since 2.0.8
     */
    public static class HyperLinkKey extends Key
   {
      public static final HyperLinkKey KEY_INSTANCE = new HyperLinkKey(9999);
      public static final Object VALUE_HYPERLINKKEY_OFF = "0";

      protected HyperLinkKey(int arg0) {
         super(arg0);
      }

      @Override
        public boolean isCompatibleValue(Object val)
      {
         return true;
      }
      @Override
        public String toString()
      {
         return "HyperLinkKey";
      }
   }

}
