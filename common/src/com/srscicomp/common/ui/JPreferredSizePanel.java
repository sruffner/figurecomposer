package com.srscicomp.common.ui;

import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * <p>JPreferredSizePanel is a simple extension of {@link JPanel} intended for packaging components at
 * their PREFERRED size.  It overrides the methods {@link javax.swing.JComponent#getMinimumSize()} and {@link 
 * javax.swing.JComponent#getMaximumSize()} so that they simply return the preferred size of the panel.  It supports 
 * all the different JPanel constructors.</p>
 * 
 * <p>This class is merely here to deal with one difficulty of laying out GUIs in Swing -- the tendency of many 
 * components to expand to fill empty space.</p>
 * 
 * @author 	sruffner
 */
public class JPreferredSizePanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   private boolean enforceW = true;
	private boolean enforceH = true;

	/**
	 * @see JPanel#JPanel(LayoutManager,boolean)
	 */
	public JPreferredSizePanel(LayoutManager layout, boolean isDoubleBuffered)
	{
		super(layout, isDoubleBuffered);
	}

	/**
	 * @see JPanel#JPanel(LayoutManager)
	 */
	public JPreferredSizePanel(LayoutManager layout)
	{
		super(layout);
	}

	/**
	 * @see JPanel#JPanel(boolean)
	 */
	public JPreferredSizePanel(boolean isDoubleBuffered)
	{
		super(isDoubleBuffered);
	}

	/**
	 * @see JPanel#JPanel()
	 */
	public JPreferredSizePanel()
	{
		super();
	}

	/**
	 * Use this constructor to create a panel that enforces the preferred size only along one of its dimensions.  All 
	 * other constructors enforce the preferred size along both dimensions by default. 
	 * 
	 * @param 	enforceW If set, then min and max width are always set to the preferred width.
	 * @param 	enforceH If set, then min and max height are always set to the preferred height.
	 */
	public JPreferredSizePanel( boolean enforceW, boolean enforceH )
	{
		super();
		this.enforceW = enforceW;
		this.enforceH = enforceH;
	}

	/**
	 * If preferred width is enforced, min width is set to preferred width. Analogously for min height.
	 * 
	 * @see javax.swing.JComponent#getMinimumSize()
	 */
	public Dimension getMinimumSize() 
	{
		Dimension prefD = this.getPreferredSize();
		Dimension minD = super.getMinimumSize();
		if( enforceW ) minD.width = prefD.width;
		if( enforceH ) minD.height = prefD.height;
		
		return( minD );
	}

	/**
	 * If preferred width is enforced, max width is set to preferred width. Analogously for max height.
	 * 
	 * @see javax.swing.JComponent#getMaximumSize()
	 */
	public Dimension getMaximumSize() 
	{
		Dimension prefD = this.getPreferredSize();
		Dimension maxD = super.getMaximumSize();
		if( enforceW ) maxD.width = prefD.width;
		if( enforceH ) maxD.height = prefD.height;
		
		return( maxD );
	}
}
