<#
  Automatischer Test von install-frosthain.ps1 auf einem Windows-Runner (CI).
  Nutzt eine künstliche .minecraft-Umgebung im Temp-Ordner – keine echte Installation wird berührt.
  Aufruf: Test-Installer.ps1 -AssetDir <Ordner mit Release-Dateien>
#>
param([Parameter(Mandatory = $true)][string]$AssetDir)

$ErrorActionPreference = "Stop"
$installer = Join-Path $PSScriptRoot "..\install-frosthain.ps1"
$exe = (Get-Process -Id $PID).Path
$root = Join-Path ([System.IO.Path]::GetTempPath()) ("frosthain-installer-test-" + [guid]::NewGuid().ToString("N"))
$failures = 0

function Check([bool]$Condition, [string]$Text) {
    if ($Condition) { Write-Host "  OK    $Text" -ForegroundColor Green }
    else { Write-Host "  FEHLER $Text" -ForegroundColor Red; $script:failures++ }
}

function New-FakeMinecraft([string]$Dir) {
    New-Item -ItemType Directory -Force -Path (Join-Path $Dir "saves\Meine Welt") | Out-Null
    Set-Content (Join-Path $Dir "saves\Meine Welt\level.dat") "unberührt"
    $profiles = '{"profiles":{"vanilla":{"name":"Meine Installation","type":"custom","created":"2024-05-01T10:00:00.000Z","lastVersionId":"latest-release","icon":"Grass"}},"settings":{"crashAssistance":true},"version":3}'
    [System.IO.File]::WriteAllText((Join-Path $Dir "launcher_profiles.json"), $profiles)
}

function Invoke-Installer([string[]]$Arguments) {
    & $exe -NoProfile -ExecutionPolicy Bypass -File $installer @Arguments | Out-Host
    return $LASTEXITCODE
}

Write-Host "PowerShell $($PSVersionTable.PSVersion) – $exe"
try {
    # 1. Normale Installation
    $mc = Join-Path $root "mc"; $inst = Join-Path $root "instance"; $backups = Join-Path $root "backups"
    New-FakeMinecraft $mc
    $code = Invoke-Installer @("-AssetDir", $AssetDir, "-MinecraftDir", $mc, "-InstanceDir", $inst, "-BackupRoot", $backups)
    Check ($code -eq 0) "Installation endet mit Code 0"
    $data = Get-Content (Join-Path $mc "launcher_profiles.json") -Raw | ConvertFrom-Json
    Check ($null -ne $data.profiles.frosthain) "Profil 'frosthain' eingetragen"
    Check ($data.profiles.vanilla.name -eq "Meine Installation") "Fremdes Profil unverändert"
    Check ($data.settings.crashAssistance -eq $true) "Launcher-Einstellungen erhalten"
    Check ($data.profiles.frosthain.gameDir -eq $inst) "Getrennter Spielordner gesetzt"
    $versionId = $data.profiles.frosthain.lastVersionId
    Check (Test-Path (Join-Path $mc "versions\$versionId\$versionId.json")) "Fabric-Version $versionId abgelegt"
    Check (Test-Path (Join-Path $inst "mods\frosthain.jar")) "frosthain.jar in mods"
    Check (Test-Path (Join-Path $inst "mods\fabric-api.jar")) "fabric-api.jar in mods"
    Check ((Get-ChildItem $backups -Recurse -Filter "launcher_profiles.json").Count -ge 1) "Backup der Profildatei angelegt"
    Check ((Get-Content (Join-Path $mc "saves\Meine Welt\level.dat")) -eq "unberührt") "Bestehende Welt unberührt"
    $bytes = [System.IO.File]::ReadAllBytes((Join-Path $mc "launcher_profiles.json"))
    Check (-not ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB)) "Profildatei ohne BOM geschrieben"

    # 2. Zweite Installation (Update) ist wiederholbar
    $code = Invoke-Installer @("-AssetDir", $AssetDir, "-MinecraftDir", $mc, "-InstanceDir", $inst, "-BackupRoot", $backups)
    Check ($code -eq 0) "Erneute Installation (Update) erfolgreich"

    # 3. Manipulierte Datei wird abgelehnt, nichts verändert
    $mc2 = Join-Path $root "mc2"; New-FakeMinecraft $mc2
    $tampered = Join-Path $root "tampered"; Copy-Item $AssetDir $tampered -Recurse
    Add-Content (Join-Path $tampered "frosthain.jar") "x"
    $before = Get-Content (Join-Path $mc2 "launcher_profiles.json") -Raw
    $code = Invoke-Installer @("-AssetDir", $tampered, "-MinecraftDir", $mc2, "-InstanceDir", (Join-Path $root "inst2"), "-BackupRoot", $backups)
    Check ($code -ne 0) "Manipulierte Datei führt zum Abbruch"
    Check ((Get-Content (Join-Path $mc2 "launcher_profiles.json") -Raw) -eq $before) "Profildatei nach Abbruch unverändert"

    # 4. Ohne Minecraft Launcher: verständlicher Abbruch
    $code = Invoke-Installer @("-AssetDir", $AssetDir, "-MinecraftDir", (Join-Path $root "fehlt"), "-InstanceDir", (Join-Path $root "inst3"), "-BackupRoot", $backups)
    Check ($code -ne 0) "Fehlender Launcher wird erkannt"

    # 5. Deinstallation entfernt nur das eigene Profil, Welten bleiben
    New-Item -ItemType Directory -Force -Path (Join-Path $inst "saves\Frosthain-Welt") | Out-Null
    $code = Invoke-Installer @("-Uninstall", "-MinecraftDir", $mc, "-InstanceDir", $inst, "-BackupRoot", $backups)
    $data = Get-Content (Join-Path $mc "launcher_profiles.json") -Raw | ConvertFrom-Json
    Check ($code -eq 0) "Deinstallation endet mit Code 0"
    Check ($null -eq $data.profiles.frosthain) "Profil 'frosthain' entfernt"
    Check ($data.profiles.vanilla.name -eq "Meine Installation") "Fremdes Profil nach Deinstallation unverändert"
    Check (Test-Path (Join-Path $inst "saves\Frosthain-Welt")) "FROSTHAIN-Welten bleiben erhalten"
} finally {
    Remove-Item $root -Recurse -Force -ErrorAction SilentlyContinue
}

if ($failures -gt 0) { Write-Host "$failures Prüfung(en) fehlgeschlagen." -ForegroundColor Red; exit 1 }
Write-Host "Alle Installer-Prüfungen bestanden." -ForegroundColor Green
exit 0
