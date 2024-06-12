package com.srscicomp.fc.uibase;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.AbstractRenderableModel;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;

/**
 * A simple graphic model that displays a text message laid out within a 4x4-in rectangle. It provides a means of 
 * temporarily displaying a message in a {@link com.srscicomp.common.g2dviewer.Graph2DViewer Graph2DViewer} while a complex graphic 
 * is being read in from file and prepared for display, or to display an error message if the graphic could not be read.
 * 
 * @author sruffner
 */
public class CenteredMessageModel extends AbstractRenderableModel implements RootRenderable
{
   /**
    * Construct the centered-message graphic initialized to display the specified string.
    * @param msg The string to display. It will be rendered in a 16pt font.
    */
   public CenteredMessageModel(String msg)
   {
      painter = new TextBoxPainter();
      painter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.BOLD, 16*1000/72), 
               0, null, Color.BLACK, Color.RED));
      painter.setText(msg==null ? "" : msg.trim());
      painter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      painter.setBoundingBox(new Point2D.Double(), FIXEDSZ, FIXEDSZ, 200);
      painter.setClipped(true);
   }
   
   /** The internal painter that renders the text message laid out in a 4x4-in text box. */
   private TextBoxPainter painter = null;

   /** The fixed size of the square graphic rendered, in milli-inches.  */
   private final static int FIXEDSZ = 4000;
   
   /**
    * Set the message string displayed by this graphic model. If the model is currently installed in a viewer, it is
    * re-rendered accordingly.
    * @param msg The string to display. Null equates to an empty string.
    */
   public void setTextMessage(String msg)
   {
      painter.setText(msg == null ? "" : msg.trim());
      if(rmviewer != null) rmviewer.modelMutated(null);
   }
   
   @Override public RootRenderable getCurrentRootRenderable() { return(this); }
   @Override public double getHeightMI() { return(FIXEDSZ); }
   @Override public double getWidthMI() { return(FIXEDSZ); }
   @Override public Point2D getPrintLocationMI() { return(new Point2D.Float(0,0)); }
   @Override public boolean hasTranslucentRegions() { return(false); }
   
   @Override public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      return(painter.render(g2d, task)); 
   }
}


