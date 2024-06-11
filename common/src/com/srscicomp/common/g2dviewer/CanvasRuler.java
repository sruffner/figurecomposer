package com.srscicomp.common.g2dviewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import com.srscicomp.common.util.Utilities;

/**
 * CanvasRuler is intended for use as a horizontal ruler along the bottom edge of the RenderingCanvas, or as a vertical 
 * ruler along its left edge. These rulers serve as guides that reflect the relative position of the canvas viewport in
 * the coordinate system of the displayed graphic. Tick marks along the ruler specify position in inches.  Ticks of 
 * different lengths mark intervals of inches, half-inches, tenth-inches, and hundredth-inches (the latter two are only 
 * drawn when there is sufficient space between adjacent ticks -- when the associated canvas is "zoomed" considerably).
 * In keeping with the RenderingCanvas's convention, coordinates increase rightward along the horizontal ruler and 
 * upward along the vertical ruler.  In addition, a translucent blue arrow serves to indicate the mouse cursor's 
 * position whenever it is inside the canvas viewport.
 *
 * <p>To use a vertical CanvasRuler, lay it out in the container immediately to the left of the RenderingCanvas in 
 * such a way that it is ALWAYS the same height as the canvas (the width is fixed). Connect it to the canvas by 
 * registering it as a CanvasListener. An analogous discussion applies to a horizontal CanvasRuler.</p>
 *
 * <p>
 * ------------------------------------------------------------------------------------------------------------------- 
 * CREDITS:  This class was adapted from the class Rule.java that was part of the ScrollDemo program in Sun 
 * Microsystem's Java Tutorial for JS2E 1.4.x.  There was no author line in that file, so here's a
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/Rule.java">link</a>f or the file
 * on Sun's Java website.
 * ------------------------------------------------------------------------------------------------------------------- 
 * </p>
 *
 * @see        RenderingCanvas
 * @author sruffner
 */
final class CanvasRuler extends JComponent implements CanvasListener
{
   private static final long serialVersionUID = 1L;

   /**
	 * Design height(width) of a horizontal(vertical) ruler, in pixels.
	 */
	private static final int DESIGN_SZ = 27;

   /**
    * Minimum length of the unconstrained dimension of the ruler, in pixels.
    */
   private static final int MIN_UNCONSTRAINED_SZ = 100;

	/**
	 * Design length for ticks used to mark inches along the ruler.
	 */
	private static final int TICK1_LEN = 12;

	/**
	 * Design length for ticks used to mark half-inches along the ruler.
	 */
	private static final int TICK2_LEN = 8;

   /**
    * Design length for ticks used to mark tenth-inches along the ruler.
    */
   private static final int TICK10_LEN = 6;

   /**
    * Design length for ticks used to mark tenth-inches along the ruler.
    */
   private static final int TICK100_LEN = 3;

	/**
	 * Min interval between half-inch, tenth-inch, or hundredth-inch tick marks, in pixels.  If calculated interval is 
    * less than this design value, then the corresponding set of tick marks are not rendered.
	 */
	private static final int MIN_SUBTICKINTV_DEV = 10;

	/**
	 * Min interval between the major labeled tick marks, in pixels.  The interval in inches is chosen so that the 
	 * interval in pixels is greater than or equal to this design value.
	 */
	private static final int MIN_TICK1INTV_DEV = 20;

   /**
    * If the interval in device pixels between consecutive half-inch tick marks exceeds this value, then we include 
    * labels for those marks.  Likewise for tenth-inch marks.  Hundredth-inch marks are never labelled.
    */
   private static final int MIN_SUBTICKLBLINTV_DEV = 100;

	/**
	 * Font size for tick labels, in points.
	 */
	private static final int LBLFONT_SZ = 10;

	/**
	 * Flag determines ruler's orientation (horizontal or vertical).
	 */
	private final boolean isHorizontal;

	/**
	 * Construct a CanvasRuler in the specified orientation that reflects horizontal or vertical position in 
	 * inches along a RenderingCanvas viewport displaying a possible zoomed & panned graphic. The ruler will not be 
    * operational until it receives the relevant events from the canvas.
	 * 
	 * @param isHoriz If <code>true</code>, a horizontal ruler is constructed, else a vertical ruler.
	 */
	public CanvasRuler(boolean isHoriz) 
	{
		this.isHorizontal = isHoriz;
	}

   /**
    * Return the width of a vertical ruler, or the height of a horizontal ruler.  These dimensions are fixed, while 
    * the orthogonal direction can grow to be any size.  This parameter should help in laying out the rulers.
    * 
    * @return Ruler design size, in pixels.
    */
   public static int getRulerDesignSize()
   {
      return(DESIGN_SZ);
   }

   @Override
   public Dimension getMaximumSize()
   {
      return(new Dimension(isHorizontal ? Integer.MAX_VALUE : DESIGN_SZ, 
            isHorizontal ? DESIGN_SZ : Integer.MAX_VALUE));
   }

   @Override
   public Dimension getMinimumSize()
   {
      return(new Dimension(isHorizontal ? MIN_UNCONSTRAINED_SZ : DESIGN_SZ, 
            isHorizontal ? DESIGN_SZ : MIN_UNCONSTRAINED_SZ));
   }

   
   //
   // Cursor arrow
   //

   /**
    * Design length of cursor arrow, in pixels.
    */
   private static final int CURSORLEN = DESIGN_SZ/2;

   /**
    * Design thickness of cursor arrow, in pixels.
    */
   private static final int CURSORTHICKNESS = 5;

   /**
    * The image of the cursor arrow.
    */
   private BufferedImage cursorArrow = null;

   /**
    * Create the arrow image that is rendered at the current location of the canvas mouse cursor along the ruler.  The 
    * size and orientation of the arrow, of course, depend upon the ruler's orientation:
    * <ul>
    *    <li>Horizontal ruler: Arrow image is CURSORTHICKNESS x CURSORLEN pixels in size.  Arrow points upward, with 
    *    arrow tip at midpoint along top edge of image.</li>
    *    <li>Vertical ruler: Arrow image is CURSORLEN x CURSORTHICKNESS pixels in size.  Arrow points rightward, with 
    *    arrow tip at midpoint along right edge of image.</li>
    * </ul>
    * <p>The arrow image is somewhat translucent, so that the ruler tick marks show through when it is drawn on top of 
    * the ruler with AlphaComposite.SRC_OVER compositing.</p>
    * 
    * <p>The buffered image in which the arrow is maintained cannot be created until the CanvasRuler is displayable (so
    * we can get a graphics configuration in order to create a compatible image type).  After calling, make sure that 
    * the arrow image was successfully created.</p>
    */
   private void createCursorArrow()
   {
      if(cursorArrow != null) return;
      GraphicsConfiguration gcfg = getGraphicsConfiguration();
      if(gcfg == null) return;

      // create the image buffer with the right dimensions, depending upon ruler orientation.
      int w = isHorizontal ? CURSORTHICKNESS : CURSORLEN;
      int h = isHorizontal ? CURSORLEN : CURSORTHICKNESS;
      cursorArrow = gcfg.createCompatibleImage(w, h,Color.TRANSLUCENT);
      Graphics2D g2 = cursorArrow.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      // fill image w/ completely transparent white
      g2.setColor( new Color(1f,1f,1f,0f) );
      g2.fillRect(0, 0, 1000, 1000);

      // draw arrow outline
      GeneralPath arrowPath = new GeneralPath();
      if( isHorizontal )
      {
         arrowPath.moveTo(w/2f, 0); arrowPath.lineTo(w-1, h/3f);
         arrowPath.lineTo(w-1,h-1); arrowPath.lineTo(0,h-1); arrowPath.lineTo(0,h/3f); arrowPath.closePath();
      }
      else
      {
         arrowPath.moveTo(w-1,h/2f); arrowPath.lineTo(2*w/3f,h-1);
         arrowPath.lineTo(0,h-1); arrowPath.lineTo(0,0); arrowPath.lineTo(2*w/3f,0); arrowPath.closePath();
      }

      // outline it with 65% transparent white, fill with 40% transparent blue
      g2.setStroke( new BasicStroke(1) );
      g2.setColor( new Color(0f,0.1f,0.7f,0.6f) );
      g2.fill(arrowPath);
      g2.setColor( new Color(1f,1f,1f,0.35f) );
      g2.draw(arrowPath);
   }

   //
   // CanvasListener -- Listen for changes in canvas viewport or mouse cursor position
   //

   /**
    * Current location (UL corner) of the cursor arrow's bounding rectangle within the ruler's window, in device pixels.
    * If either coordinate is negative, then the cursor is currently not drawn. 
    */
   private final Point currentCursorUL = new Point(-1,-1);

   /**
    * The current canvas viewport rectangle, expressed in the logical coordinate system of the graphic displayed on the 
    * canvas. If it is empty, it is assumed that no viewport is defined -- so the ruler will not be drawn.
    */
   private final Rectangle2D vpRectLogical = new Rectangle2D.Double();

   public void renderingStarted(CanvasEvent e)
   {
   }

   public void renderingStopped(CanvasEvent e)
   {
   }

   public void renderingCompleted(CanvasEvent e)
   {
   }

   public void renderingInProgress(CanvasEvent e)
   {
   }

   /**
    * Whenever the cursor moves on the canvas, repaint the ruler to reflect its new position.
    */
   public void cursorMoved(CanvasEvent e)
   {
      // ignore until we've create the cached image of the cursor arrow
      if(cursorArrow == null) return;

      // the new position of the cursor, in logical coordinates
      double xCursor = e.getCurrentCursorX();
      double yCursor = e.getCurrentCursorY();
      
      // update location of cursor rectangle's UL, in device coordinates
      int oldX = currentCursorUL.x;
      int oldY = currentCursorUL.y;
      if(Double.isNaN(xCursor) || Double.isNaN(yCursor) || vpRectLogical.isEmpty())
         currentCursorUL.setLocation(-1,-1);
      else if(isHorizontal)
      {
         double devX = (xCursor - vpRectLogical.getX()) * getWidth() / vpRectLogical.getWidth();
         currentCursorUL.x = (int) Math.round(devX - ((double)CURSORTHICKNESS)/2.0);
         currentCursorUL.y = 1;
      }
      else
      {
         double canvasH = getHeight();
         double devY = canvasH - ((yCursor - vpRectLogical.getY()) * canvasH / vpRectLogical.getHeight());
         currentCursorUL.y = (int) Math.round(devY - ((double)CURSORTHICKNESS)/2.0);
         currentCursorUL.x = DESIGN_SZ-CURSORLEN-1;
      }
      
      // now decide what needs repainting, if anything, based on change in cursor rectangle's pos.
      int w = isHorizontal ? CURSORTHICKNESS : CURSORLEN;
      int h = isHorizontal ? CURSORLEN : CURSORTHICKNESS;
      if(oldX < 0)
      {
          // repaint if cursor has just entered the canvas viewport
          if(currentCursorUL.x >= 0) repaint(currentCursorUL.x, currentCursorUL.y, w, h);
      }
      else if(currentCursorUL.x < 0)
      {
         // cursor has just left the canvas viewport
         repaint(oldX, oldY, w, h);
      }
      else if( (isHorizontal && Math.abs(oldX - currentCursorUL.x)>1) ||
            (!isHorizontal && Math.abs(oldY - currentCursorUL.y)>1) )
      {
         // cursor rect has shifted at least 2 pixels within the canvas viewport.
         repaint(oldX, oldY, w, h);
         repaint(currentCursorUL.x, currentCursorUL.y, w, h);
      }
      else
      {
         // leave the cursor where it was
         currentCursorUL.x = oldX;
         currentCursorUL.y = oldY;
      }
   }

   /**
    * Whenever canvas viewport changes, update the preferred size of the ruler to stay aligned with the canvas, save 
    * the new logical viewport definition, and repaint.
    */
   public void viewportChanged(CanvasEvent e)
   {
      vpRectLogical.setRect(e.getViewportRectLogical());
      repaint();
   }

	/**
	 * Paints the ruler in the specified graphics context.  We paint a ruler with ticks of different lengths at 
	 * increments of 1in, 0.5in, and -- if there's sufficient resolution -- 0.1 and 0.01in.  To optimize performance, we 
    * paint only inside the current clip rectangle in the graphics context.
	 * 
	 * <p>If there currently is no page displayed in the page canvas, the ruler ticks are not drawn.</p>
	 */
	protected void paintComponent( Graphics g )
	{
      // we create the cursor arrow here, since we must have a graphics configuration by now
      if(cursorArrow == null)
         createCursorArrow();
 
      Graphics2D g2 = (Graphics2D) g;

      // get clipping area and fill it with the component's current background color.
		Rectangle drawHere = g2.getClipBounds();
      g2.setColor(getBackground());
      g2.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

      // if canvas viewport not defined, we can't draw anything!
      if(vpRectLogical.getWidth() <= 0 || vpRectLogical.getHeight() <= 0) return;

      // get endpoints of the ruler section that needs painting, in device coordinates
      int startDev = (isHorizontal) ? drawHere.x : drawHere.y;
      int endDev = startDev + (isHorizontal ? drawHere.width : drawHere.height);

      // get tick intervals in device units, but NOT rounded to nearest pixel.
      double devSz = isHorizontal ? getWidth() : getHeight();
      double inch = devSz * 1000.0 / (isHorizontal ? vpRectLogical.getWidth() : vpRectLogical.getHeight());
      double halfInch = inch/2.0;
      double tenthInch = inch/10.0;
      double hundredthInch = inch/100.0;

      // draw a line along the top or left edge, using the component's current foreground color.
      g2.setColor(getForeground());
      if(isHorizontal)
         g2.drawLine(startDev, 0, endDev, 0);
      else
         g2.drawLine(DESIGN_SZ-1, startDev, DESIGN_SZ-1, endDev);

		// render ruler ticks and labels in the component's current foreground color.  Set font for labels.
      g2.setFont(new Font("SansSerif", Font.PLAIN, LBLFONT_SZ));

      // get current font metrics so we can horizontally or vertically center tick label wrt the tick line
      FontMetrics fm = g2.getFontMetrics();

     // get left(top) edge of horizontal(vertical) ruler in logical coordinates (inches). For vertical ruler, remember 
      // that logical y-axis is inverted!!
      double startLogical = 
         (isHorizontal ? vpRectLogical.getX() : (vpRectLogical.getY() + vpRectLogical.getHeight())) / 1000.0;

      // find the device and logical coords of the first whole-inch tick mark that's to the left of(above) the 
      // horizontal(vertical) ruler's left(top) edge.  Of course, this mark is never drawn because it is outside the 
      // ruler's bounding rectangle.
      double adj = Math.rint(startLogical);
      if( isHorizontal && adj > startLogical ) adj -= 1;
      else if( !isHorizontal && adj < startLogical ) adj += 1;
      double originDev = -Math.abs( (startLogical-adj) * inch );
      startLogical = adj;

      // when we're repainting a narrow ruler section to animate the cursor, we have to be careful to redraw everything 
      // we erase. If the repaint section is just adjacent to a tick mark, the mark may get erased (in the fillRect() 
      // call earlier), but we'll fail to redraw it. A bigger problem is the fact that the cursor rectangle slightly 
      // overlaps the numeric tick labels, particularly for the tenth-inch marks. To mark sure we always rerender such 
      // nearby labelled tick marks, we expand the endpoints of the ruler section that's being repainted...
      double expand = ((isHorizontal) ? fm.stringWidth("000") : fm.getHeight())/2.0;
      if(expand < 1) expand = 1;
      startDev -= (int) expand;
      endDev += (int) expand;

		// draw the one-inch tick marks that are within the clip rectangle, along with associated tick labels.  If the 
		// distance in pixels between consecutive tick marks is too small, then we increment the tick interval until the 
		// distance is large enough.  In this case, the finer ticks will never be drawn.
		int scaleUp = 1;
		double incrDev = inch;
		while( incrDev < MIN_TICK1INTV_DEV )
		{
			incrDev += inch;
			++scaleUp;
		}
		double posLogical = startLogical;
		double incrLogical = isHorizontal ? scaleUp : -scaleUp;  // for vertical ruler, log coords DECREASE downward!
		double posDev = originDev;
		while(posDev < startDev) 
		{
			posDev += incrDev;
			posLogical += incrLogical;
		}
		while(posDev <= endDev)
		{
			int tick = (int) Math.round( posDev );
			String tickLbl = Utilities.toString(posLogical, 6, 0);
			Rectangle2D lblRect = fm.getStringBounds(tickLbl,g2);
			if( isHorizontal ) 
			{
            g2.drawLine(tick, 0, tick, TICK1_LEN-1);
				int textStartX = (int) Math.round(posDev - lblRect.getWidth()/2.0);
            int textStartY = (int) Math.round(TICK1_LEN + 2.0*lblRect.getHeight()/3.0);
            g2.drawString(tickLbl, textStartX, textStartY);
			} 
			else
			{
            g2.drawLine(DESIGN_SZ-1, tick, DESIGN_SZ-TICK1_LEN-1, tick);
				int textStartX = (int) Math.round( DESIGN_SZ - TICK1_LEN - 2 - lblRect.getWidth() );
				int textStartY = (int) Math.round( tick + lblRect.getHeight()/3.0 );
            g2.drawString(tickLbl, textStartX, textStartY);
			} 

			posDev += incrDev;
			posLogical += incrLogical;
		}
		
		// draw the half-inch tick marks that are within the clip rectangle. Don't draw over the inch marks, and don't
		// draw any if the half-inch ticks are too close! Labels are drawn if the marks are far enough apart.
		if(halfInch >= MIN_SUBTICKINTV_DEV)
      {
         posDev = originDev;
         posLogical = startLogical;
         incrLogical = isHorizontal ? 0.5 : -0.5;  // for vertical ruler, log coords DECREASE downward!
         int nTick = 0;
         while(posDev < startDev) 
         {
            posDev += halfInch;
            posLogical += incrLogical;
            ++nTick;
         }
         while(posDev <= endDev)
         {
            if((nTick % 2) != 0)
            {
               int tick = (int) Math.round( posDev );
               if(isHorizontal) g2.drawLine( tick, 0, tick, TICK2_LEN-1);
               else g2.drawLine(DESIGN_SZ-1, tick, DESIGN_SZ-TICK2_LEN-1, tick);

               if(halfInch >= MIN_SUBTICKLBLINTV_DEV)
               {
                  String tickLbl = Utilities.toString(posLogical, 6, 1);
                  Rectangle2D lblRect = fm.getStringBounds(tickLbl, g2);
                  if(isHorizontal) 
                  {
                     int textStartX = (int) Math.round(posDev - lblRect.getWidth()/2.0);
                     int textStartY = (int) Math.round(TICK1_LEN + 2.0*lblRect.getHeight()/3.0);
                     g2.drawString(tickLbl, textStartX, textStartY);
                  } 
                  else
                  {
                     int textStartX = (int) Math.round(DESIGN_SZ - TICK2_LEN - 2 - lblRect.getWidth());
                     int textStartY = (int) Math.round(tick + lblRect.getHeight()/3.0);
                     g2.drawString(tickLbl, textStartX, textStartY);
                  }                 
               }
            }

            posDev += halfInch;
            posLogical += incrLogical;
            ++nTick;
         }
      }
		
		// draw the tenth-inch tick marks that are within the clip rectangle. Don't draw over the other marks, and don't
		// draw any if the tenth-inch ticks are too close! Labels are drawn if the marks are far enough apart.
		if( tenthInch >= MIN_SUBTICKINTV_DEV )
      {
         posDev = originDev;
         posLogical = startLogical;
         incrLogical = isHorizontal ? 0.1 : -0.1;  // for vertical ruler, log coords DECREASE downward!
         int nTick = 0;
         while(posDev < startDev) 
         {
            posDev += tenthInch;
            posLogical += incrLogical;
            ++nTick;
         }
         while(posDev <= endDev)
         {
            if((nTick % 5) != 0)
            {
               int tick = (int) Math.round( posDev );
               if(isHorizontal) g2.drawLine( tick, 0, tick, TICK10_LEN-1);
               else g2.drawLine(DESIGN_SZ-1, tick, DESIGN_SZ-TICK10_LEN-1, tick);

               if(tenthInch >= MIN_SUBTICKLBLINTV_DEV)
               {
                  String tickLbl = Utilities.toString(posLogical, 6, 1);
                  Rectangle2D lblRect = fm.getStringBounds(tickLbl, g2);
                  if(isHorizontal) 
                  {
                     int textStartX = (int) Math.round(posDev - lblRect.getWidth()/2.0);
                     int textStartY = (int) Math.round(TICK1_LEN + 2.0*lblRect.getHeight()/3.0);
                     g2.drawString(tickLbl, textStartX, textStartY);
                  } 
                  else
                  {
                     int textStartX = (int) Math.round(DESIGN_SZ - TICK10_LEN - 2 - lblRect.getWidth());
                     int textStartY = (int) Math.round(tick + lblRect.getHeight()/3.0);
                     g2.drawString(tickLbl, textStartX, textStartY);
                  }                 
               }
            }

            posDev += tenthInch;
            posLogical += incrLogical;
            ++nTick;
         }
      }

      // draw the hundredth-inch tick marks that are within the clip rectangle. Don't draw over the other marks, and 
      // don't draw any if the hundredth-in ticks are too close! Labels are never drawn for these marks.
      if( hundredthInch >= MIN_SUBTICKINTV_DEV )
      {
         posDev = originDev;
         int nTick = 0;
         while(posDev < startDev) 
         {
            posDev += hundredthInch;
            ++nTick;
         }
         while(posDev <= endDev)
         {
            if((nTick % 10) != 0)
            {
               int tick = (int) Math.round( posDev );
               if(isHorizontal) g2.drawLine( tick, 0, tick, TICK100_LEN-1);
               else g2.drawLine(DESIGN_SZ-1, tick, DESIGN_SZ-TICK100_LEN-1, tick);
            }

            posDev += hundredthInch;
            ++nTick;
         }
      }

      // draw cursor arrow if its bounding rect on the ruler is currently defined.
      if(currentCursorUL.x >= 0 && currentCursorUL.y >= 0)
      {
         g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
         g2.drawImage(cursorArrow, currentCursorUL.x, currentCursorUL.y, null);
      }
   }

}
