# FigureComposer

**FigureComposer** is a standalone Java application for preparing, revising and reviewing detailed scientific figures 
intended for journal publication. It is a mature application that has been used by members of Stephen G.
Lisberger's laboratory at Duke University since 2007, as well as some others in the neuroscience commmunity.

<mark>**_NOTE: I am no longer actively developing FC. I have made this repo available for anyone in the neuroscience 
community that continues to use the application and might wish to fork the repo to adapt the program for their own 
use._**</mark>


## User Guide
An [online user's guide](https://sites.google.com/a/srscicomp.com/figure-composer/introduction) provides a thorough 
introduction to the program, a detailed format description of the application's figure document format (_FypML_), a
complete version history, and a downloads page where you can get the latest release.

## Installation
An (unsigned) application bundle is available for installation on MacOS, an installer EXE for Windows, and an executable
JAR for Linux. You must separately install a Java Runtime Environment compatible with your host operating system in 
order to run **FigureComposer**. As of version 5.2.0, a Java 8 runtime is required. For details, see the 
[online guide](https://sites.google.com/a/srscicomp.com/figure-composer/download-files).

**_NOTE: As of version 5.5.0, the code base is JDK11-compliant, and a Java 11 runtime is required. Recommend using
[Amazon Corretto 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/what-is-corretto-11.html)._**

### Related Matlab Utilities
Researchers in the Lisberger and other labs have used [_Matlab_](https://www.mathworks.com/products/matlab.html) 
extensively over the years to process their experimental data and prepare draft figures for journal publication. I have 
written a number of Matlab scripts to address the issue of how to conveniently get data into or out of a **FigureComposer**
figure (_FypML_ file). You will find these scripts in the `supporting` folder within the repo. These are probably of
limited use, since **FigureComposer** (as of version 4.2.3) can import many Matlab figure (`*.fig`) files directly.

The various utility scripts are described in details in the 
[online guide](https://sites.google.com/a/srscicomp.com/figure-composer/loading-data/matlab-utilities).

The `matfig2fyp()` script relies on JAR files `hhmi-ms-common.jar` and `hhmi-ms-datanav.jar`, which are included in the
**Figure Composer** download ZIP and must be installed in Matlab's Java class path. **_As of version 5.5.0, the
class files in these JARs are JDK11-compliant, so they require that Matlab be configured to use a Java 11 runtime_**. 
Matlab's default bundled JRE is still Java 8 as of R2024a, but it supports using OpenJDK 11 runtimes such as Amazon 
Corretto 11. To specify the JRE that Matlab should use, run `jenv(<jre_home>)` from the Matlab command line (where 
`<jre_home>` is the installation path for the OpenJDK 11-compliant runtime environment), then restart Matlab.

## Building/Packaging
- The code base in this repository is set-up as an IntelliJ IDEA project, and the IDEA project settings are included in 
the repo.
- The `macosxdist` folder contains resource files for packaging FC as a MacOS application bundle, which is then archived
in a ZIP file for distribution. 
- The `windist` folder contains resource files for building a Windows installer EXE for FC. The installer is created on
a Windows workstation using [InnoSetup](https://jrsoftware.org/isinfo.php).
- An Ant build file, `release.xml`, semi-automates the build process: creating the source JARs for the three modules 
in the IDEA project (`common`, `jmatio`, and `figurecomposer`), packaging the class files in module JARS, and preparing 
ZIP archives for installing FC on MacOS, Windows, and Linux. **_Be sure to read through this file carefully before 
attempting to build your own release, and always rebuild the entire project before running the Ant build._**

## License
**Figure Composer** was created by [Scott Ruffner](mailto:sruffner@srscicomp.com). It is licensed under the terms of 
the MIT license.

## Credits
**FigureComposer** was developed with funding provided by the Stephen G. Lisberger laboratory in the Department of
Neurobiology at Duke University.

As a full-featured Java Swing application, it relies heavily upon Oracle's Java Development Kit; as of June 2024, it is 
JDK11-compliant and built with Amazon Corretto 11. 

In addition, it makes use of a number of open-source software packages. Without these third-party packages, 
**FigureComposer** would have been a much more difficult endeavor. 
- _FypML_ figure documents have been read and written 
using [**MXP1**](https://github.com/codelibs/xpp3?tab=readme-ov-file), an open-source implementation of an XML pull 
parser. As of version 5.2.0, this library is no longer used; instead, FC relies on the Streaming API for XML Processing 
that is now part of the Java runtime environment.
- The JavaScript Object Notation (JSON) data interchange format is used heavily, and the application relies on the 
[org.json](https://www.json.org/json-en.html) package to read and write JSON data.
- _Matlab_ FIG files are read and parsed using a modified version of the 
[Java MAT-file IO (JMatIO)](https://sourceforge.net/projects/jmatio/) package.
- _FypML_ figures are exported to Portable Document Format (PDF) and Scalable Vector Graphics (SVG) files 
with the help of the [iText5 PDF](https://itextpdf.com/products/itext-5-legacy) and the 
[JFreeSVG](https://www.jfree.org/jfreesvg/index.html) libraries, respectively.

