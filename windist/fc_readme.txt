FigureComposer is a Java Swing-based desktop application supporting the construction of graphs and figures suitable for 
publication in scientific journals. It has been in use for some time in the Lisberger laboratory at Duke University.

To improve desktop integration on Windows, the app has been wrapped as a WinRun4J-based executable, FigureComposer.exe. 
The wrapper handles a few setup tasks before launching the actual Java app in a JVM runtime located on the host.

The application is distributed in different ways for different platforms. For Windows (Win 7 and later), 
FigureComposerSetup.exe is a self-extracting installer that offers a simple, single-user installation, modifies the 
registry to associate the application with FypML figure definition files (.fyp extension), creates a program folder 
accessible from the Start Menu, optionally creates a desktop icon, and includes a standard uninstaller to remove the 
program. It will also install a folder containing the files that are part of the FigureComposer Matlab support package 
(.M scripts with supporting JARs).

The uninstaller is accessible from the FigureComposer program folder, or via the Add/Remove Programs feature in Control 
Panel (called "Programs and Features" in Windows 7).

System Requirements:
  -- Windows 7 or greater. Only tested on Windows 7 32-bit and Windows 10 64-bit. The installer will install a 32-bit 
  launcher on a 32-bit system and a 64-bit launcher on a 64-bit system. It is assumed that the user installs a 64-bit 
  JVM under 64-bit Windows!
  -- Java runtime environment with major version 8. It is recommended that users install the latest public
  release of Java 8. If no suitable JRE is found, the application will not launch. Users are also welcome to try the
  next long-term support release of the Java runtime, Java 11.
  -- A 2GHz or better computer with 2-4GB RAM.

** In an upcoming release we plan to migrate FC to compile against Java 11, which, like Java 8, is a long-term support 
version. This release will also come with its own custom-tailored JRE, so that it will no longer be necessary to install
a JRE separately.


Support Website: https://sites.google.com/a/srscicomp.com/figure-composer
Support email: sruffner@srscicomp.com


CREDITS:
Besides the libraries that come with the Java runtime environment, FigureComposer relies on two third-party libraries:
1) The Java Matlab IO (JMatIO) package makes it possible to read in and parse Matlab FIG files, which are really MAT 
files in disguise. See also jmatio-LICENSE.txt
2) The iText PDF library is required to export FypML figures as PDF documents. It is being used under the GNU Affero 
General Public License. See also itextpdf-agpl.txt.

We rely on two freeware Windows tools to repackage the FigureComposer application as a Win32 executable and distribute 
it in a convenient self-extracting installer:
1) WinRun4J: http://winrun4j.sourceforge.net.
2) Inno Setup Compiler: http://www.jrsoftware.org/isinfo.php


saruffner
(last updated 03/03/2020)



