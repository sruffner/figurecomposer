package com.srscicomp.common.ui;

import com.srscicomp.common.util.Utilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * A non-bitmap icon that paints an arrow of a specified color pointing in any of the four cardinal directions. The 
 * arrow is constrained to lie within the bounds of a NxN-pixel square, where N is restricted to the range [8..64]. If 
 * no color is specified, the arrow is rendered in the parent component's current foreground color. Both the color and
 * direction of the arrow may be changed while in use.
 * 
 * <p>If a border color is specified via {@link #setOutlineColor(Color)}, the arrow icon is outlined in that color.</p>
 * 
 * <p>CREDITS:  This class was modelled after an example in Sun's Java Tutorial at 
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/example-1dot4/ArrowIcon.java.</p>
 * 
 * @author 	sruffner
 */
public class ArrowIcon implements Icon
{
   /** An enumeration of the four cardinal directions in which a {@link ArrowIcon} can point. */
   public enum Direction { RIGHT, LEFT, UP, DOWN }

	/**
	 * Construct an arrow icon of the specified color and size. Initially, the arrow points upward.
	 * 
	 * @param c Desired color with which arrow's interior is filled. If null, the interior is painted with the foreground
	 * color of the icon's parent component.
	 * @param size Desired size of arrow; restricted to the range [8..64].
	 */
	public ArrowIcon(Color c, int size) { this(c, size, Direction.UP); }

   /**
    * Construct an arrow icon of the specified color, size, and direction. 
    * @param c Desired color with which arrow's interior is filled. If <code>null</code>, the interior is painted with 
    * the foreground color of the icon's parent component.
    * @param size Desired size of arrow; restricted to the range [8..64].
    * @param dir The initial direction in which arrow points. If null, the arrow will point upward.
    */
	public ArrowIcon(Color c, int size, Direction dir)
	{
		this.dir = (dir == null) ? Direction.UP : dir;
		this.color = c;
		this.size = Utilities.rangeRestrict(MIN_SIZE, MAX_SIZE, size);
		computeVertices();
	}
	
	/**
	 * Get the current cardinal direction in which this arrow icon is pointing.
	 * @return The arrow's direction.
	 */
	public Direction getDirection() { return(dir); }
	/**
	 * Set direction of arrow icon. <i>The icon will be updated when its parent component is repainted.</i>
	 * @param dir The desired direction in which arrow should point.
	 */
	public void setDirection(Direction dir)
	{
		if(this.dir == dir) return;
		this.dir = dir;
		computeVertices();
	}

   /**
    * Get this arrow icon's color.
    * @return The color. <b>If null, then arrow is painted in the foreground color of the parent component.</b>
    */
   public Color getColor() { return(color); }
   /**
    * Set this arrow icon's color. <i>The icon will be updated when its parent component is repainted.</i>
    * @param c The desired color. <b>If null, arrow is painted in the foreground color of the parent component.</b>
    */
   public void setColor(Color c)
   {
      this.color = c;
   }
   
   /**
    * Get the color used to trace the arrow's outline (1-pixel thick).
    * @return The color. <b>If null, then arrow icon is not outlined.</b>
    */
   public Color getOutlineColor() { return(outlineColor); }
   /**
    * Set the color used trace the arrow's outline. <i>The icon is updated when its parent component is repainted.</i>
    * @param c The desired color. <b>If null, then the arrow icon is not outlined</b>
    */
   public void setOutlineColor(Color c)
   {
      this.outlineColor = c;
   }
   
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		// if a color was specified for icon, use that color; else, use parent component's foreground color
		Color paintColor = color;
		if(paintColor == null) paintColor = (c != null) ? c.getForeground() : Color.black;
		
		Color oldColor = g.getColor();
		g.setColor(paintColor);
		g.translate( x, y );

		g.fillPolygon(xPts, yPts, xPts.length);
		
		if(outlineColor != null)
		{
		   g.setColor(outlineColor);
		   g.drawPolygon(xPts, yPts, xPts.length);
		}
		
		g.translate(-x, -y);
		g.setColor(oldColor);
	}

	public int getIconWidth() { return(size); }
	public int getIconHeight() { return(size); }

	/**
	 * Helper method computes and stores internally the three vertices defining the triangle that is filled to "render"
	 * the arrow icon. The vertex coordinates are relative to a bounding square, the top-left corner of which coincides
	 * with the icon's location in the parent component. This method is invoked whenever the arrow direction changes.
	 */
	private void computeVertices()
	{
	   if(dir==Direction.UP || dir == Direction.DOWN)
	   {
	      xPts[0] = 0;
	      xPts[1] = size;
	      xPts[2] = size/2;
	      yPts[0] = (dir==Direction.UP) ? size - size/5 : size/5;
	      yPts[1] = yPts[0];
	      yPts[2] = (dir==Direction.UP) ? size/5 : size - size/5;
	   }
	   else
	   {
         yPts[0] = 0;
         yPts[1] = size;
         yPts[2] = size/2;
         xPts[0] = (dir==Direction.LEFT) ? size - size/5 : size/5;
         xPts[1] = xPts[0];
         xPts[2] = (dir==Direction.LEFT) ? size/5 : size - size/5;
	   }
	}
	
	
   /** Arrow icon's fill color. If <i>null</i>, use foreground color of icon's parent component. */
   private Color color;
   /** Size of the square bounding the arrow icon. */
   private final int size;
   /** The cardinal direction in which the arrow icon points.  */
   private Direction dir;
   /** The x-coordinates of the arrow's vertices, specified relative to the icon's location in parent component. */
   private final int[] xPts = new int[3];
   /** The y-coordinates of the arrow's vertices, specified relative to the icon's location in parent component. */
   private final int[] yPts = new int[3];
   
   /** Arrow icon's border color. If <i>null</i>, the 1-pixel border is not drawn. */
   private Color outlineColor = null;
   
   /** Maximum size of the arrow icon's bounding square. */
   private final static int MAX_SIZE = 64;

   /** Minimum size of the arrow icon's bounding square. */
   private final static int MIN_SIZE = 8;
}
