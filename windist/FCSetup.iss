; --FCSetup.iss --
; Inno Setup script to generate a Windows installer for the Java desktop application FigureComposer, 
; along with a FigureComposer-specific Matlab support package.
; NOTES:
; 1) FigureComposer.exe is a WinRun4J wrapper for the corresponding Java app. Wrapping it as an EXE file
; permits better desktop integration.
; 2) FigureComposer can open and edit FypML figure definition files (.fyp extension). DDE is enabled
; to associate the application with the .fyp file type. The installer created by this script updates
; the registry to setup the file association -- unless the user elects not to install FigureComposer.
; 3) During installation, user can elect to install either of the two components (FigureComposer, Matlab 
; support package). If neither component is selected, then the installer aborts.
; 4) For each new version, be sure to update the AppVersion and AppVerName directives in the
; [Setup] section accordingly.
; 5) Note that, as of Jan 2017, FigureComposer is installed in its own "FigureComposer" program group.
; The "DataNav" program group is no more, as the DataNav application suite is defunct.
; 6) As of Nov 2017, the installer supports installation on 32-bit or 64-bit Windows. Separate 32-bit
; and 64-bit Windows executables (fc32.exe and fc64.exe) are included. The 32-bit version is installed on
; 32-bit Windows, and the 64-bit version on 64-bit Windows. Only the 64-bit executable can load the 64-bit
; Java runtime!

[Setup]
AppId={{B409BEF3-AD01-48E8-AB2C-EE902831B85B}
AppName=FigureComposer
AppVersion=5.5.0
AppVerName=FigureComposer 5.5.0
DefaultDirName={userpf}\FigureComposer
DefaultGroupName=FigureComposer
UninstallDisplayIcon={app}\FCSetup.ico
Compression=lzma2/max
SolidCompression=yes
OutputDir=.
OutputBaseFilename=FigureComposerSetup
; "ArchitecturesInstallIn64BitMode=x64" requests that the install be
; done in "64-bit mode" on x64, meaning it should use the native
; 64-bit Program Files directory and the 64-bit view of the registry.
; On all other architectures it will install in "32-bit mode".
ArchitecturesInstallIn64BitMode=x64os
; Note: We don't set ProcessorsAllowed because we want this
; installation to run on all architectures (including Itanium,
; since it's capable of running 32-bit code too).

; always want the initial welcome page to appear
DisableWelcomePage=no

; we're not giving user any control over where things go
DisableDirPage=yes
DisableProgramGroupPage=yes
AlwaysShowDirOnReadyPage=yes
AlwaysShowGroupOnReadyPage=yes

AllowNetworkDrive=no
AllowUNCPath=no
CloseApplications=yes
RestartApplications=no
MinVersion=6.1sp1
PrivilegesRequired=lowest
SetupIconFile=FCSetup.ico
SetupLogging=yes
Uninstallable=yes

AppCopyright=Copyright (C) 2003-2025 Duke University (Lisberger Lab)/Scott Ruffner Scientific Computing
AppPublisher=Scott Ruffner Scientific Computing
AppPublisherURL=https://sites.google.com/a/srscicomp.com/figure-composer
AppSupportURL=https://sites.google.com/a/srscicomp.com/figure-composer
AppUpdatesURL=https://sites.google.com/a/srscicomp.com/figure-composer/download-files
VersionInfoVersion=1.0

ChangesAssociations=yes

[Types]
Name: full; Description: "Install FigureComposer"; Flags: iscustom

[Components]
Name: fc; Description: "FigureComposer Java application"; Types: full
Name: ms; Description: "FigureComposer support package for MATLAB"; Types: full

[Tasks]
Name: fcdesktopicon; Description: "FigureComposer"; GroupDescription: "Create optional desktop icon?"; Flags: unchecked; Components: fc

[Files]
Source: "fc64.exe"; DestDir: "{app}"; DestName: "FigureComposer.exe"; Check: Is64BitInstallMode; Components: fc
Source: "fc32.exe"; DestDir: "{app}"; DestName: "FigureComposer.exe"; Check: not Is64BitInstallMode; Components: fc
Source: "code\*.jar"; Excludes: "hhmi-ms-*.jar"; DestDir: "{app}\jars"; Components: fc
Source: "code\*.txt"; DestDir: "{app}"; Components: fc
Source: "code\*.m"; DestDir: "{app}\matlab"; Components: ms
Source: "code\hhmi-ms-*.jar"; DestDir: "{app}\matlab"; Components: ms
Source: "code\itextpdf*.*"; DestDir: "{app}\matlab"; Components: ms
Source: "FCSetup.ico"; DestDir: "{app}"; Components: fc or ms
Source: "FCApp.ico"; DestDir: "{app}"; DestName: "fyp-ext.ico"; Components: fc

; The WinRun4J wrapper writes/overwrites a log file, fc.log, in the installation directory.
; This could be helpful if FigureComposer crashes, as the Java exception gets written there.
; But this means, on uninstall, the installation directory won't be removed. So here we remove
; the log file(s), and if nothing else is in it, the installation directory
[UninstallDelete]
Type: files; Name: {userpf}\FigureComposer\*.log
Type: dirifempty; Name: {userpf}\FigureComposer

[Icons]
Name: "{group}\FigureComposer"; Filename: "{app}\FigureComposer.exe"; AppUserModelID: "HHMI.FigureComposer"; Components: fc; Flags: createonlyiffileexists
Name: "{group}\FigureComposer MATLAB Utilities"; Filename: "{app}\matlab"; Components: ms
Name: "{group}\FigureComposer Help Site"; Filename: "https://sites.google.com/a/srscicomp.com/figure-composer"; Flags: preventpinning
Name: "{group}\Uninstall FigureComposer"; Filename: "{uninstallexe}"; Flags: createonlyiffileexists preventpinning
Name: "{userdesktop}\FigureComposer"; Tasks: fcdesktopicon; Filename: "{app}\FigureComposer.exe"; AppUserModelID: "HHMI.FigureComposer"; Flags: createonlyiffileexists

[Registry]
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1"; ValueType: string; ValueName: ""; ValueData: "FypML figure definition file"; Flags: uninsdeletekey; Components: fc
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\fyp-ext.ico"; Components: fc
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\FigureComposer.exe"" ""%1"""; Components: fc
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1\shell\open\ddeexec"; ValueType: string; ValueName: ""; ValueData: ""; Components: fc
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1\shell\open\ddeexec\application"; ValueType: string; ValueName: ""; ValueData: "FigureComposer"; Components: fc
Root: HKCU; Subkey: "Software\Classes\FigureComposer.FypML.1\shell\open\ddeexec\topic"; ValueType: string; ValueName: ""; ValueData: "system"; Components: fc
Root: HKCU; Subkey: "Software\Classes\.fyp"; ValueType: string; ValueName: ""; ValueData: "FigureComposer.FypML.1"; Flags: uninsdeletevalue; Components: fc
Root: HKCU; Subkey: "Software\Classes\.fyp"; ValueType: string; ValueName: "PerceivedType"; ValueData: "text"; Components: fc
Root: HKCU; Subkey: "Software\Classes\.fyp\OpenWithProgids"; ValueType: string; ValueName: "FigureComposer.FypML.1"; ValueData: ""; Flags: uninsdeletevalue; Components: fc
