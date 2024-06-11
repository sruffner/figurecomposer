package com.srscicomp.common.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.srscicomp.common.util.Utilities;

/**
 * A collection of static GUI-related utility methods and constants. Any utility methods that require javax.swing or 
 * user interface components in java.awt should go here; put non-GUI utility methods in {@link 
 * Utilities}.
 * 
 * @author sruffner
 */
public class GUIUtilities
{
   /** Mnemonic for the Command (Mac) or Control key (otherwise); for composing hot key mnemonics. */
   public static final String MODCMD = Utilities.isMacOS() ? "\u2318" : "Ctrl-";
   /** Mnemonic for the Shift key; for composing hot key mnemonics. */
   public static final String MODSHIFT = Utilities.isMacOS() ? "\u21E7" : "Shift-";
   /** Mnemonic for the Ctrl+Shift keys; for composing hot key mnemonics. */
   public static final String MODCTRLSHIFT = Utilities.isMacOS() ? "\u2303\u21E7" : "Ctrl-Shift-";
   /** Mnemonic for the Alt key (Option key on Mac OSX); for composing hot key mnemonics. */
   public static final String MODALT = Utilities.isMacOS() ? "\u2325" : "Alt-";

   /**
    * Installs the native look and feel on the system if possible.
    */
   public static void initLookAndFeel()
   {
      String sysLAFName = UIManager.getSystemLookAndFeelClassName();
      try
      {
         UIManager.setLookAndFeel( sysLAFName );
      }
      catch( Exception e )
      {
         System.out.println( "Unable to install native look and feel; using Java L&F" );
      }
   }

   /**
    * This convenience method creates an image icon from the resource file specified by a package-relative filename.
    * 
    * @param c Class identifies the package in which the resource file is located.
    * @param path Filename of resource file relative to the class's package.
    * @param description A brief text description of the icon.
    * @return The image icon.  If unable to load the image, <code>null</code> is returned.
    */
   @SuppressWarnings("rawtypes")
   public static ImageIcon createImageIcon(Class c, String path, String description) 
   {
      java.net.URL imgURL = c.getResource(path);
      if( imgURL != null ) 
         return( new ImageIcon(imgURL, description) );
      else 
      {
         System.err.println("Couldn't find icon resource file: " + path);
         return( null );
      }
   }

   /**
    * This convenience method may be used to adjust the initial location of a pop-up window to ensure it stays on
    * screen. To deal with the possibility of a Windows-style task bar or a Mac-style dock bar or menu bar using part of
    * the screen real estate, the method forces the pop-up to stay at least 100 pixels away from any screen edge. This 
    * should work fine as long as the pop-up panel itself is small compared to the screen.
    * @param pUL Upper-left corner of pop-up window. The method adjusts the coordinates as described above -- if 
    * necessary. No action taken if null.
    * @param popupSz The size of the pop-up window. No action taken if null.
    */
   public static void adjustULToFitScreen(Point pUL, Dimension popupSz)
   {
      if(pUL == null || popupSz == null) return;
      adjustULToFitScreenEx(pUL, popupSz.width, popupSz.height, 100);
   }
   
   /**
    * This convenience method may be used to adjust the initial location of a pop-up window to ensure it stays on
    * screen. 
    * @param pUL Point contains initial coordinates of pop-up window's UL corner. Upon return, the coordinates are
    * adjusted in an attempt to ensure the window will be entirely on screen, assuming the window is smaller than the
    * current screen dimensions. No action taken if null.
    * @param popupW Width of pop-up window in pixels.
    * @param popupH Height of pop-up window in pixels.
    * @param margin If non-zero, pop-up window coordinates are computed to ensure that popup window lies this many
    * pixels inside the screen rectangle on all sides (if possible). Range-restricted to [0..100].
    */
   public static void adjustULToFitScreenEx(Point pUL, int popupW, int popupH, int margin)
   {
      if(pUL == null) return;
      int m = Utilities.rangeRestrict(0, 100, margin);
      
      Dimension screenSz = Toolkit.getDefaultToolkit().getScreenSize();
      screenSz.height -= m;
      screenSz.width -= m;
      if(pUL.y < m) pUL.y = m;
      if(pUL.y + popupH > screenSz.height) pUL.y = screenSz.height - 1 - popupH;
      if(pUL.x < m) pUL.x = m;
      if(pUL.x + popupW > screenSz.width) pUL.x = screenSz.width - 1 - popupW;
   }
}
