package com.srscicomp.common.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;

/**
 * A {@link Runnable} that serves to show the main frame window of an application and optionally sets its initial
 * bounds. 
 * 
 * <p>This class is adapted from the recommendations of 
 * <a href="http://java.sun.com/developer/JDCTechTips/2003/tt1208.html#1">Core Java Technologies Tip</a> dated 
 * 12/8/2003, entitled "Multithreading in Swing".  The article recommends that the main frame window of the application 
 * be shown on the event dispatch thread to avoid potential subtle deadlocks.</p> 
 * 
 * @author sruffner
 */
public class MainFrameShower implements Runnable
{
   /** Construct a runnable thread that validates then shows the specified frame window. */
	public MainFrameShower(JFrame frame) { this(frame, null); }
	
	/** 
	 * Construct a runnable thread that sets the initial bounding rectangle for the specified frame window, then
	 * validates and shows the window.
	 * @param frame The frame window.
	 * @param bounds The desired initial bounding rectangle for the frame window.
	 */
	public MainFrameShower(JFrame frame, Rectangle bounds)
	{
	   this.frame = frame;
	   this.initBounds = bounds;
	}
	
	public void run() 
	{ 
	   if(frame != null)
	   {
	      frame.pack();
	      if(initBounds != null)
	      {
	         Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
	         if(initBounds.x < 0) initBounds.x = 0;
	         if(initBounds.y < 0) initBounds.y = 0;
	         if(initBounds.width > sz.width) initBounds.width = sz.width;
	         if(initBounds.height > sz.height) initBounds.height = sz.height;
	         
	         int state = java.awt.Frame.NORMAL;
	         if(initBounds.width == sz.width) state |= java.awt.Frame.MAXIMIZED_HORIZ;
	         if(initBounds.height == sz.height) state |= java.awt.Frame.MAXIMIZED_VERT;
	         
	         frame.setBounds(initBounds);
	         frame.setExtendedState(state);
	         frame.validate();
	      }
	      frame.setVisible(true);
	   }
	}
	
	private final JFrame frame;
	private final Rectangle initBounds;
}
