; ================================
; Nexis DB Setup Installer
; ================================

[Setup]
AppName=Nexis DB Setup
AppVersion=1.0
DefaultDirName={commonpf}\NexisDB
DefaultGroupName=Nexis DB Setup
OutputBaseFilename=NexisDB_Setup_v1
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
SetupIconFile=icon.ico

[Files]
Source: "Nexis_DB_Setup.exe";  DestDir: "{app}"; Flags: ignoreversion
Source: "nexis-db-setup.jar";  DestDir: "{app}"; Flags: ignoreversion
Source: "icon.ico";            DestDir: "{app}"; Flags: ignoreversion
Source: "icon.png";            DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Nexis DB Setup";       Filename: "{app}\Nexis_DB_Setup.exe"; IconFilename: "{app}\icon.ico"
Name: "{userdesktop}\Nexis DB Setup"; Filename: "{app}\Nexis_DB_Setup.exe"; IconFilename: "{app}\icon.ico"

[Run]
Filename: "{app}\Nexis_DB_Setup.exe"; Description: "Launch Nexis DB Setup"; Flags: nowait postinstall skipifsilent runascurrentuser