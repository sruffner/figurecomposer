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

System Requirements for Release 5.5.0:
  -- Windows 7 or greater. Tested on Windows 7 (32-bit), 10 (64-bit), and 11 (64-bit). The installer will install a
  32-bit launcher on a 32-bit system and a 64-bit launcher on a 64-bit system. It is assumed that the user installs a
  64-bit JVM under 64-bit Windows!
  -- Java runtime environment with major version 11. It is recommended that users install the latest public
  release of Java 11. If no suitable JRE is found, the application will not launch.
  -- A 2GHz or better computer with 2-4GB RAM.

IMPORTANT: If you use any Matlab script in the FigureComposer Matlab support package that requires a supporting JAR
file, that JAR also requires JDK11-compliance. You must update your Matlab installation to use a Java 11 runtime
environment rather than the default Java 8 runtime that comes with Matlab. Matlab is unlikely to include a Java 11
runtime as the default any time soon, but it has supported OpenJDK 11 (Eclipse Adoptium, Amazon Corretto) since R2023a.

Support Website: https://sites.google.com/a/srscicomp.com/figure-composer
Support email: sruffner@srscicomp.com


CREDITS:
Besides the libraries that come with the Java runtime environment, FigureComposer relies on two third-party libraries:
1) The Java Matlab IO (JMatIO) package makes it possible to read in and parse Matlab FIG files, which are really MAT 
files in disguise. See also jmatio-LICENSE.txt
2) The iTextPDF 5.5 library is required to export FypML figures as PDF documents. It is being used under the GNU Affero
General Public License. See also itextpdf-agpl.txt.
3) FC relies on the JFreeSVG library (https://www.jfree.org/jfreesvg/index.html) to export FypML figures as Scalable
Vector Graphics (SVG).

We rely on two freeware Windows tools to repackage the FigureComposer application as a Win32 executable and distribute 
it in a convenient self-extracting installer:
1) WinRun4J: http://winrun4j.sourceforge.net.
2) Inno Setup Compiler: http://www.jrsoftware.org/isinfo.php


saruffner
(last updated 23oct2024)



