The Java application Figure Composer (FC) is distributed as an "OS X application bundle" for the Mac OSX operating 
system. This is the preferred way to package an OS X application, as it allows for Apple-specific user interface 
conveniences like: (1) docking the application icon on the dock bar; (2) a start-up splash screen; (3) menu bar at the 
top of the screen; (4) double-clicking a .FYP file to open Figure Composer (file association).

The FC application bundle does not come with its own Java runtime environment. A suitable version of Java must be
installed on your system.

Release notes for most recent releases (in reverse chronological order):

--- Release 5.5.0 ---
FC 5.5.0 is compiled against a Java 11 SDK and requires a Java 11 runtime environment; Java 11 is the next long-term
stable version after Java 8. DO NOT INSTALL version 5.5.0 unless you're ready to update your system to Java 11.
Furthermore, if you use any Matlab script in the FigureComposer Matlab support package that requires a supporting JAR
file, that JAR also requires JDK11-compliance. You must update your Matlab installation to use a Java 11 runtime
environment rather than the default Java 8 runtime that comes with Matlab. Matlab is unlikely to include a Java 11
runtime as the default any time soon, but it has supported OpenJDK 11-compliant runtimes (Eclipse Adoptium, Amazon
Corretto) since R2023a.


--- Release 5.3.0 ---
Since Mac OS X Mojave (10.14), Apple prevents an application from accessing "Protected Resources" like the user's
Desktop, Documents, and Downloads folders without user permission. When an application attempts such an access, OS will
intervene and raise an AppleScript dialog asking the user for permission. If the user grants it, OS remembers that --
and it shows up in System Preferences -> Security & Privacy -> Privacy -> Files and Folders. Unfortunately, this
only works for application **binaries**. As of 5.2.0, FC is launched on Mac OS with a BASH-based script. You could grant
BASH itself full access to the system via the Security & Privacy preferences, but that's probably NOT a good idea
security-wise. Another issue with using a binary launcher -- we're once again have to provide multiple installs b/c
that binary may have to be different for different OS versions.

To address this issue, FC 5.3.0 updates how FC is packaged for installation on Mac OS X. The developers of the BASH
script universalJavaApplicationStub dealt with the problem above by compiling the script into a binary. Two binaries are
available in the latest release -- one for Catalina (10.15.x) and one for Big Sur (11.x). Both are compiled for the
x86_64 architecture. So, we now provide 3 versions of the FC app bundle for OS -- one targeting Catalina, one
targeting Big Sur, and a third that uses the BASH-based script and should work on any Mac OS -- albeit you will not be
able to browse figure files under those protected folders unless you grant BASH full access to your system (not
recommended). The Big Sur app bundle should run on Mac OS Monterrey (12.x), which is deployed on newer Macs with the
ARM-based M1 chip. However, x86_64 binaries are run on the M1 using Rosetta emulation, which may be a performance
issue. Hopefully, an ARM version of the launcher binary will be available in the near future. Better yet, it would be
helpful to create a so-called "Universal Binary" that runs on Intel or M1 Macs withou emulation and supports multiple
recent versions of the Mac OS! Also, we will need a more sophisticated installer to avoid having to create these
different versions...


--- Release 5.2.0 ---
As of April 2019, Oracle licensing of its Java runtime environment changed dramatically. In addition, the
"release cadence" for Java has switched to more frequent releases with fewer major changes, followed by limited
patches and security updates until the next major release (9, 10, 11, 12, 13). Free, non-commercial use of
the Java 8 runtime has ended, and users are encouraged to switch to the open-source OpenJDK project, which appears to
have wide community support. A number of vendors are offering OpenJDK binaries for the various major OS platforms.

To handle this transition, FC 5.2.0 is compiled against JDK 8 and requires a Java 8 runtime. Also the bundle now uses a
different launcher -- an executable bash script in Contents/MacOS/universalApplicationStub. With this launcher I was
able to successfully launch and run FC as an integrated macOS app using the Amazon Corretto JRE 8, which is based on the
OpenJDK project. FC 5.2.0 was also tested on another long-term support release, Java 11, using the Amazon Corretto 11
runtime.


--- Release 5.1.0 ---
Apple stopped supporting its proprietary Java implementation with Java 6 (1.6.0_65). This version is very out-of-date, 
and no longer supported; in fact, the last version of FC that runs on Java 6 will crash on OS X Sierra, and the fault
occurs in the Apple JDK's runtime.

As of FC 5.1.0 (Jan 2017), FC required a Java 7 runtime. The distributed application bundle is packaged to run on 
Oracle's Java implementation. It is recommended, therefore, that you install the latest release of the Oracle Java 7
runtime on your system in order to use Figure Composer; better yet, upgrade to Java 8 since Java 7 and below are EOL and
very out-dated, and Java 8 has been around since early 2014. (If you're using a different Java runtime from some other 
vendor, then the FC application bundle may not work correctly; the bundle launching infrastructure is specific to 
Oracle's Java implementation, unfortunately.)


---------
saruffner
(last updated 23oct2024)