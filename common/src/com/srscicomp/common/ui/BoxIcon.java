package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * BoxIcon is custom icon that paints a square filled with a specified color.  The side of the square is restricted to 
 * the range [8..64] pixels.  The color can be changed programmatically.  If no color is specified, BoxIcon will take 
 * on the foreground color of its parent component.
 *
 * <p>CREDITS:  This class was modelled after the
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/example-1dot4/ArrowIcon.java">ArrowIcon in Sun's Java
 * Tutorial</a>.</p>
 *
 * @author sruffner
 */
public class BoxIcon implements Icon
{
	public static final int MAX_SZ = 64;
	public static final int MIN_SZ = 8;

	private Color color;
	private final int size;

	public BoxIcon()
	{
		this( null, MIN_SZ );
	}

	public BoxIcon( Color c, int sz )
	{
		color = c;
		size = (sz < MIN_SZ) ? MIN_SZ : Math.min(sz, MAX_SZ);
	}

	public Color getIconColor()
	{
		return( color );
	}

	/**
	 * Set the color of this BoxIcon.  The color change won't be apparent until the parent component repaints the icon 
	 * by calling {@link #paintIcon(Component, Graphics,int,int) paintIcon()}.
	 * 
	 * @param 	c The new color.
	 */
	public void setIconColor( Color c )
	{
		color = c;
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
		g.fill3DRect(x, y, size, size, true);
		g.setColor( oldColor );
	}

	/* (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
	public int getIconWidth()
	{
		return( size );
	}

	/* (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
	public int getIconHeight()
	{
		return( size );
	}
}
