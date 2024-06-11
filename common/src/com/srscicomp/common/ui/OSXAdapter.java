package com.srscicomp.common.ui;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import com.srscicomp.common.util.Utilities;


/**
 * <b>OSXAdapter</b> is a macOS-specific adapter that is part of a strategy for improving the integration of a Java
 * desktop application with the native operating system. It handles file-opening events from macOS (so that user can
 * start the relevant app by double-clicking a file that the app is registered to open), plus three standard items in 
 * the macOS "Application" menu: the "About", "Preferences", and "Quit" menu commands.
 * 
 * <p>Originally, the implementation relied on an internal package, <b>com.apple.eawt</b>, that provides the hooks into
 * the native macOS functionality. While that package was accessible through Java 8, it became an internal JDK with the
 * "modularization" of the Java runtime in JDK 9. At the same time, the desktop integration functionality was moved to
 * {@link java.awt.Desktop}, but that functionality has not been backported to Java 8. Thus, it is rather tricky to
 * support the macOS desktop integration features we need while at the same time allowing the application to run on
 * Java 8 as well as Java 9+.</p>
 * 
 * <p>We adapted a solution (https://github.com/kaikramer/keystore-explorer/pull/103/files) which uses reflection
 * techniques to use classes from either <b>com.apple.eawt</b> or <b>java.awt.desktop</b>, depending on the major 
 * version number of the JRE in which the application is running.</p>
 * 
 * <p>USAGE: Early during application startup, construct the adapter object, specifying the {@link OSXCompatibleApp}
 * object that actually implements the various desktop actions. Then call {@link #addEventHandlers()} to hook the 
 * event handlers into macOS. Be sure to check for any exceptions thrown by this method!</p>
 * 
 * TODO: This is an interim solution compiled against JDK 8 that works in both Java 8 and Java 9+. Eventually, when we 
 * compile against and require the next Java long-term support release, version 11, we should reimplement this by 
 * explicitly using {@link java.awt.Desktop} and eliminating reliance on both <b>com.apple.eawt</b> and reflection 
 * techniques.
 * 
 * @author sruffner
 */
public class OSXAdapter implements InvocationHandler
{
	/** The macOS-compatible application, which provides the actual handlers. */
	private final OSXCompatibleApp theApp;

	/**
	 * Construct the macOS desktop integration adapter. To register the handlers with the underlying macOS, you must
	 * call {@link #addEventHandlers()}.
	 * 
	 * @param inApp The macOS-compatible app that provides handlers for the "About", "Quit", "Preferences" (optional),
	 * and "Open Files" (optional) actions. Cannot be null.
	 * menu items.
	 */
	public OSXAdapter(OSXCompatibleApp inApp) 
	{
      if(inApp == null) throw new NullPointerException("Required argument is null");
      
		theApp = inApp;
	}
	
	/**
	 * Register the event handlers for the "About", "Quit", "Preferences", and "Open Files" actions on macOS. Support
	 * for the "Preferences" and "Open Files" actions are optional. If the macOS-compatible application object does not
	 * support them, the relevant handlers are not registered.
	 * 
	 * <p>This method uses reflection techniques both to register the handlers. Be sure to catch any exceptions in case
	 * reflection fails!</p>
	 */
	public void addEventHandlers() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
	   InvocationTargetException, InstantiationException 
	{
	   // using reflection to avoid Mac specific classes being required for compiling on other platforms, AND to handle
      // significant changes in JDK 9, which moved the relevant functionality from the internal package com.apple.eawt 
	   // to java.awt.desktop
      Class<?> applicationClass; 
      Class<?> quitHandlerClass;
      Class<?> aboutHandlerClass;
      Class<?> openFilesHandlerClass;
      Class<?> preferencesHandlerClass;
      Object application;
      
      if(Utilities.getJavaMajorVersion() > 8)
      {
         applicationClass = Class.forName("java.awt.Desktop");
         application = applicationClass.getDeclaredMethod("getDesktop").invoke(null, new Object[0]);
         
         quitHandlerClass = Class.forName("java.awt.desktop.QuitHandler");
         aboutHandlerClass = Class.forName("java.awt.desktop.AboutHandler");
         openFilesHandlerClass = Class.forName("java.awt.desktop.OpenFilesHandler");
         preferencesHandlerClass = Class.forName("java.awt.desktop.PreferencesHandler");
      }
      else
      {
         applicationClass = Class.forName("com.apple.eawt.Application");
         application = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
         
         quitHandlerClass = Class.forName("com.apple.eawt.QuitHandler");
         aboutHandlerClass = Class.forName("com.apple.eawt.AboutHandler");
         openFilesHandlerClass = Class.forName("com.apple.eawt.OpenFilesHandler");
         preferencesHandlerClass = Class.forName("com.apple.eawt.PreferencesHandler");    
      }

      Object proxy = Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(), new Class<?>[]{
         quitHandlerClass, aboutHandlerClass, openFilesHandlerClass, preferencesHandlerClass}, this);
      applicationClass.getDeclaredMethod("setQuitHandler", quitHandlerClass).invoke(application, proxy);
      applicationClass.getDeclaredMethod("setAboutHandler", aboutHandlerClass).invoke(application, proxy);
      if(theApp.supportsFileOpenEvents())
         applicationClass.getDeclaredMethod("setOpenFileHandler", openFilesHandlerClass).invoke(application, proxy);
      if(theApp.hasPreferences())
         applicationClass.getDeclaredMethod("setPreferencesHandler", preferencesHandlerClass).invoke(application,proxy);

	}

	@SuppressWarnings("unchecked")
   @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
	{
	   if("openFiles".equals(method.getName())) 
	   {
	      if(args[0] != null) 
	      {
	         Object files = args[0].getClass().getMethod("getFiles").invoke(args[0]);
	         if(files instanceof List<?>) 
	            theApp.doFileOpen((List<File>) files);
	      } 
	   }
	   else if("handleQuitRequestWith".equals(method.getName())) 
	   {
	      theApp.doExit();
	      // the above method will return only if user cancels exit
	      if(args[1] != null)
	      {
	         args[1].getClass().getDeclaredMethod("cancelQuit").invoke(args[1]);
	      }
	   } 
	   else if("handleAbout".equals(method.getName())) 
	   {
	      theApp.doAbout();
	   } 
	   else if("handlePreferences".equals(method.getName())) 
	   {
	      theApp.doPreferences();
      }
	   return null;
	}
}
