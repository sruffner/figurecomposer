package com.srscicomp.common.ui;

import java.awt.Dimension;

import javax.swing.JTextField;

/**
 * JFixedWidthTextField is a simple extension of JTextField that overrides certain methods in an attempt to force the 
 * text field to have a fixed width in pixels.  This is merely here to deal with one difficulty of laying out GUIs in 
 * Swing -- the tendency for text fields to expand to fill empty space.
 * 
 * @author 	sruffner
 */
public class JFixedWidthTextField extends JTextField
{
   private static final long serialVersionUID = 1L;
   private static final int DEF_FIXED_WIDTH = 100;
	private int fixedW = DEF_FIXED_WIDTH;

	/**
	 * Retrieve the current fixed width of text field in pixels. 
	 * 
	 * @return 	fixed text field width in pixels
	 */
	public int getFixedWidth() { return( fixedW ); }

	/**
	 * Change the fixed width of this text field.  The field and its parent hierarchy are laid out again as a result.
	 * 
	 * @param 	w the new fixed width
	 * @throws 	IllegalArgumentException if specified width is <= 0
	 */
	public void setFixedWidth( int w ) throws IllegalArgumentException
	{
		if( w <= 0 ) throw new IllegalArgumentException( "Width must be > 0" );
		if( w != fixedW )
		{
			fixedW = w;
			invalidate();
		} 
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		d.width = fixedW;
		return( d );
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getMaximumSize()
	 */
	public Dimension getMaximumSize()
	{
		Dimension d = super.getMaximumSize();
		d.width = fixedW;
		return( d );
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getMinimumSize()
	 */
	public Dimension getMinimumSize()
	{
		Dimension d = super.getMinimumSize();
		d.width = fixedW;
		return( d );
	}

}
