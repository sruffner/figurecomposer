package com.srscicomp.common.ui;

import java.io.File;
import java.util.List;

/**
 * This interface defines the required functionality of a Java application so that it can use {@link OSXAdapter} to 
 * handle file-open events from Mac OSX (outside of the application's own file-open command mechanisms -- eg, when
 * double-clicking a file that the application is registered to handle), as well as three standard items in the Mac OSX 
 * "Application" menu: the "About", "Preferences", and "Quit" menu commands. All application objects implementing this
 * interface must handle the "About" and "Quit" menu events; support for handling the "Preferences" menu event or a
 * file-open event is optional.
 * 
 * @author 	sruffner
 */
public interface OSXCompatibleApp
{
   /**
    * The application's handler for the Mac OSX "Quit" menu. This handler should perform any clean-up, save unsaved
    * documents (perhaps interacting with user), and exit the application with {@link System#exit(int)} -- in which case
    * case the handler never returns. If the quit is cancelled due to an interaction with the user, then the handler 
    * returns and <code>OSXAdapter</code> will cancel the "Quit" action. 
    */
   void doExit();

   /** The application's handler for the Mac OSX "About" menu. Typically, this raises a modal "About" dialog. */
   void doAbout();

   /**
    * The application's handler for the Mac OSX "Preferences" menu. Typically, this raises a modal dialog in which
    * application preferences can be viewed and modified.
    */
   void doPreferences();
   
   /** 
    * Does the application handle the "Preferences" item in the Mac OSX application menu? If not, that item is disabled 
    * by <code>OSXAdapter</code>.
    * @return True iff application implements the "Preferences" menu item.
    */
   boolean hasPreferences();
   
   /**
    * The application's handler for a file-open event from OS X. It is possible that file list could contain more than
    * one file. The application should open at least the first file in the list; it may choose to ignore the other 
    * entries if opening multiple files is not appropriate for that application.
    * <p>NOTE that OS X will only send the file-open event for file types specified in the <b>CFBundleDocumentTypes</b> 
    * key of its <code>Info.plist</code> file.</p>
    * @param files The list of files to open. Each entry should contain the full path for the file to be opened.
    */
   void doFileOpen(List<File> files);
   
   /**
    * Does the application handle file-open events from OSX? The application must specify the file types it can handle
    * in the <b>CFBundleDocumentTypes</b> key of its <code>Info.plist</code> file, and provide a valid implementation 
    * for {@link #doFileOpen(List)}.
    * @return True if application handles file-open events for recognized file types; false otherwise.
    */
   boolean supportsFileOpenEvents();
}
