package com.srscicomp.common.ui;

import com.srscicomp.common.util.Utilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * <p>CheckMarkIcon is custom icon that paints a check mark of a specified color.  The check mark is constained to lie 
 * within the bounds of a 2:1 (h:w) rectangle of specified height.  The enclosing rectangle is not rendered, and its 
 * height is restricted to the range [8..64] pixels.  If no color is specified, the check mark is rendered in the 
 * parent component's current foreground color.</p>
 *
 * <p>CREDITS:  This class was modelled after the example
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/example-1dot4/ArrowIcon.java">ArrowIcon in Sun's Java
 * Tutorial</a>.</p>
 *
 * @author sruffner
 */
public class CheckMarkIcon implements Icon
{
	public static final int MAX_H = 64;
	public static final int MIN_H = 8;

	private final Color color;
	private final int height;

	private final int[] xPts = new int[6];
	private final int[] yPts = new int[6];

	public CheckMarkIcon()
	{
		this( null, MIN_H );
	}

	public CheckMarkIcon( Color c, int h )
	{
		color = c;
		height = Utilities.rangeRestrict(MIN_H, MAX_H, h);

		// prepare the set of points defining the check mark as a filled polygon
		int width = height/2;
		xPts[0] = width;
		yPts[0] = 0;
		xPts[1] = (width*3)/8;
		yPts[1] = height;
		xPts[2] = 0;
		yPts[2] = (height*7)/8;
		xPts[3] = 0;
		yPts[3] = yPts[2] - (height/4);
		xPts[4] = xPts[1];
		yPts[4] = height - (height/4);
		xPts[5] = width - 1;
		yPts[5] = 0;
		
	}

	public Color getIconColor()
	{
		return( color );
	}


	/* (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		// if a color was specified for icon, use that color; else, use parent component's foreground color
		Color paintColor = color;
		if( paintColor == null )
			paintColor = (c != null) ? c.getForeground() : Color.black;

		Color oldColor = g.getColor();

		g.setColor( paintColor );
		g.translate( x, y );
		g.fillPolygon( xPts, yPts, xPts.length );

		g.translate (-x, -y );
		g.setColor( oldColor );
	}

	/* (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
	public int getIconWidth()
	{
		return( height/2 );
	}

	/* (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
	public int getIconHeight()
	{
		return( height );
	}
}
