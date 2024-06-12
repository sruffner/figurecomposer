package com.srscicomp.fc.uibase;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import com.srscicomp.common.g2dviewer.RenderableModel;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSrcFactory;
import com.srscicomp.fc.fig.FGNPreferences;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fypml.FGModelSchema;
import com.srscicomp.fc.matlab.MatlabFigureImporter;
import com.srscicomp.fc.uibase.DirWatcher.Event;


/**
 * <code>FCWorkspace</code> manages the current user's <i>Figure Composer (FC)</i> workspace. The workspace is intended 
 * to provide convenient access to various <i>FC</i>-specific files that the user has created and stored on the host 
 * system:
 * <ul>
 * <li>FypML figure definition files (*.fyp)</li>
 * <li>Matlab figure files (*.fig) -- because these can sometimes be imported successfully as FypML files.</li>
 * <li>Custom-formatted binary or plain-text data set source files (*.dnr, *.dnb, *.dna, *.txt)</li>
 * </ul>
 * The workspace manager keeps track of file-system path information for these kinds of files found on the host machine 
 * -- the "workspace path cache". It also manages the user's application preferences, and maintains most recently used
 * used lists of figures (either .fyp files or Matlab .fig files) and data sets.
 * 
 * <p><b>FCWorkspace</b> is a singleton class, as it is intended to represent the current user's workspace throughout 
 * the application's runtime. Between runs, the workspace is persisted in the ".figurecomposer" directory under the 
 * user's home directory. Application preferences are stored in a properties file called "settings.txt", while the
 * "path cache" is kept in a file called "paths.txt". The latter file also includes a list of the most recently used 
 * figure definition or data set source files.</p>
 * 
 * <p>Static functions in <b>FCWorkspace</code> also provide access to some general information about the application
 * including application title, version info, copyright, and contact email address.</p>
 * 
 * <p>One workspace setting -- the screen resolution in pixels per inch -- is exposed as a user-controlled setting only
 * because there is no cross-platform support for getting the actual resolution from the OS. We have to rely on the
 * user to set the resolution accurately. It is exposed as a bound property since changes in its value may affect the 
 * size of components (like the canvas on which a figure is rendered) in the user interface.</p>
 * 
 * <h3>Background tasks update path cache</h3>
 * Once the workspace manager has loaded the path cache from "paths.txt", it launches a directory watching service,
 * {@link DirWatcher} to monitor all directories currently in the cache (whether they exist at startup or not). Based on
 * the Java <b>WatchService</b> API, this service runs a background daemon thread which reports any changes in the path
 * cache directories -- files added or removed, existing directory "disappearing" (eg, a directory on a networked drive
 * when the network goes down), or missing directory "reappearing". The workspace manager updates its path cache in
 * response to these notifications. [<b>NOTE</b>: As of Feb 2023, the WatchService implementation for MacOSX uses a 
 * fallback polling implementation rather than MacOSX's native file system events API, so there can be delays of a few 
 * seconds before the workspace manager receives notification of changes in the monitored directories.]
 * 
 * <h3>Obsolete: <i>DataNav Builder</i> settings and related files</h3>
 * <p>FC was originally part of a larger application suite, <i>DataNav</i>, which implemented a data-sharing portal. The
 * portal application was never put to use, and we decided to fork FC as its own stand-alone application. At the same 
 * time, we redefined the FC workspace to only include FC-specific information. If FC (as of v5.0.3) finds that the
 * FC workspace directory is missing, it will check for the old DataNav workspace directory (.datanavx) and copy all
 * FC-specific content and settings to the FC workspace directory, without altering the DataNav workspace content.</p>
 * 
 * <h3>Obsolete: The figure thumbnail cache</h3>
 * <p>In release v4.1.1, the workspace manager was modified to generate and maintain small thumbnail images (in PNG 
 * format) of all figures in the path cache; these were stored as individual files in the "thumbs" subfolder of the 
 * user's workspace directory. This figure thumbnail cache supported the "figure finder", a view controller that was
 * part of <i>DataNav Builder</i>'s "My Figures" perspective. However, the figure finder was removed in V4.4.0 when the 
 * workspace browser component, {@link WSFigBrowser}, was updated to support operations like renaming, copying and 
 * deleting individual figure files. At the same time, the thumbnail cache feature was removed from the workspace
 * manager implementation. At startup, the "thumbs" folder (if present), is removed from the workspace directory.</p>
 * 
 * <h3>Obsolete: The style palette</h3>
 * <p>In release v4.4.1, the style palette was introduced with the copy/paste styling commands. The styling-related
 * properties of a graphic object -- its style set -- were saved to a "palette" of stylings, which the user could then
 * access to apply to another object. The 20 most recent copied style sets were maintained in the file palette.json in
 * the user's workspace so they would be preserved between runtime sessions. However, the palette did not prove useful, 
 * as it was difficult to present a clear summary of a style set to the user. While the copy/paste styling commands 
 * remain, the style palette was deprecated in v4.7.1. The palette.json file is silently removed by v4.7.1+.</p>
 * 
 * @author sruffner
 */
public class FCWorkspace implements DirWatcher.Listener
{
   //
   // Application information (extracted from a properties file in the resources package).
   //

   /** 
    * Absolute path to resource file containing general information for Figure Composer. <b>NOTE: Can use relative path
    * ONLY if resources package is under this class's path. Cannot use "../" to denote a parent package. This may work
    * when run from IDE, but not when app is packaged as an executable JAR!</b> 
    */
   private final static String APPINFO_FILE = "/com/srscicomp/fc/resources/appinfo.properties";

   /** Application info, loaded from a properties file at class-load time. */
   private static Properties appInfo = null;

   static 
   {
      appInfo = new Properties();
      try 
      { 
         appInfo.load( FCWorkspace.class.getResourceAsStream(APPINFO_FILE) ); 
      }
      catch( IOException ioe ) {}
   }

   /**
    * Returns the official name of the application, as defined in the application information properties file.
    * @return The application title.
    */
   public final static String getApplicationTitle() { return(appInfo.getProperty("title", "FigureComposer")); }

   /**
    * Returns the current version number for this application as a string in the form "M.m.r", where M, m, and r are 
    * integers indicating the major version #, minor version #, and revision #, respectively. The three version numbers 
    * are retrieved from the application information properties file (keys "major", "minor" and "revision").
    * @return Current version, as described (fallback default = "5.4.0").
    */
   public final static String getApplicationVersion()
   {
      String major = appInfo.getProperty("major", "5");
      String minor = appInfo.getProperty("minor", "4");
      String rev = appInfo.getProperty("revision", "0");
      return( major + "." + minor + "." + rev );
   }

   /**
    * Returns the current version number of this application as an integer = M*100+m*10+r, where M, m, and r are 
    * integers indicating the major version #, minor version #, and revision #, respectively. The three version numbers 
    * are retrieved from the application information properties file (keys "major", "minor" and "revision").
    * @return Current version number, as described (fallback default = 540).
    */
   public final static int getApplicationVersionAsInteger()
   {
      int vnum = 503;
      try
      {
         int major = Integer.parseInt(appInfo.getProperty("major", "5"));
         int minor = Integer.parseInt(appInfo.getProperty("minor", "4"));
         int rev = Integer.parseInt(appInfo.getProperty("revision", "0"));
         vnum = major*100 + minor*10 + rev;
      }
      catch(NumberFormatException nfe) {}
      
      return(vnum);
   }

   /**
    * Returns the release date for last official version of the application, in the form "DD Mmm YYYY". The version 
    * date is extracted from the application information properties file, via key "versiondate".
    * @return Release date string, as described (fallback default = "release date unknown").
    */
   public final static String getApplicationVersionReleaseDate()
   {
      return(appInfo.getProperty("versiondate", "release date unknown"));
   }

   /**
    * Returns a short string containing a copyright line for this application, as defined by the "copyrightline" key in 
    * the application information properties file.
    * @return Copyright string, as described.
    */
   public final static String getCopyrightLine()
   {
      return(appInfo.getProperty("copyrightline", "\u00A9 Copyright 2003-2023 HHMI/Lisberger Lab"));
   }

   /**
    * Returns the email address to which inquiries about this application should be sent, as defined by the "mailto" key
    * in the application information properties file.
    * @return Email address, as described.
    */
   public final static String getEmailAddress()
   {
      return(appInfo.getProperty("mailto", "lisberger@neuro.duke.edu"));
   }

   
   /**
    * Get the current user's <i>Figure Composer</i> workspace.
    * <p>This method is intended to be called early during application startup, prior to raising the application frame
    * window. On first call, it creates and loads the singleton instance. On all future calls, it simply returns that
    * instance. If the workspace is successfully loaded, a shutdown hook is registered which will save any changes in
    * workspace state (if possible) prior to shutdown.</p>
    * @return The current user's workspace. Returns null if unable to load workspace, in which case application should 
    * report the failure and exit.
    */
   public static FCWorkspace getInstance()
   {
      if(workspace == null) 
      {
         workspace = new FCWorkspace();
         if(!workspace.onStartup()) workspace = null;
         else Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run()
            {
               if(workspace != null) workspace.onExit();
            }
         });
      }
      return(workspace);
   }
   
   /** The singleton instance. */
   private static FCWorkspace workspace = null;
   
   /** Private constructor. Use {@link #getInstance()}. */
   private FCWorkspace() { }
   
   /** The workspace directory. */
   private File home = null;
   
   /** The <i>Figure Composer</i> workspace directory, a sub-directory of the user's home directory on file system. */
   private final static String WSDIR = ".figurecomposer";
   
   /** 
    * The obsolete <i>DataNav</i> workspace directory, a sub-directory of user's home directory on file system. If the
    * new FC-specific workspace directory does not exist, FC-specific workspace information is copied from this old
    * workspace directory, if possible.
    */
   @Deprecated private final static String OLD_DATANAV_WSDIR = ".datanavx";
   
   /** 
    * Load the user's workspace object from files stored in a dedicated workspace directory in the current user's home 
    * directory. This is called when the singleton workspace instance is initially constructed, which should happen 
    * during application startup.
    * 
    * <p>All application preferences are maintained in a file named "settings.txt", while the pathnames of all known 
    * FC-specific files are stored in a file named "paths.txt", both in the workspace directory "$HOME/{@link #WSDIR}", 
    * where $HOME is the user's home directory. The former is a Java properties file, the latter a plain-text file in an
    * internal but relatively straightforward format. Nevertheless, these files should not be edited by hand --
    * unless you know what you're doing!</p>
    * 
    * <p><b>The workspace will fail to load in a sand-boxed security environment, as it requires access to the user's
    * home directory.</b></p>
    * 
    * @return True if workspace was initialized and loaded successfully, false otherwise -- in which case the workspace
    * is unavailable.
    */
   private boolean onStartup()
   {
      // initialize pathname to the FC workspace directory within the current user's home directory. Create it if it
      // does not yet exist. This operation must succeed, or workspace is unavailable.
      home = null;
      try
      {
         home = new File(System.getProperty("user.home"), WSDIR);
         if(!home.isDirectory()) 
         {
            if(!home.mkdir()) home = null;
         }
      }
      catch(SecurityException se) { home = null; }
      if(home == null) return(false);
      
      settingsFile = new File(home, SETTINGSFILENAME);
      pathCacheFile = new File(home, PATHCACHEFILENAME);
      
      // if deprecated style palette file present, remove it (deprecated as of v4.7.1)
      File f = new File(home, PALETTEFILENAME);
      if(f.isFile()) f.delete();
      
      loadSettings();
      loadPathCache();
      removeObsoleteThumbCacheDir();
      startPathCacheMonitor();
      return(true);
   }
   
   /** 
    * Name of JSON file in user's workspace directory that stores user's style palette between runtime sessions. 
    * 18jun2015 (for v4.7.1) : Permanently removed support for the style palette, which proved not useful. Kept this
    * filename to ensure the JSON style palette file could be removed from user's workspace.
    */
   @Deprecated private final static String PALETTEFILENAME = "palette.json";

   /**
    * Helper method for {@link #onStartup()}. It removes the obsolete figure thumbnail image cache directory -- the
    * subdirectory "thumbs" within the workspace directory. The thumbnail image cache was eliminated in V4.4.0.
    */
   private void removeObsoleteThumbCacheDir()
   {
      File thumbDir = new File(home, "thumbs");
      if(thumbDir.isDirectory())
      {
         File[] files = thumbDir.listFiles();
         if(files != null) for(File f : files) f.delete();
         
         thumbDir.delete();
      }
   }
   
   /**
    * This method is called by the shutdown hook installed when the workspace was loaded. It ensures that any background
    * tasks are cancelled and persists any changes in application settings and the path cache to the relevant files in 
    * the workspace directory.
    */
   private void onExit()
   {
      stopPathCacheMonitor();
      saveSettings();
      savePathCache();
   }

   /** 
    * Get the file system location of the user's workspace on the host machine.
    * @return The home directory for the user's workspace.
    */
   public File getWorkspaceHome() { return(home); }
   
   
   //
   // User-controlled application settings
   //
   
   /** Key for the screen resolution in dots per inch (an integer). This is a bound property. */
   public final static String KEY_DPI = "screen_dpi";
   
   /** Key for FigureComposer frame window bounds (an integer array <i>[xUL yUL W H]</i>, in screen pixels). */
   public final static String KEY_FRAMEBOUNDS_FC = "fc.bounds";
   
   /** 
    * Key for DataNav Builder frame window bounds (int array <i>[xUL yUL W H]</i>, in screen pix). 
    * <p><b>Deprecated as of v5.0.3.</b> The <i>DataNav Builder</i> application is no longer being developed.</p>
    */
   @Deprecated public final static String KEY_FRAMEBOUNDS_DN = "pb.bounds";
   
   /** 
    * Key for list of <i>FypML</i> figure model definition files (or Matlab FIG files) that were open when the 
    * <i>FigureComposer</i> application last shutdown. The value under this key is a list of absolute file paths 
    * separated by the line feed character "\n".
    */
   public final static String KEY_OPENFIGS = "openFigs";
   
   /** Key for the FC workspace browser's file sorting preference. Value is an integer string in [0..4]. */
   public final static String KEY_WSFILE_SORT = "wsSort";
   
   /**
    * <i>[As of v4.3.2]</i> Key for portal connections defined in <i>DataNav Builder</i> at last shutdown. Value is a 
    * semicolon-separated list of up to 4 portal connection tokens. Each such token, in turn, is a comma-separated list 
    * of five tokens that define a connection (in order): the portal host name, the path to the portal server on that 
    * host, the HTTPS port number for secure communications with the portal server, the HTTP port number for anonymous 
    * communications, and the username for the most recent login attempt (this may be an empty string).
    * <p><b>Deprecated as of v5.0.3.</b> The <i>DataNav Builder</i> application is no longer being developed.</p>
    */
   @Deprecated public final static String KEY_DB_PORTALS = "db.portals";
   /**
    * <i>[As of v4.3.2]</i> Key holding identity of the portal connection that was selected in <i>DataNav Builder</i>
    * when the app last shutdown. Value is an integer string that is an index into the list of portal connections 
    * stored under the {@link #KEY_DB_PORTALS} key.
    * <p><b>Deprecated as of v4.5.5</b></p>
    */
   @Deprecated public final static String KEY_DB_SELPORTAL = "db.selportal";
   /** 
    * <i>[As of v4.3.2]</i> Key identifying which perspective was in use when <i>DataNav Builder</i> last shutdown, so
    * so that it can be restored in that perspective on next launch. Its value is set by the <i>Builder</i>'s master 
    * view controller and is not checked by the workspace manager.
    * <p><b>Deprecated as of v4.5.5</b></p>
    */
   @Deprecated public final static String KEY_DB_PERSPECTIVE = "db.perspective";
   /** 
    * [07feb2023] This key identified a user setting applicable to some incarnation of the <i>DataNav</i> application 
    * suite that has long been abandoned. It's been carried over in the user settings files for a long time. I've
    * defined it here to ensure the key is removed permanently if it is found in the user settings file.
    */
   @Deprecated public final static String KEY_VIEW_CONTROLLER = "viewController";
   
   /** 
    * <i>[As of v4.1.1]</i> Key containing the full pathname of the figure file selected for editing when application 
    * last shutdown. On next launch it will be selected initially (if possible).
    */
   public final static String KEY_FC_SELECTED = "fc.selected";
   /** <i>[As of v4.1.4]</i> Key containing the on/off state ("true" or "false") of the figure tree view within FC. */
   public final static String KEY_FC_TREEVIEWON = "fc.treeon";
   /** <i>[As of v4.2.3]</i> Key containing the on/off state ("true" or "false") of the workspace browser within FC. */ 
   public final static String KEY_FC_WSFIGBROWSERON = "fc.wsfbon";
   /**
    * <i>[As of v4.7.1]</i> Key containing the locations of the two split pane dividers in the workspace browser within
    * FC. The expected value is a comma-separated list of 2 string tokens. These are integer strings indicating the 
    * locations of the two split pane dividers; the first is for the main split pane; the second is for the nested split
    * pane containing the figure preview (canvas + notes).
    */
   public final static String KEY_FC_WSFBDIV = "fc.wsfbdiv";
   /** <i>[As of v4.3.1]</i> Key containing a string specifying FC's toolbar state. */
   public final static String KEY_FC_TOOLSTATE = "fc.toolstate";
   
   
   /** 
    * Absolute pathname for resource file containing default user settings. <b>NOTE: Can use relative path ONLY if
    * resources package is under this class's path. Cannot use "../" to denote a parent package. This may work when run 
    * from IDE, but not when app is packaged as an executable JAR!</b> 
    */
   private final static String DEFSETTINGS_FILE = "/com/srscicomp/fc/resources/defaultSettings.properties";

   /** Name of properties file in the workspace directory that holds the user's current FC application preferences. */
   private final static String SETTINGSFILENAME = "settings.txt";

   /** The workspace properties file that holds the user's FC application preferences. */
   private File settingsFile = null;
   
   /** The user's current FC application preferences. */
   private Properties currentSettings = null;
   
   /** 
    * Helper method loads FC application preferences from the settings file in the current user's workspace. 
    * 
    * <p>If the settings file does not exist, the method will attempt to copy the settings file from the obsolete 
    * <i>DataNav</i> workspace directory, if present. If a settings file cannot be found, all application preferences 
    * are set to default values. NOTE that this method does NOT attempt to import old <i>Phyplot 2.1</i>-style settings,
    * as was done through version 3.6.2.</p>
    */
   private void loadSettings()
   {
      Properties defSettings = new Properties();
      try { defSettings.load(FCWorkspace.class.getResourceAsStream(DEFSETTINGS_FILE)); }
      catch(IOException ioe) { throw new Error("Unable to retrieve default settings; JAR corrupted?"); }

      currentSettings = new Properties(defSettings);
      
      // if no settings file, copy from old DataNav workspace, if possible.
      if(!settingsFile.exists()) copyOldSettingsFile();
      
      if(settingsFile.exists())
      {
         FileInputStream in = null;
         try
         {
            in = new FileInputStream(settingsFile);
            currentSettings.load(in);
            FGNPreferences.getInstance().load(currentSettings);
         } 
         catch(IOException ioe) {}
         finally { try{ if(in != null) in.close(); } catch(IOException ioe) {} }
         
         // make sure deprecated settings have been removed
         currentSettings.remove(KEY_DB_PERSPECTIVE);
         currentSettings.remove(KEY_DB_SELPORTAL);
         currentSettings.remove(KEY_FRAMEBOUNDS_DN);
         currentSettings.remove(KEY_DB_PORTALS);
         currentSettings.remove(KEY_VIEW_CONTROLLER);
      }

      screenDPI = Toolkit.getDefaultToolkit().getScreenResolution();
      try {screenDPI = Float.parseFloat(currentSettings.getProperty(KEY_DPI));} catch(NumberFormatException nfe) {}
      screenDPI = Math.max(MIN_DPI, Math.min(screenDPI, MAX_DPI));
   }
   
   /**
    * Copy the old <i>DataNav</i> workspace settings file to the <i>Figure Composer</i> workspace directory. No action
    * taken if a settings file already exists in the FC workspace directory. Fails silently (old settings file not 
    * found, or copy operation failed).
    */
   private void copyOldSettingsFile()
   {
      if(settingsFile.exists()) return;
      
      File oldSettingsFile = null;
      try
      {
         File oldHome = new File(System.getProperty("user.home"), OLD_DATANAV_WSDIR);
         if(oldHome.isDirectory()) 
         {
            oldSettingsFile = new File(oldHome, SETTINGSFILENAME);
            Files.copy(oldSettingsFile.toPath(), settingsFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
         }
      }
      catch(SecurityException se) {}
      catch(IOException ioe) {}
   }
   
   /** 
    * Helper method saves FC application preferences to current user's workspace. If an IO error occurs, the method 
    * fails silently. The current preference values are first saved to a temporary file to minimize the chance of 
    * corrupting the current settings file.
    */
   private void saveSettings()
   {
      File tmpFile = new File(settingsFile.getAbsolutePath() + ".tmp");
      FileOutputStream out = null;
      try
      {
         FGNPreferences.getInstance().save(currentSettings);
         currentSettings.setProperty(KEY_DPI, Utilities.toString(screenDPI, 5, 1));
         
         out = new FileOutputStream(tmpFile);
         currentSettings.store(out, " Current user-defined FigureComposer settings");
         out.close();
         out = null;
         
         settingsFile.delete();
         tmpFile.renameTo(settingsFile);
      }
      catch(IOException ioe) {}
      finally
      {
         try { if(out != null) out.close(); } catch(IOException ioe) {}
      }
   }
   
   
   /** Minimum value for the screen resolution in dots per inch. */
   public final static float MIN_DPI = 50;

   /** Maximum value for the screen resolution in dots per inch. */
   public final static float MAX_DPI = 300;

   /** The current screen resolution in dots per inch. */
   private float screenDPI = MIN_DPI;
   
   /**
    * Get the screen resolution used by <i>Figure Composer</i> to convert pixels to inches.
    * @return Screen resolution in pixels per inch.
    */
   public float getScreenDPI() { return(screenDPI); }
   
   /**
    * Set the screen resolution used by <i>Figure Composer</i> to convert pixels to inches. Any registered workspace 
    * listeners are notified of the change (on the event dispatch thread).
    * @param dpi New screen resolution in pixels per inch. Range-limited to <code>[MIN_DPI..MAX_DPI]</code> and saved 
    * with a single decimal digit's precision.
    */
   public void setScreenDPI(float dpi)
   {
      float oldDPI = screenDPI;
      screenDPI = (dpi<MIN_DPI) ? MIN_DPI : ((dpi>MAX_DPI) ? MAX_DPI : dpi);
      screenDPI = Math.round(10f * screenDPI);
      screenDPI = screenDPI / 10.0f;
      
      if(Math.abs(screenDPI - oldDPI) > 0.05f)
      {
         notifyListeners(EventID.SCREENRES, null, null);
      }
   }
   
   /** Default application frame window bounds. */
   private final static Rectangle DEFBOUNDS = new Rectangle(10, 10, 700, 700);
   
   /** Minimum value for the width or height of the application frame window. */
   private final static int MIN_BOUNDS_WH = 500;

   /**
    * Get the preferred screen bounds for <i>Figure Composer</i>'s main frame window.
    * @return The preferred frame window bounds -- (x,y) of upper left corner, width, height in screen pixels.
    */
   public Rectangle getFrameBounds()
   {
      try
      {
         StringTokenizer st = new StringTokenizer(currentSettings.getProperty(KEY_FRAMEBOUNDS_FC));
         int x = Integer.parseInt(st.nextToken());
         int y = Integer.parseInt(st.nextToken());
         int w = Integer.parseInt(st.nextToken());
         int h = Integer.parseInt(st.nextToken());
         return(new Rectangle(x, y, w, h));
      }
      catch(Throwable t) {}
      
      return(DEFBOUNDS);
   }
   
   /**
    * Set the preferred screen bounds for <i>Figure Composer</i>'s  main frame window.
    * @param r Desired frame window bounds -- (x,y) of upper left corner, width, height in screen pixels. The preferred 
    * screen bounds will not be updated if either x or y is negative, or if either dimension is less than 500.
    */
   public void setFrameBounds(boolean which, Rectangle r)
   {
      if(r==null || r.x < 0 || r.y < 0 || r.width < MIN_BOUNDS_WH || r.height < MIN_BOUNDS_WH)
         return;
      currentSettings.setProperty(KEY_FRAMEBOUNDS_FC, "" + r.x + " " + r.y + " " + r.width + " " + r.height);
   }

   /**
    * Get the list of figure files that were open in <i>Figure Composer</i> when user last shutdown the application.
    * @return List of abstract pathnames of figure files last open and that still exist on the host when this method is
    * invoked.
    */
   public List<File> getLastOpenFigures()
   {
      List<File> files = new ArrayList<File>();
      StringTokenizer tokenizer = new StringTokenizer(currentSettings.getProperty(KEY_OPENFIGS, ""), "\n");
      while(tokenizer.hasMoreTokens())
      {
         File f = new File(tokenizer.nextToken());
         if(f.isFile()) files.add(f);
      }
      return(files);
   }
   
   /**
    * Set the list of figure files that were open in <i>Figure Composer</i> when user last shutdown the application.
    * @param figFiles List of abstract pathnames of figure files when the <i>FigureComposer</i> last shutdown.
    */
   public void setLastOpenFigures(List<File> figFiles)
   {
      String paths = "";
      if(figFiles != null) for(int i=0; i<figFiles.size(); i++)
      {
         File f = figFiles.get(i);
         if(f != null)
         {
            if(paths.length() == 0) paths += f.getAbsolutePath();
            else paths += "\n" + f.getAbsolutePath();
         }
      }
      currentSettings.setProperty(KEY_OPENFIGS, paths);
   }
   
   /**
    * Enumeration of the different ways in which workspace directories or files may be sorted for presentation within
    * the <i>Figure Composer</i>'s workspace browser.
    * @author sruffner
    */
   public enum SortOrder
   {
      /** No sorting. Files/directories listed in the order they appear in workspace cache. */ 
      NONE("Unsorted"),
      
      /** Sort in ascending alphabetical order by filename. */ 
      BYNAME_ASCENDING("By name, A to Z"),
      /** Sort in descending alphabetical order by filename. */ 
      BYNAME_DESCENDING("By name, Z to A"),
      /** Sort in ascending order by last modified date (most recent last). */ 
      BYDATE_ASCENDING("By date, most recent last"),
      /** Sort in descending order by last modified date (most recent first). */ 
      BYDATE_DESCENDING("By date, most recent first");
      
      private SortOrder(String desc) {this.desc = desc;}
      
      /** Description of this file sorting order, for presentation in the user interface. */
      private String desc;
      
      /** 
       * Get a user-friendly description of this file sorting order preference, for presentation on the user interface.
       * @return Short description of the sorting order (25 characters or less).
       */
      public String getDescription() { return(desc); }
      
      /** Offers a user-friendly description of the sort order, for presentation on the user interface. */
      @Override public String toString() {  return(desc); }
      
      /**
       * Return the <b>SortOrder</b> instance identified by its string form as returned by {@link #name()}.
       * @param s The enumerant name.
       * @return The corresponding enumerant instance. If argument is invalid, returns {@link #NONE}.
       */
      public static SortOrder fromString(String s)
      {
         SortOrder so = SortOrder.NONE;
         try {so = SortOrder.valueOf(SortOrder.class, s); }
         catch(Exception e) { so = SortOrder.NONE; }
         return(so);
      }
   }
   
   /**
    * This comparator orders two {@link File} objects IAW the user's current sort order preference setting, as defined
    * by the {@link SortOrder} enumeration. It supports sorting alphabetically by file name or chronologically by the 
    * file's last modified time, in either ascending or descending order. Alphabetical sorting ignores case.
    * @author sruffner
    */
   public static class FileComparator implements Comparator<File>
   {
      public int compare(File o1, File o2)
      {
         SortOrder order = workspace.getFileSortOrder();
         if(order == SortOrder.BYNAME_ASCENDING || order == SortOrder.BYNAME_DESCENDING)
         {
            String name1 = o1.getName();
            String name2 = o2.getName();
            int res = name1.compareToIgnoreCase(name2);
            return(order == SortOrder.BYNAME_ASCENDING ? res : -res);
         }
         else 
         {
            long t1 = o1.lastModified();
            long t2 = o2.lastModified();
            int res = (t1 < t2) ? -1 : ((t1 > t2) ? 1 : 0);
            return(order == SortOrder.BYDATE_ASCENDING ? res : -res);
         }
      }
   }
   
   /** 
    * Get the preferred order in which workspace directories or files are listed in the workspace browser.
    * @return The preferred file sort order.
    */
   public SortOrder getFileSortOrder() { return(SortOrder.fromString(currentSettings.getProperty(KEY_WSFILE_SORT))); }
   
   /** 
    * Set the preferred order in which workspace directories or files are listed in the workspace browser.
    * @param The preferred file sort order. Rejected silently if null.
    */
   public void setFileSortOrder(SortOrder order)
   {
      if(order != null) currentSettings.setProperty(KEY_WSFILE_SORT, order.name());
   }

   /**
    * Get the figure file that was selected for editing in <i>Figure Composer</i> when the user last shutdown the 
    * application.
    * @return Abstract pathname of the selected figure file. Returns null if corresponding property not available in 
    * workspace settings, or if file path retrieved from the settings no longer exists when this method is invoked.
    */
   public File getLastSelectedFigure()
   {
      String path = currentSettings.getProperty(KEY_FC_SELECTED, "");
      if(path.isEmpty()) return(null);
      File f = new File(path);
      return(f.isFile() ? f : null);
   }
   
   /**
    * Set the figure file that was selected for editing in <i>Figure Composer</i> when the user last shutdown the 
    * application.
    * @param Abstract pathname of the selected figure file. The corresponding property in the workspace settings is set 
    * to the file's absolute pathname. If null, the property is set to an empty string.
    */
   public void setLastSelectedFigure(File f)
   {
      currentSettings.setProperty(KEY_FC_SELECTED, f==null ? "" : f.getAbsolutePath());
   }

   /**
    * Get the on/off state of the figure tree view in FC when the user last shutdown the application.
    * @return True if tree view should be visible; false if it should be hidden.
    */
   public boolean getFigComposerTreeViewOn()
   {
      String on = currentSettings.getProperty(KEY_FC_TREEVIEWON, "true");
      return("true".equals(on));
   }
   
   /**
    * Save the on/off state of the figure tree view in FC.
    * @param on True if tree view is visible, false if it is hidden.
    */
   public void setFigComposerTreeViewOn(boolean on)
   {
      currentSettings.setProperty(KEY_FC_TREEVIEWON, on ? "true" : "false");
   }
   
   /**
    * Get the on/off state of the workspace figures browser in FC when the user last shutdown the application.
    * @return True if workspace browser should be visible; false if it should be hidden.
    */
   public boolean getWSFigBrowserOn()
   {
      String on = currentSettings.getProperty(KEY_FC_WSFIGBROWSERON, "true");
      return("true".equals(on));
   }
   
   /**
    * Save the on/off state of the workspace figures browser in FC.
    * @param on True if workspace browser is visible, false if it is hidden.
    */
   public void setWSFigBrowserOn(boolean on)
   {
      currentSettings.setProperty(KEY_FC_WSFIGBROWSERON, on ? "true" : "false");
   }
   
   /**
    * Get the divider locations for the primary split pane and nested preview split pane within the workspace figures
    * browser in FC. These should be the locations when the user last shutdown the application.
    * @return A 2-element array containing the divider locations relative to their parent split pane. If not found in
    * workspace settings, returns [-1, -1].
    */
   public int[] getWSFigBrowserDivLocs()
   {
      int[] locs = new int[] {-1, -1};
      String[] strLocs = currentSettings.getProperty(KEY_FC_WSFBDIV, "").split(",");
      if(strLocs.length == 2)
      {
         try
         {
            locs[0] = Integer.parseInt(strLocs[0]);
            locs[1] = Integer.parseInt(strLocs[1]);
         }
         catch(NumberFormatException nfe) {}
      }
      return(locs);
   }
   
   /**
    * Save the divider locations for the primary split pane and nested preview split pane within the workspace figures
    * browser in FC. This should be called just prior to application exit.
    * @param mainDiv Location of the primary split pane's divider.
    * @param prvuDiv Location of the nested preview split pane's divider.
    */
   public void setWSFigBrowserDivLocs(int mainDiv, int prvuDiv)
   {
      String s = "" + mainDiv + "," + prvuDiv;
      currentSettings.setProperty(KEY_FC_WSFBDIV, s);
   }
   
   /**
    * Get the state of the <i>Figure Composer</i>'s tool bar when the user last shutdown the application.
    * @return A string defining the state. Its value is managed/validated by the caller, not by the workspace manager.
    * If the workspace settings key for this string does not yet exist, an empty string is returned.
    */
   public String getFigComposerToolbarState() { return(currentSettings.getProperty(KEY_FC_TOOLSTATE, "")); }
   
   /**
    * Save the state of <i>Figure Composer</i>'s toolbar.
    * @param state A string defining the state. This value is stored <b>as is</b> under the relevant key in the 
    * workspace settings; caller is responsible for ensuring the value is a valid one. Null == empty string.
    */
   public void setFigComposerToolbarState(String state)
   {
      currentSettings.setProperty(KEY_FC_TOOLSTATE, state == null ? "" : state);
   }
   
   
   //
   // MRU figure model and data set source files
   //
   
   /** 
    * Maximum number of entries in the MRU figure file list and the MRU data set source file list. The figure file
    * list may include .FYP or Matlab .FIG files.
    */
   private final static int MRU_MAXLEN = 20;
   
   /** List of most recently used figure files. Some may be marked as "unavailable". */
   private List<CacheFileKey> mruFigures = new ArrayList<CacheFileKey>();
   
   /** List of most recently used dataset source files. Some may be marked as "unavailable". */
   private List<CacheFileKey> mruData = new ArrayList<CacheFileKey>();
   
   /**
    * Get a list of the most recently used figure files or data set source files. The list returned does NOT include any
    * files currently marked as unavailable.
    * @param wantData True for MRU data set source file list; false for MRU figure file list.
    * @return The requested MRU file list.
    */
   public List<File> getRecentFiles(boolean wantData)
   {
      List<File> copy = new ArrayList<File>();      
      List<CacheFileKey> mru = (wantData) ? mruData : mruFigures;

      int i = 0; 
      while(i < mru.size())
      {
         CacheFileKey key = mru.get(i);
         if(Files.isDirectory(key.path.toPath()))
            mru.remove(i);
         else
         {
            if(key.path.isFile())
            {
               key.tUnavailable = -1;
               copy.add(key.path);
            }
            else if(key.tUnavailable < 0) key.tUnavailable = System.currentTimeMillis();
            
            ++i;
         }
      }
      return(copy);
   }

   /**
    * Get a list of the directories containing the most recently used figure files or data set source files. The list 
    * returned does NOT include directories for any files currently marked as unavailable.
    * @param wantData True for MRU data set source file directories; false for MRU figure file directories.
    * @return The requested MRU directory list; it should not contain any duplicate entries and should be ordered from
    * most to least recent.
    */
   public List<File> getRecentDirectories(boolean wantData)
   {
      List<File> copy = new ArrayList<File>();  
      List<CacheFileKey> mru = (wantData) ? mruData : mruFigures;

      int i = 0; 
      while(i < mru.size())
      {
         CacheFileKey key = mru.get(i);
         if(Files.isDirectory(key.path.toPath()))
            mru.remove(i);
         else
         {
            if(key.path.isFile())
            {
               key.tUnavailable = -1;
               File dir = key.path.getParentFile();
               if(!copy.contains(dir)) copy.add(dir);
            }
            else if(key.tUnavailable < 0) key.tUnavailable = System.currentTimeMillis();
            
            ++i;
         }
      }
      return(copy);
   }
   
   /**
    * Get the most recently used figure file or data set source file. 
    * 
    * @param wantData True for the MRU data set source file; false for the MRU figure file.
    * @return The requested MRU file, possibly null if no such file exists.
    */
   public File getMostRecentFile(boolean wantData)
   {
      List<CacheFileKey> mru = (wantData) ? mruData : mruFigures;

      int i = 0; 
      while(i < mru.size())
      {
         CacheFileKey key = mru.get(i);
         if(Files.isDirectory(key.path.toPath()))
            mru.remove(i);
         else
         {
            if(key.path.isFile())
            {
               key.tUnavailable = -1;
               return(key.path);
            }
            else if(key.tUnavailable < 0) key.tUnavailable = System.currentTimeMillis();
            
            ++i;
         }
      }
      return(null);
   }
  
   /**
    * Add a file to the workspace's "most recently used" figure file list or data set source file list.
    * @param f The file to be added. The file is examined to determine which MRU list it should be added. If it is not 
    * a valid figure file or data set source file, it is ignored. NOTE that a valid figure file may be a FypML figure
    * (*.fyp) or a Matlab figure file (*.fig).
    */
   public void addToRecentFiles(File f)
   {
      if(f == null || !f.isFile()) return;
      
      SrcType st = null;
      if(DataSrcFactory.isDataSource(f)) st = SrcType.DATA;
      else if(FGModelSchema.isFigureModelXMLFile(f)) st = SrcType.FYP;
      else if(MatlabFigureImporter.isMatlabFigFile(f)) st = SrcType.MATFIG;
      if(st == null) return;
      
      // grow the path cache!
      addToPathCache(f, st);
      
      List<CacheFileKey> mru = (st==SrcType.DATA) ? mruData : mruFigures;
      boolean found = false;
      for(int i=0; i<mru.size(); i++) if(f.equals(mru.get(i).path))
      {
         found = true;
         mru.remove(i);
         mru.add(0, new CacheFileKey(f));
         break;
      }

      if(!found)
      {
         if(mru.size() == MRU_MAXLEN) mru.remove(MRU_MAXLEN-1);
         mru.add(0, new CacheFileKey(f));
      }
   }
   
   
   //
   // Path cache for the different kinds of files tracked by the FC workspace manager
   //
   
   /** A carriage-return-linefeed pair. */
   private final static String CRLF = "\r\n";
   
   /** Informmation source type, enumerating the different categories of files stored in the workspace path cache. */
   public enum SrcType 
   {
      /** A FypML figure definition file (*.fyp) */ FYP("F"),
      /** A Matlab figure, stored as a Level 5 MAT file with extension .fig. */ MATFIG("G"),
      /** A FypML data set source file (*.dna, *.dnb, *.dnx, *.txt) */ DATA("D");
      
      SrcType(String tag) { this.tag = tag; }
      
      /** The tag used in workspace path cache to identify the type of information stored in a cached file. */
      private final String tag;
      
      /**
       * Get the information source type enumerant for the specified workspace path cache tag.
       * @param tag The path cache  tag.
       * @return The corresponding {@link SrcType}, or null if path tag is not recognized.
       */
      static SrcType getSrcTypeByPathCacheTag(String tag)
      {
         SrcType st = null;
         if(FYP.tag.equals(tag)) st = FYP;
         else if(MATFIG.tag.equals(tag)) st = MATFIG;
         else if(DATA.tag.equals(tag)) st = DATA;
         return(st);
      }
      
      /** 
       * Get the path cache tag for this information source type.
       * @return Single-character string used to identify the information source type for a file listed in the workspace
       * path cache.
       */
      String getPathCacheTag() { return(tag); }
      
      /** 
       * Infer the information source type from the file's extension.
       * @param f An abstract pathname.
       * @return The source type inferred from the file's extension (case ignored); null if extension not recognized.
       */
      static SrcType getSrcTypeByExtension(File f)
      {
         FCFileType ft = FCFileType.getFileType(f);
         if(ft == FCFileType.FYP) return(SrcType.FYP); 
         else if(ft == FCFileType.FIG) return(SrcType.MATFIG);
         else if(ft == FCFileType.DNA || ft == FCFileType.DNB) return(SrcType.DATA);
         else return(null);
      }
   }
   
   /**
    * <b>CacheFileKey</b> encapsulates the abstract pathname and "availability" of MRU files and workspace directories 
    * in the workspace's path cache.
    * 
    * <p>A MRU file or a path cache directory may become unavailable because it was actually deleted OR because the
    * network mount or hard drive on which it is located is temporarily unavailable. Rather than remove an unavailable
    * path immediately, we mark the file or directory as "unavailable", remember the system time at which it first 
    * became unavailable, and remove it from the MRU files list or the path cache only if it has remained unavailable 
    * for at least 1 week. In this way, the MRU files list and the path cache should prove more reliable in the face of 
    * temporary network drop-outs.</p>
    * 
    * <p>Equality between two <b>CacheFileKey</b>s is based on their abstract pathnames only, so a {@link File} can
    * equate to a <b>CacheFileKey</b> instance.</p>
    * 
    * @author sruffner
    */
   private class CacheFileKey
   {
      CacheFileKey(File path)
      {
         if(path == null) throw new IllegalArgumentException("null arg");
         this.path = path;
         this.tUnavailable = path.exists() ? -1 : System.currentTimeMillis();
      }
      
      @Override public boolean equals(Object obj) 
      { 
         if(obj instanceof CacheFileKey) return(path.equals(((CacheFileKey)obj).path));
         else if(obj instanceof File) return(path.equals(obj));
         else return(false);
      }
      @Override public int hashCode() { return(path.hashCode()); }

      /** The abstract pathname of the file or directory. */
      File path;
      /** The system time at which file was found to be unavailable. -1 if file exists. */
      long tUnavailable;
   }
   
   /** 
    * Cache of known <i>Figure Composer</i>-related files on the host's file system. Three types of files are tracked in
    * this cache: FypML figure files (.fyp), Matlab figure files (.fig), and data set source files (.dna, .txt, .dnr, 
    * and .dnb). The hash-map cache is keyed by parent directory, and each map value is a list of relevant files in
    * that directory.
    * <p>The parent directory could be marked as "unavailable", in which case the associated list of cache entries is
    * maintained, but is not exposed to users of <b>FCWorkspace</b>.</p>
    * <p><i>Thread-safe implementations of the hashmap and the list are used so that the path cache can be accessed
    * and updated on multiple threads.</i></p>
    */
   private Map<CacheFileKey, List<File>> pathCache = new ConcurrentHashMap<CacheFileKey, List<File>>();
   
   /** Name of cache file in user's workspace directory that stores path information on <i>FC</i>-related files. */
   private final static String PATHCACHEFILENAME = "paths.txt";
   
   /** 
    * Path cache file version number. Version is reported in first line of file as "version=N". Versioning began in 
    * May 2010. Before then, the file lacked this version line.
    * 
    * <ul>
    * <li>29nov2012: Version = 2. Introduced support for keeping track of hub view instance data batch files. No 
    * conversion needed to go from version 1 to 2, as we're adding support for a new kind of file.</li>
    * <li>15jul2013: Version = 3. Introduced support for keeping track of Matlab figure files (.fig), which can now be
    * imported as equivalent FypML figures whenever possible. No migration required to go from version 2 to 3, as we're
    * simply adding support for another type of file.
    * <li>09jan2017: Version = 4. Dropped support for <i>DataNav</i> application suite -- Figure Composer is now a fully
    * stand-alone application with its own workspace directory. No longer keep track of hub view instance data batch
    * files, which were specific to the <i>DataNav Builder</i> application. No migration required; any view instance
    * data batch file entries in the path cache file are simply ignored.</li>
    * <li>13dec2018: Version = 5. The MRU figure list can now include Matlab FIG files as well as FypML files. No
    * migration required.</li>
    * </ul>
    */
   private final static int PATHCACHEFILEVERSION = 5;
   
   /** Workspace file in which path information is cached for all known <i>Figure Composer</i>-specific files. */
   private File pathCacheFile = null;
   
   /**
    * Helper method loads the workspace path cache and MRU lists from a dedicated file in the user's workspace 
    * directory.
    * <p>If the path cache file does not exist, the method will NOT try to access the user's old <i>Phyplot 2.1</i> 
    * preferences node, as was done through version 3.6.2. However, it WILL try to copy the same-named file in the
    * old <i>DataNav</i> workspace directory. Any non-FC files in that old path cache file will simply be ignored.</p>
    */
   private void loadPathCache()
   {
      pathCache.clear();
      mruFigures.clear();
      mruData.clear();
      
      // copy old DataNav path cache file to FC workspace if the FC path cache file does not yet exist
      if(!pathCacheFile.exists()) copyOldPathCacheFile();
      
      if(pathCacheFile.exists())
      {
         String mruFypTag = SrcType.FYP.getPathCacheTag() + ": ";
         String mruMatFigTag = SrcType.MATFIG.getPathCacheTag() + ": ";
         String mruDataTag = SrcType.DATA.getPathCacheTag() + ": ";
         BufferedReader rdr = null;
         CacheFileKey nextDirKey = null;
         List<File> entries = new ArrayList<File>();
         try
         {
            rdr = new BufferedReader( new InputStreamReader( new FileInputStream(pathCacheFile), "8859_1" ) );
            boolean isCurrent = false;
            boolean isFirstLine = true;
            boolean done = false;
            while(!done)
            {
               String line = rdr.readLine();
               if(isFirstLine)
               {
                  if(line == null) break;
                  isFirstLine = false;
                  isCurrent = line.startsWith("version=");
                  if(isCurrent) continue;
               }
               
               if(line == null) done = true;
               else if(line.startsWith(mruFypTag) || line.startsWith(mruMatFigTag) || line.startsWith(mruDataTag))
               {
                  boolean isFig = !line.startsWith(mruDataTag);
                  String tag = isFig ? (line.startsWith(mruFypTag) ? mruFypTag : mruMatFigTag) : mruDataTag;
                  
                  if(!isCurrent)
                  {
                     File f = new File(line.substring(tag.length()));
                     if(!Files.isDirectory(f.toPath())) 
                     {
                        if(isFig) mruFigures.add(new CacheFileKey(f));
                        else mruData.add(new CacheFileKey(f));
                     }
                  }
                  else
                  {
                     line = line.substring(tag.length());
                     if(line.startsWith("OK "))
                     {
                        File f = new File(line.substring(3));
                        if(!Files.isDirectory(f.toPath()))
                        {
                           if(isFig) mruFigures.add(new CacheFileKey(f));
                           else mruData.add(new CacheFileKey(f));
                        }
                     }
                     else
                     {
                        // MRU file is marked as "unavailable" in the cache file. Get unavailable time and path. If 
                        // file still does not exist, discard it if it has been unavailable for a week; else keep it.
                        int nextSpace = line.indexOf(' ');
                        if(nextSpace < 0) continue;
                        String tstamp = line.substring(0, nextSpace);
                        File f = new File(line.substring(nextSpace+1));
                        if(Files.isDirectory(f.toPath())) continue;
                        CacheFileKey key = new CacheFileKey(f);
                        if(!f.isFile()) 
                        {
                           long t = -1;
                           try { t = Long.parseLong(tstamp); }
                           catch(NumberFormatException nfe) {}
                           if(t > 0 && (System.currentTimeMillis() - t > 86400000)) continue;
                           if(t > 0) key.tUnavailable = t;
                        }
                        if(isFig) mruFigures.add(key);
                        else mruData.add(key);
                     }
                  }
               }
               else if(line.startsWith(": "))
               {
                  if((nextDirKey != null) && !entries.isEmpty()) 
                  {
                     pathCache.put(nextDirKey, new CopyOnWriteArrayList<File>(entries));
                     entries = new ArrayList<File>();
                  }
                  nextDirKey = null;
                  
                  if(!isCurrent)
                  {
                     File dir = new File(line.substring(2));
                     if(!dir.isFile()) nextDirKey = new CacheFileKey(dir);
                  }
                  else
                  {
                     line = line.substring(2);
                     if(line.startsWith("OK "))
                     {
                        File f = new File(line.substring(3));
                        if(!f.isFile()) nextDirKey = new CacheFileKey(f);
                     }
                     else
                     {
                        // directory is marked as "unavailable" in the cache file. Get unavailable time and path. If it
                        // still does not exist, discard it if it has been unavailable for a week; else keep it.
                        int nextSpace = line.indexOf(' ');
                        if(nextSpace < 0) continue;
                        String tstamp = line.substring(0, nextSpace);
                        File f = new File(line.substring(nextSpace+1));
                        if(f.isFile()) continue;
                        CacheFileKey key = new CacheFileKey(f);
                        if(!Files.isDirectory(f.toPath())) 
                        {
                           long t = -1;
                           try { t = Long.parseLong(tstamp); }
                           catch(NumberFormatException nfe) {}
                           if(t > 0 && (System.currentTimeMillis() - t > 86400000)) continue;
                           if(t > 0) key.tUnavailable = t;
                        }
                        nextDirKey = key;
                     }
                  }
               }
               else if(nextDirKey != null && line.length() > 2)
               {
                  SrcType srcType = SrcType.getSrcTypeByPathCacheTag(line.substring(0, 1));
                  if(srcType != null)
                  {
                     File f = new File(nextDirKey.path, line.substring(2));
                     if((f.isFile() || !nextDirKey.path.exists()) && !entries.contains(f))
                        entries.add(f);
                  }
               }
            }
         }
         catch(InvalidPathException|IOException e) {}
         finally
         {
            if(nextDirKey != null && !entries.isEmpty())
               pathCache.put(nextDirKey, new CopyOnWriteArrayList<File>(entries));
            try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
         }
      }
      
      // for each directory in the path cache that currently exists, update its path cache entry to reflect the
      // FC-related files it actually holds. If it no longer has any, remove it from path cache. This addresses file
      // system changes that occur while FC is not running.
      List<File> scanList = new ArrayList<File>();
      for(CacheFileKey k : pathCache.keySet()) if(Files.isDirectory(k.path.toPath()))
         scanList.add(k.path);
      for(File dir : scanList)
         addDirectoryToPathCache(dir, true);
   }
   
   /**
    * Copy the old <i>DataNav</i> workspace path cache file to the <i>Figure Composer</i> workspace directory. Any
    * non-FC files cached in the old path cache file are simply ignored and will be removed when the path cache file is
    * written back to disk. No action is taken if a path cache file already exists in the current <i>Figure Composer</i>
    * workspace directory. Fails silently (old path cache file not found, or copy operation failed).
    */
   private void copyOldPathCacheFile()
   {
      if(pathCacheFile.exists()) return;
      
      File oldPathCacheFile = null;
      try
      {
         File oldHome = new File(System.getProperty("user.home"), OLD_DATANAV_WSDIR);
         if(oldHome.isDirectory()) 
         {
            oldPathCacheFile = new File(oldHome, PATHCACHEFILENAME);
            Files.copy(oldPathCacheFile.toPath(), pathCacheFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
         }
      }
      catch(SecurityException se) {}
      catch(IOException ioe) {}
   }
   

   /**
    * Helper method saves the workspace path cache and MRU file lists to a dedicated path cache file in the user's 
    * <i>Figure Composer</i> workspace directory. If an IO error occurs, the method fails silently. To minimize the 
    * chance of corrupting the existing path cache file, the path cache and MRU lists are first saved to a temporary 
    * file before replacing the old file. 
    */
   private void savePathCache()
   {
      if(pathCache.isEmpty() && mruFigures.isEmpty() && mruData.isEmpty())
      {
         pathCacheFile.delete();
         return;
      }
      
      File tmpFile = new File(pathCacheFile.getAbsolutePath() + ".tmp");
      BufferedWriter writer = null;
      try
      {
         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "8859_1"));

         writer.write("version=" + PATHCACHEFILEVERSION + CRLF);
         
         for(CacheFileKey key : mruFigures)
         {
            SrcType st = null;
            if(key.path.isFile())
            {
               if(FGModelSchema.isFigureModelXMLFile(key.path)) st = SrcType.FYP;
               else if(MatlabFigureImporter.isMatlabFigFile(key.path)) st = SrcType.MATFIG;
               
               if(st != null)
                  writer.write(st.getPathCacheTag()+ ": OK " + key.path.getAbsolutePath() + CRLF);
            }
            else
            {
               String nameStr = key.path.getName().toLowerCase();
               if(nameStr.endsWith(".fyp")) st = SrcType.FYP;
               else if(nameStr.endsWith(".fig")) st = SrcType.MATFIG;
               
               if(st != null)
               {
                  long tUnavail = key.tUnavailable < 0 ? System.currentTimeMillis() : key.tUnavailable;
                  writer.write(st.getPathCacheTag() + ": " + tUnavail + " " + key.path.getAbsolutePath() + CRLF);
               }
            }
         }
            
         for(CacheFileKey key : mruData)
         {
            if(key.path.isFile()) 
               writer.write(SrcType.DATA.getPathCacheTag() + ": OK " + key.path.getAbsolutePath() + CRLF);
            else
            {
               long tUnavail = key.tUnavailable < 0 ? System.currentTimeMillis() : key.tUnavailable;
               writer.write(SrcType.DATA.getPathCacheTag() + ": " + tUnavail + " " + key.path.getAbsolutePath() + CRLF);
            }
         }
         
         for(CacheFileKey dir : pathCache.keySet())
         {
            List<File> entries = pathCache.get(dir);
            if(entries.isEmpty()) continue;
            
            boolean dirExists = Files.isDirectory(dir.path.toPath());
            if(dirExists) writer.write(": OK " + dir.path.getAbsolutePath() + CRLF);
            else
            {
               long tUnavail = dir.tUnavailable < 0 ? System.currentTimeMillis() : dir.tUnavailable;
               writer.write(": " + tUnavail + " " + dir.path.getAbsolutePath() + CRLF);
            }
            
            // if workspace directory exists, we discard any files within it that do not exist. However, if directory 
            // does not exist, we do not discard any files -- the directory may only be temporarily unavailable.
            for(File f : entries) if((!dirExists) || f.isFile())
            {
               SrcType st = SrcType.getSrcTypeByExtension(f);
               if(st != null)
                  writer.write(st.getPathCacheTag() + " " + f.getName() + CRLF);
            }
         }
         
         writer.close();
         pathCacheFile.delete();
         tmpFile.renameTo(pathCacheFile);
      }
      catch(InvalidPathException|IOException ioe) {}
      finally
      {
         try { if(writer != null) writer.close(); } catch(IOException ioe) {}
      }
   }

   /** Monitors workspace directories for external changes (on a background daemon thread. */
   private DirWatcher pathCacheMonitor = null;
   
   /** Start monitoring workspace directories in the background to detect external changes in the "path cache". */
   private void startPathCacheMonitor()
   {
      stopPathCacheMonitor();
      pathCacheMonitor = DirWatcher.startWatcher(
            getWorkspaceDirectories(true), true, true, new String[] {"fyp", "fig", "txt", "dna", "dnr", "dnb"});
      pathCacheMonitor.setPollingTimeout(2);
      pathCacheMonitor.addListener(this);
   }
   
   /** Stop monitoring workspace directories in the background. */
   private void stopPathCacheMonitor()
   {
      if(pathCacheMonitor != null)
      {
         pathCacheMonitor.removeListener(this);
         pathCacheMonitor.stop();
         pathCacheMonitor = null;
      }
   }
   
   /**
    * This handler is called -- on the Swing event dispatch thread -- whenever a change is detected by the path cache
    * monitor, which is configured at startup to "watch" for file system changes within all existing workspace
    * directories listed in the path cache file. The handler updates the path cache IAW the specified file system
    * event(s).
    */
   @Override public void onDirectoryWatchUpdate(Event e)
   {
      Event nextEvt = e;
      while(nextEvt != null)
      {
         switch(nextEvt.kind) {
         case CREATE:
            addToWorkspace(nextEvt.target);
            break;
         case MODIFY:
            break;
         case DELETE:
            removeFromPathCache(nextEvt.target);
            break;
         case DROPPED:
            for(CacheFileKey key : pathCache.keySet()) if(key.path.equals(nextEvt.target))
            {
               key.tUnavailable = System.currentTimeMillis();
               notifyListeners(EventID.PATHCACHE, null, null);
               break;
            }
            break;
         case RESTORED:
            for(CacheFileKey key : pathCache.keySet()) if(key.path.equals(nextEvt.target))
            {
               addDirectoryToPathCache(nextEvt.target, false);
               notifyListeners(EventID.PATHCACHE, null, null);
               break;
            }
            break;
         case UNREGISTERED:
            break;
         case CLOSED:
            startPathCacheMonitor();
            return;
         }
         
         nextEvt = nextEvt.getNext();
      }
   }
   

   /**
    * Add a file to the workspace's "path cache" of <i>Figure Composer</i>-related files: a FypML figure file (*.fyp), 
    * a Matlab figure file (*.fig), or a data set source file (*.dna, *.txt, *.dnr, or *.dnb).
    * @param f The file to be added. The file is examined to ensure it is one of the tracked file types.
    * @return True if successful; false if file does not exist or is not one of the tracked file types.
    */
   public boolean addToWorkspace(File f)
   {
      if(f == null || !f.isFile()) return(false);
      SrcType st = null;
      if(DataSrcFactory.isDataSource(f)) st = SrcType.DATA;
      else if(FGModelSchema.isFigureModelXMLFile(f)) st = SrcType.FYP;
      else if(MatlabFigureImporter.isMatlabFigFile(f)) st = SrcType.MATFIG;
      
      if(st != null) addToPathCache(f, st);
      return(st != null);
   }
   
   /**
    * Remove a file from the workspace's "path cache" of <i>Figure Composer</i>-related files: a FypML figure file 
    * (*.fyp), a Matlab figure file (*.fig), or a data set source file (various extensions). This offers a way to 
    * immediately update the workspace path cache when a relevant file is removed by the program itself. No action is 
    * taken if the specified file still exists or is not already in the path cache.
    * @param f The file to be removed.
    * @return True if file was removed from path cache, else false.
    */
   public boolean removeFromWorkspace(File f)
   {
      return(removeFromPathCache(f));
   }
   
   /**
    * Update workspace "path cache" to reflect a change in the abstract file-system pathname of a <i>FC</i>-related file
    * present in that cache. If both the original and renamed file paths exist in the file system, then {@link 
    * #addToWorkspace(File)} is invoked for the new file path. Both files must have the same {@link FCFileType}.
    * @param original The original file path. If this is not present in the path cache, no action is taken.
    * @param renamed The renamed path. If this file does not exist, then no action is taken. It need not reside in the
    * same parent directory as the original file path.
    * @return True if original file was replaced by the renamed file path in the path cache; else false.
    */
   public boolean renameInWorkspace(File original, File renamed)
   {
      if(original == null || renamed == null || (!renamed.isFile()) || 
            (FCFileType.getFileType(original) != FCFileType.getFileType(renamed)))
         return(false);
      if(original.isFile())
      {
         addToWorkspace(renamed);
         return(false);
      }
      else
         return(renameInPathCache(original, renamed));
   }
   
   /**
    * Is the specified file stored in the <i>Figure Composer</i> workspace's "path cache"?
    * @param f The abstract pathname of the file to test.
    * @param st The file's expected source type.
    * @return True iff specified file exists, is listed in the workspace path cache, and is the requested source type. 
    */
   public boolean isInWorkspace(File f, SrcType st)
   {
      if(st == null || f == null || !f.isFile()) return(false);
      if(st != SrcType.getSrcTypeByExtension(f)) return(false);
      List<File> entries = pathCache.get(new CacheFileKey(f.getParentFile()));
      return(entries != null && entries.contains(f));
   }
   
   /**
    * Get list of all directories in user's <i>Figure Composer</i> workspace -- i.e., all known directories containing 
    * <i>FC</i>-specific files. The list returned does NOT include any directories currentlyl marked as unavailable.
    * @return List of existing directories with <i>FC</i>-specific files, in no particular order.
    */
   public List<File> getWorkspaceDirectories()
   {
      return(getWorkspaceDirectories(false));
   }
   
   /** 
    * Get list of all directories in the user's path cache -- optionally including any directories that are currently 
    * marked as unavailable (because the directory disappeared from the the file system within the last 7 days and has
    * not yet reappeared).
    * 
    * @param all If true, any "unavailable" directories in the path cache are also included in the result.
    * @return List of workspace directories with <i>FC</i>-specific files, in no particular order.
    */
   private List<File> getWorkspaceDirectories(boolean all)
   {
      List<File> results = new ArrayList<File>();
      for(CacheFileKey key : pathCache.keySet()) 
      {
         if(Files.isDirectory(key.path.toPath()))
         {
            key.tUnavailable = -1;
            results.add(key.path);
         }
         else 
         {
            if(key.tUnavailable < 0) key.tUnavailable = System.currentTimeMillis();
            if(all) results.add(key.path);
         }
      }
      return(results);
   }

   /**
    * Get list of all directories in user's <i>Figure Composer</i> workspace that contain files of the specified type. 
    * The list returned does NOT include any directories currently marked as unavailable.
    * @param st The file source type.
    * @return List of requested workspace directories, in no particular order. Could be empty.
    */
   public List<File> getWorkspaceDirectories(SrcType st)
   {
      List<File> results = new ArrayList<File>();
      if(st == null) return(results);
      
      for(CacheFileKey key : pathCache.keySet())
      {
         if(Files.isDirectory(key.path.toPath()))
         {
            key.tUnavailable = -1;
            List<File> entries = pathCache.get(key);
            if(entries != null) 
            {
               for(File f : entries) if(st == SrcType.getSrcTypeByExtension(f))
               {
                  results.add(key.path);
                  break;
               }
            }
         }
         else if(key.tUnavailable < 0) 
            key.tUnavailable = System.currentTimeMillis();
      }
      return(results);
   }
   
   /**
    * Get list of all known <i>Figure Composer</i> files of the specified source type in the specified directory.
    * @param dir A workspace directory, i.e., a directory in the workspace path cache known to have <i>FC</i> files.
    * @param st A file source type.
    * @return List of all files in the workspace path cache that are in the specified directory and are of the specified
    * source type. List will be empty if the specified directory does not exist or is not in the workspace cache, or if
    * there are no files in the path cache that are both in the directory and of the specified type.
    */
   public List<File> getWorkspaceFiles(File dir, SrcType st)
   {
      List<File> results = new ArrayList<File>();
      if((st == null) || (dir == null) || !Files.isDirectory(dir.toPath())) return(results);
      
      List<File> entries = pathCache.get(new CacheFileKey(dir));
      if(entries != null) for(File f : entries)
      {
         if(f.isFile() && (st == SrcType.getSrcTypeByExtension(f))) results.add(f);
      }
      return(results);
   }
   
   /**
    * Does the specified workspace directory have <i>Figure Composer</i>-specific files of the specified source type?
    * @param dir A workspace directory, i.e., a directory in the workspace cache known to have <i>FC</i> files.
    * @param st A file source type.
    * @param True if specified directory exists, is stored in the workspace cache, and has at least one known file of
    * the specified type. 
    */
   public boolean hasContent(File dir, SrcType st)
   {
      if(dir == null || !Files.isDirectory(dir.toPath())) return(false);
      
      List<File> entries = pathCache.get(new CacheFileKey(dir));
      if(entries == null || entries.size() == 0) return(false);
      for(File f : entries) if(st == SrcType.getSrcTypeByExtension(f)) return(true);
      return(false);
   }
   
   /** 
    * Helper method adds a file's abstract pathname to the workspace path cache.
    * @param f Abstract pathname of file. It is assumed that file exists.
    */
   private void addToPathCache(File f, SrcType st)
   {
      File dir = f.getParentFile();
      CacheFileKey key = new CacheFileKey(dir);
      List<File> entries = pathCache.get(key);
      if(entries == null)
         addDirectoryToPathCache(dir, false);
      else if(!entries.contains(f))
         entries.add(f);
      
      notifyListeners(EventID.PATHCACHE, null, null);
   }

   /**
    * Add specified directory in the workspace path cache. This method scans the directory for any FC-related files. If
    * there are none, no further action is taken. If there are, the directory is either added to the path cache, or if
    * it's already there, its set of FC-related files is updated. The directory is also registered with the path
    * cache monitor so that future changes within the directory are detected.
    * @param dir Abstract pathname of directory. If directory does not exist, no action is taken.
    * @param removeEmpties. If the specified directory is already in the cache but no longer contains any FC-related
    * files, it will be removed from the cache IF this flag is set.
    */
   private void addDirectoryToPathCache(File dir, boolean removeEmpties)
   {
      // scan all files (do not search subdirectories!) in directory for relevant files, accounting 
      // for changes to files of which we're already aware and adding new files of which we're not.
      File[] files = (dir != null && dir.isDirectory()) ? dir.listFiles() : null;
      if(files == null) return;
      List<File> relevant = new ArrayList<File>();
      for(File f : files)
      {
         if(Files.isDirectory(f.toPath())) continue;
         if(DataSrcFactory.isDataSource(f) || FGModelSchema.isFigureModelXMLFile(f)  
               || MatlabFigureImporter.isMatlabFigFile(f))
            relevant.add(f);
      }
      if(relevant.size() > 0)
      {
         pathCache.put(new CacheFileKey(dir), new CopyOnWriteArrayList<File>(relevant));
         if(pathCacheMonitor != null) pathCacheMonitor.registerDirectory(dir);
      }
      else if(removeEmpties && (null != pathCache.remove(new CacheFileKey(dir))))
      {
         if(pathCacheMonitor != null) pathCacheMonitor.unregisterDirectory(dir);
      }
   }
   
   /**
    * Helper method removes a file's abstract pathname from the workspace path cache -- but ONLY if the specified file
    * no longer exists.
    * @param f Abstract pathname of file. If file exists, no action is taken.
    * @return True if and only if specified file was removed from path cache.
    */
   private boolean removeFromPathCache(File f)
   {
      if(f.isFile()) return(false);
      
      boolean wasRemoved = false;
      CacheFileKey key = new CacheFileKey(f.getParentFile());
      List<File> entries = pathCache.get(key);
      if(entries != null)
      {
         if(entries.remove(f))
         {
            wasRemoved = true;
            notifyListeners(EventID.PATHCACHE, null, null);
         }
      }
      return(wasRemoved);
   }
   
   /**
    * Helper method renames a file in the workspace path cache.
    * @param original Abstract pathname of the original file. If there is no entry for this file in the path cache, or
    * if the physical file still exists, no action is taken.
    * @param renamed Abstract pathname specifying the new filename. No action is taken if the physical file does not
    * exist. <i>The file need not have the same parent directory as the original file path.</i>
    * @return True if operation completed; false if arguments did not satisfy the constraints specified above.
    */
   private boolean renameInPathCache(File original, File renamed)
   {
      if(original.isFile() || (!renamed.isFile()))
         return(false);
      
      boolean wasRenamed = false;
      CacheFileKey key = new CacheFileKey(original.getParentFile());
      boolean sameParent = Utilities.filesEqual(original.getParentFile(), renamed.getParentFile());
      List<File> entries = pathCache.get(key);
      if(entries != null)
      {
         int idx = entries.indexOf(original);
         if(idx > -1)
         {
            // if original and renamed paths share the same parent directory, the update is easy. Otherwise, we have to
            // remove the cache entry for the original path and add an entry for the renamed path, which may also
            // involve adding a new directory key if the renamed path's parent directory is not found in path cache.
            if(sameParent)
               entries.set(idx, renamed);
            else
            {
               entries.remove(idx);
               key = new CacheFileKey(renamed.getParentFile());
               entries = pathCache.get(key);
               if(entries == null)
                  addDirectoryToPathCache(renamed.getParentFile(), false);
               else if(!entries.contains(renamed))
                  entries.add(renamed);
            }
            
            wasRenamed = true;
            notifyListeners(EventID.PATHRENAME, original, renamed);
         }
      }
      
      return(wasRenamed);
   }
   
   //
   // Workspace listeners
   //

   /**
    * Enumeration of the different events broadcast by the <i>Figure Composer</i> workspace manager to any registered
    * listeners. All events are delivered on the Swing event dispatch thread.
    * @author sruffner
    */
   public static enum EventID
   {
      /** The user has changed the screen resolution.*/ SCREENRES, 
      /** The workspace path cache has been updated. */ PATHCACHE, 
      /** A file path in the workspace path cache has been renamed or moved in the file system. */ PATHRENAME
   }
   
   /** Interface implemented by objects that want to receive events that may be broadcast by the workspace manager. */
   public interface Listener extends EventListener
   {
      /**
       * Notification of a specific update in the user's workspace. The workspace manager delivers this notification on
       * the Swing event dispatch thread to inform registered listeners of a change in screen resolution, a change in 
       * the workspace path cache, or a change in the filename of an individual file in the path cache.
       * @param id The event ID.
       * @param fp1 For {@link EventID#PATHRENAME} only, this is the original abstract file path that was just renamed. 
       * Otherwise, ignore.
       * @param fp2 For {@link EventID#PATHRENAME} only, this is the renamed file path (note that renamed file need not
       * reside in the same parent directory as the original). Otherwise, ignore.
       */
      void onWorkspaceUpdate(EventID id, File fp1, File fp2);
   }
   
   /**
    * Add a workspace listener.
    * <p>The workspace manager will notify registered listeners if the user changes the current screen resolution (the
    * "screenDPI" property), if the workspace path cache is updated in some way, or if an existing file in the path
    * cache was renamed. Notifications are delivered on the Swing event dispatch thread, even though path cache updates
    * happen on a background thread. The primary purpose here is to update the GUI elements whose appearance or content 
    * would be affected by these changes.</p>
    * @param l The workspace listener to add.
    */
   public void addWorkspaceListener(Listener l) { listeners.add(Listener.class, l); }
   
   /**
    * Remove the specified listener from this workspace manager's listener list.
    * @param l The workspace listener to remove.
    */
   public void removeWorkspaceListener(Listener l) { listeners.remove(Listener.class, l); }

   /**
    * A {@link Runnable} which sends workspace update notifications to registered listeners.
    * @author sruffner
    */
   private class Notifier implements Runnable
   {
      Notifier(EventID id, File fp1, File fp2)
      {
         this.id = id;
         this.fp1 = (id==EventID.PATHRENAME) ? fp1 : null;
         this.fp2 = (id==EventID.PATHRENAME) ? fp2 : null; 
      }
      
      @Override public void run()
      {
         if(id == null) return;
         
         Object[] rcvrs = listeners.getListenerList();
         for(int i=rcvrs.length-2; i>=0; i-=2)
         {
            if(rcvrs[i] == Listener.class)
               ((Listener) rcvrs[i+1]).onWorkspaceUpdate(id, fp1, fp2);
         }
      }

      private EventID id;
      private File fp1;
      private File fp2;
   }

   /** The set of listeners registered to receive events broadcast by the workspace manager. */
   private EventListenerList listeners = new EventListenerList();

   /** 
    * Send specified workspace event to all registered listeners. Ensure it is sent on the Swing event dispatch thread.
    * @param id The workspace event ID.
    * @param fp1 For {@link EventID#PATHRENAME}, this is the original file path. Otherwise ignored.
    * @param fp2 For {@link EventID#PATHRENAME}, this is the renamed file path. Otherwise ignored.
    */
   private void notifyListeners(EventID id, File fp1, File fp2)
   {
      Notifier notifier = new Notifier(id, fp1, fp2);
      if(SwingUtilities.isEventDispatchThread())
         notifier.run();
      else
         SwingUtilities.invokeLater(notifier);
   }

   
   // 
   // Background monitoring and updates of path cache
   //
   


   //
   // A small cache of recently previewed models of Figure Composer figures or datasets
   //
   
   /**
    * An entry in the internal model cache. It includes a reference to the renderable model, the source file, and the 
    * dataset ID (if applicable). It also caches the file's last modified time -- in order to detect and remove stale 
    * cache entries.
    */
   private static class ModelEntry
   {
      ModelEntry(File f, String dsID, RenderableModel model)
      {
         this.source = f;
         this.lastModTime = f.lastModified();
         this.model = model;
         this.dataSetID = dsID;
      }
      
      long lastModTime;
      File source;
      String dataSetID;
      RenderableModel model;
   }
   
   /** Maximum number of entries in the workspace's prepared model cache. */
   private final static int MAXCACHESIZE = 10;
   
   /** The workspace's pre-prepared model cache. */
   private List<ModelEntry> renderModelCache = new ArrayList<ModelEntry>();
   
   /** 
    * Save a graphic model to the workspace's internal model cache. If the cache is maxed out, the last item in it is 
    * removed before adding the new model. If there's already a matching entry in the cache, that entry is updated and 
    * moved to the top. 
    * 
    * @param f The abstract pathname of the file from which the model was built. This file must exist. It will be a 
    * <i>FypML</i> figure file, a <i>Matlab</i> .FIG file that was successfully imported and converted to a <i>FypML</i>
    * figure model, or a <i>FC</i> dataset source file.
    * @param dsID If non-null, then this is the data set ID.
    * @param m The model to be cached. If <i>dsID</i> is specified, then this should be an instance of 
    * {@link DataSetPreview}; otherwise, it should be an instance of {@link FGraphicModel}. Regardless, an independent 
    * copy is made -- so the model is cached in the state it was in at the time this method was called!
    */
   public synchronized void putModelToCache(File f, String dsID, RenderableModel m)
   {
      if(f == null || !f.isFile() || m == null) return;
      for(int i=0; i<renderModelCache.size(); i++) 
      {
         ModelEntry c = renderModelCache.get(i);
         if(c.source.equals(f) && (dsID == null || dsID.equals(c.dataSetID)))
         {
            c.lastModTime = f.lastModified();
            c.model = (m instanceof FGraphicModel) ? FGraphicModel.copy((FGraphicModel)m) : 
               new DataSetPreview(((DataSetPreview)m).getDataSet());
            renderModelCache.remove(i);
            renderModelCache.add(0, c);
            return;
         }
      }
      
      if(renderModelCache.size() == MAXCACHESIZE) renderModelCache.remove(MAXCACHESIZE-1);
      RenderableModel copy = (m instanceof FGraphicModel) ? FGraphicModel.copy((FGraphicModel)m) : 
         new DataSetPreview(((DataSetPreview)m).getDataSet());
      renderModelCache.add(0, new ModelEntry(f, dsID, copy));
   }
   
   /**
    * Retrieve the graphic model rendering a <i>Figure Composer</i> data set or figure from the workspace's model cache.
    * @param f Abstract pathname of source file: a FypML file, a Matlab .FIG file, or a data set source file.
    * @param dsID If retrieving a figure model, this should be <b>null</b>; else, it specifies the ID of the 
    * particular data set within the source file for which a graphic preview is sought.
    * @return The requested model, or null if cache does not contain the requested model, or the cached model is 
    * out-of-date (in which case it is removed from the cache). Note that a copy of the cached model is returned to 
    * protect against corrupting the cache!
    */
   public synchronized RenderableModel getModelFromCache(File f, String dsID)
   {
      for(ModelEntry c : renderModelCache) if(c.source.equals(f) && (dsID == null || dsID.equals(c.dataSetID)))
      {
         if(c.lastModTime == f.lastModified())
         {
            renderModelCache.remove(c);
            renderModelCache.add(0, c);
            if(c.model instanceof FGraphicModel)
               return(FGraphicModel.copy((FGraphicModel) c.model));
            else
               return(new DataSetPreview(((DataSetPreview)c.model).getDataSet()));
         }
         else
         {
            renderModelCache.remove(c);
            return(null);
         }
      }
      return(null);
   }
   
   /**
    * Remove the graphic model rendering a <i>Figure Composer</i> data set or figure from the workspace's model cache.
    * @param f Abstract pathname of source file: a FypML file, a Matlab .FIG file, or a data set source file.
    * @param dsID If retrieving a figure model, this should be null; else, it specifies the ID of the particular data
    * set within the source file for which a graphic preview is sought.
    * @return The requested model, or null if cache does not contain the requested model, or the cached model is 
    * out-of-date.
    */
   public synchronized RenderableModel removeModelFromCache(File f, String dsID)
   {
      for(ModelEntry c : renderModelCache) if(c.source.equals(f) && (dsID == null || dsID.equals(c.dataSetID)))
      {
         renderModelCache.remove(c);
         return((c.lastModTime == f.lastModified()) ? c.model : null);
      }
      return(null);
   }
}
