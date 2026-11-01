<#
.SYNOPSIS
  Installiert FROSTHAIN für den offiziellen Minecraft Launcher (ohne winget, ohne Java, ohne Git).

.DESCRIPTION
  Ablauf:
    1. Prüft Windows, PowerShell und den offiziellen Minecraft Launcher (.minecraft mit launcher_profiles.json).
    2. Lädt die Release-Dateien (oder nimmt sie aus -AssetDir) und prüft jede Datei per SHA-256 gegen SHA256SUMS.txt.
       Bei einer Abweichung wird abgebrochen, bevor irgendetwas verändert wurde.
    3. Sichert die Launcher-Profildateien nach %LOCALAPPDATA%\Frosthain\backups\<Zeitstempel>.
    4. Legt die Fabric-Versionsbeschreibung additiv in .minecraft\versions ab, richtet eine getrennte
       FROSTHAIN-Instanz (eigener Spielordner mit mods, Welten, Einstellungen) ein und trägt die
       Installation "FROSTHAIN" in den Launcher ein.
    5. Bei einem Fehler werden die Profildateien aus dem Backup wiederhergestellt.

  Anmeldung, Besitzprüfung und Java-Laufzeit übernimmt der offizielle Minecraft Launcher (ADR 0004).
  Bestehende Welten, Profile und Mods anderer Installationen werden nicht verändert.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File install-frosthain.ps1
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File install-frosthain.ps1 -Uninstall
#>
[CmdletBinding()]
param(
    [string]$Source = "https://github.com/danilohuth888-create/Frosthain/releases/download/frosthain-dev",
    [string]$AssetDir = "",
    [string]$MinecraftDir = (Join-Path $env:APPDATA ".minecraft"),
    [string]$InstanceDir = (Join-Path $env:LOCALAPPDATA "Frosthain\instance"),
    [string]$BackupRoot = (Join-Path $env:LOCALAPPDATA "Frosthain\backups"),
    [ValidateRange(2, 32)][int]$MemoryGB = 4,
    [switch]$Uninstall,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$ProfileKey = "frosthain"
$ProfileFiles = @("launcher_profiles.json", "launcher_profiles_microsoft_store.json")
$Assets = @("frosthain.jar", "fabric-api.jar", "fabric-loader-profile.json")
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Info([string]$Text) { Write-Host "  $Text" }
function Step([string]$Text) { Write-Host "`n==> $Text" -ForegroundColor Yellow }
function Fail([string]$Text) { throw [System.Exception]::new($Text) }

function Read-Json([string]$Path) {
    $text = [System.IO.File]::ReadAllText($Path)
    $convert = Get-Command ConvertFrom-Json
    if ($convert.Parameters.ContainsKey("DateKind")) {
        return $text | ConvertFrom-Json -DateKind String
    }
    return $text | ConvertFrom-Json
}

function Write-Json([string]$Path, $Object) {
    $json = $Object | ConvertTo-Json -Depth 32
    [System.IO.File]::WriteAllText($Path, $json, $Utf8NoBom)
}

function Get-ProfilePaths {
    $found = @()
    foreach ($name in $ProfileFiles) {
        $path = Join-Path $MinecraftDir $name
        if (Test-Path $path) { $found += $path }
    }
    return $found
}

function Assert-LauncherClosed {
    $running = Get-Process -Name "MinecraftLauncher", "Minecraft" -ErrorAction SilentlyContinue
    if ($running -and -not $Force) {
        Fail "Der Minecraft Launcher läuft gerade. Bitte schließe ihn vollständig und starte die Installation erneut (er würde die Änderungen sonst überschreiben)."
    }
}

function Backup-Profiles([string[]]$Paths) {
    $dir = Join-Path $BackupRoot (Get-Date -Format "yyyyMMdd-HHmmss")
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    foreach ($path in $Paths) { Copy-Item $path (Join-Path $dir (Split-Path $path -Leaf)) }
    return $dir
}

function Restore-Profiles([string]$BackupDir, [string[]]$Paths) {
    foreach ($path in $Paths) {
        $saved = Join-Path $BackupDir (Split-Path $path -Leaf)
        if (Test-Path $saved) { Copy-Item $saved $path -Force }
    }
}

# ------------------------------------------------------------------------------------------------
Write-Host "FROSTHAIN – Installation für den offiziellen Minecraft Launcher" -ForegroundColor Cyan

try {
    if ($env:OS -ne "Windows_NT") { Fail "Dieses Skript ist für Windows 10/11 gedacht." }
    if ($PSVersionTable.PSVersion.Major -lt 5) { Fail "PowerShell 5.1 oder neuer wird benötigt." }
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    Step "Minecraft Launcher prüfen"
    $profilePaths = @(Get-ProfilePaths)
    if ($profilePaths.Count -eq 0) {
        Fail "Kein offizieller Minecraft Launcher gefunden ($MinecraftDir\launcher_profiles.json fehlt). Bitte den Launcher installieren, einmal starten, mit deinem Microsoft-Konto anmelden und das Skript erneut ausführen."
    }
    $profilePaths | ForEach-Object { Info "gefunden: $_" }
    Assert-LauncherClosed

    if ($Uninstall) {
        Step "FROSTHAIN-Installation aus dem Launcher entfernen"
        $backup = Backup-Profiles $profilePaths
        foreach ($path in $profilePaths) {
            $data = Read-Json $path
            if ($data.profiles.PSObject.Properties[$ProfileKey]) {
                $data.profiles.PSObject.Properties.Remove($ProfileKey)
                Write-Json $path $data
                Info "entfernt aus $(Split-Path $path -Leaf)"
            }
        }
        Info "Sicherung der Profildateien: $backup"
        Info "Deine FROSTHAIN-Welten bleiben erhalten in: $(Join-Path $InstanceDir 'saves')"
        Write-Host "`nFertig." -ForegroundColor Green
        exit 0
    }

    Step "Dateien laden und prüfen (SHA-256)"
    $work = Join-Path ([System.IO.Path]::GetTempPath()) ("frosthain-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $work | Out-Null
    foreach ($name in @("SHA256SUMS.txt") + $Assets) {
        $target = Join-Path $work $name
        if ($AssetDir) {
            $local = Join-Path $AssetDir $name
            if (-not (Test-Path $local)) { Fail "Datei fehlt im Ordner ${AssetDir}: $name" }
            Copy-Item $local $target
        } else {
            try {
                Invoke-WebRequest -UseBasicParsing -Uri "$Source/$name" -OutFile $target
            } catch {
                Fail "Download fehlgeschlagen: $Source/$name – bitte Internetverbindung prüfen. ($($_.Exception.Message))"
            }
        }
    }
    $expected = @{}
    foreach ($line in [System.IO.File]::ReadAllLines((Join-Path $work "SHA256SUMS.txt"))) {
        if ($line -match '^([0-9a-fA-F]{64})\s+\*?(.+)$') { $expected[$Matches[2].Trim()] = $Matches[1].ToLowerInvariant() }
    }
    foreach ($name in $Assets) {
        if (-not $expected.ContainsKey($name)) { Fail "Keine Prüfsumme für $name in SHA256SUMS.txt." }
        $actual = (Get-FileHash -Algorithm SHA256 (Join-Path $work $name)).Hash.ToLowerInvariant()
        if ($actual -ne $expected[$name]) {
            Fail "Prüfsumme stimmt nicht für $name (erwartet $($expected[$name]), erhalten $actual). Installation abgebrochen, nichts wurde verändert."
        }
        Info "ok: $name"
    }

    $loaderProfile = Read-Json (Join-Path $work "fabric-loader-profile.json")
    $versionId = [string]$loaderProfile.id
    if (-not ($versionId -match '^fabric-loader-[0-9.]+-[0-9.]+$')) { Fail "Unerwartete Fabric-Versionsbeschreibung: '$versionId'." }
    Info "Fabric-Version: $versionId"

    Step "Launcher-Profile sichern"
    $backup = Backup-Profiles $profilePaths
    Info "Sicherung: $backup"

    $versionDir = Join-Path $MinecraftDir "versions\$versionId"
    $createdVersionDir = -not (Test-Path $versionDir)
    try {
        Step "Fabric-Version eintragen (additiv)"
        New-Item -ItemType Directory -Force -Path $versionDir | Out-Null
        $versionJson = Join-Path $versionDir "$versionId.json"
        if (Test-Path $versionJson) {
            $same = (Get-FileHash $versionJson).Hash -eq (Get-FileHash (Join-Path $work "fabric-loader-profile.json")).Hash
            if (-not $same) { Copy-Item $versionJson (Join-Path $backup "$versionId.json") }
        }
        Copy-Item (Join-Path $work "fabric-loader-profile.json") $versionJson -Force
        Info $versionJson

        Step "FROSTHAIN-Instanz einrichten"
        $mods = Join-Path $InstanceDir "mods"
        New-Item -ItemType Directory -Force -Path $mods | Out-Null
        Get-ChildItem $mods -File | Where-Object { $_.Name -in @("frosthain.jar", "fabric-api.jar") } | Remove-Item -Force
        Copy-Item (Join-Path $work "frosthain.jar") $mods
        Copy-Item (Join-Path $work "fabric-api.jar") $mods
        Info "Spielordner: $InstanceDir"

        Step "Installation 'FROSTHAIN' im Launcher eintragen"
        $now = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        foreach ($path in $profilePaths) {
            $data = Read-Json $path
            if (-not $data.profiles) { $data | Add-Member -NotePropertyName profiles -NotePropertyValue ([pscustomobject]@{}) -Force }
            $created = $now
            if ($data.profiles.PSObject.Properties[$ProfileKey] -and $data.profiles.$ProfileKey.created) { $created = $data.profiles.$ProfileKey.created }
            $entry = [pscustomobject]@{
                name          = "FROSTHAIN"
                type          = "custom"
                created       = $created
                lastUsed      = $now
                lastVersionId = $versionId
                gameDir       = $InstanceDir
                javaArgs      = "-Xmx${MemoryGB}G -XX:+UseG1GC -XX:MaxGCPauseMillis=50"
                icon          = "Crafting_Table"
            }
            $data.profiles | Add-Member -NotePropertyName $ProfileKey -NotePropertyValue $entry -Force
            Write-Json $path $data
            Info "eingetragen in $(Split-Path $path -Leaf)"
        }
    } catch {
        Write-Host "`nFehler – stelle die Launcher-Profile aus der Sicherung wieder her …" -ForegroundColor Red
        Restore-Profiles $backup $profilePaths
        if ($createdVersionDir -and (Test-Path $versionDir)) { Remove-Item $versionDir -Recurse -Force }
        throw
    } finally {
        Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
    }

    Write-Host "`nFROSTHAIN ist installiert." -ForegroundColor Green
    Write-Host @"
So spielst du:
  1. Offiziellen Minecraft Launcher öffnen (beim ersten Mal mit deinem Microsoft-Konto anmelden).
  2. Links neben "Spielen" die Installation "FROSTHAIN" wählen und "Spielen" drücken.
  3. Einzelspielerwelt erstellen. FROSTHAIN-Welten liegen getrennt in: $InstanceDir\saves
Entfernen (Welten bleiben erhalten): dieses Skript mit -Uninstall ausführen.
Sicherung der Launcher-Profile: $backup
"@
    exit 0
} catch {
    Write-Host "`nFROSTHAIN konnte nicht installiert werden:" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Hilfe: docs/11-windows-test-und-uebergabe.md im Repository oder die Konsolenausgabe an das Projekt senden."
    exit 1
}
