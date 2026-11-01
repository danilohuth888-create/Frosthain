<#
.SYNOPSIS
  FROSTHAIN – Test- und Entwicklungsumgebung unter Windows 11 einrichten.

.DESCRIPTION
  1. Prüft Git und JDK 25 und installiert sie bei Bedarf über winget.
  2. Klont oder aktualisiert das Repository.
  3. Führt alle Tests und die Inhaltsprüfung aus und baut die Fabric-Mod.
  4. Legt eine getrennte Test-Instanz an (mods-Ordner mit FROSTHAIN und Fabric API).
  5. Startet auf Wunsch den UI-Prototyp (-Prototype).

  Verändert weder %APPDATA%\.minecraft noch bestehende Welten oder Launcher-Profile.
  Stand: 01.11.2026 – auf Windows noch nicht ausgeführt (siehe docs/11-windows-test-und-uebergabe.md).
#>
param(
    [string]$Branch = "claude/ui-prototyp",
    [string]$Target = (Join-Path $env:USERPROFILE "Frosthain"),
    [switch]$Prototype
)

$ErrorActionPreference = "Stop"
$Repo = "https://github.com/danilohuth888-create/Frosthain.git"
$FabricApi = "0.165.0+26.3"
$Instance = Join-Path $env:LOCALAPPDATA "Frosthain\test-instance"

function Step([string]$Text) { Write-Host "`n==> $Text" -ForegroundColor Yellow }
function Have([string]$Command) { [bool](Get-Command $Command -ErrorAction SilentlyContinue) }
function Run([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Fehlgeschlagen ($LASTEXITCODE): $Exe $($Arguments -join ' ')" }
}
function JavaMajor {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { $out = (& java -version 2>&1 | Out-String) } catch { $out = "" } finally { $ErrorActionPreference = $previous }
    if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    return 0
}

Step "Voraussetzungen prüfen"
if (-not (Have "git")) {
    Run "winget" @("install", "--id", "Git.Git", "-e", "--source", "winget", "--accept-package-agreements", "--accept-source-agreements")
    $env:Path = "$env:ProgramFiles\Git\cmd;$env:Path"
}
if ((Have "java") -and (JavaMajor) -eq 25) {
    Write-Host "JDK 25 gefunden."
} else {
    Write-Host "JDK 25 wird installiert (Eclipse Temurin)…"
    Run "winget" @("install", "--id", "EclipseAdoptium.Temurin.25.JDK", "-e", "--source", "winget", "--accept-package-agreements", "--accept-source-agreements")
    $jdk = Get-ChildItem (Join-Path $env:ProgramFiles "Eclipse Adoptium") -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jdk) { throw "JDK 25 nicht gefunden. Bitte manuell installieren: https://adoptium.net (Version 25) und Skript erneut starten." }
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$($jdk.FullName)\bin;$env:Path"
}

Step "Repository holen ($Branch)"
if (Test-Path (Join-Path $Target ".git")) {
    Run "git" @("-C", $Target, "fetch", "origin", $Branch)
    Run "git" @("-C", $Target, "checkout", $Branch)
    Run "git" @("-C", $Target, "pull", "--ff-only", "origin", $Branch)
} else {
    Run "git" @("clone", "--branch", $Branch, $Repo, $Target)
}

Push-Location $Target
try {
    Step "Tests, Inhaltsprüfung und Mod-Build"
    Run ".\gradlew.bat" @("check", ":mod:build", "--console=plain")

    Step "Test-Instanz vorbereiten: $Instance"
    $mods = Join-Path $Instance "mods"
    New-Item -ItemType Directory -Force -Path $mods | Out-Null
    Get-ChildItem $mods -Filter "frosthain-*.jar" -ErrorAction SilentlyContinue | Remove-Item
    $jar = Get-ChildItem "mod\build\libs" -Filter "frosthain-*.jar" | Where-Object { $_.Name -notlike "*-sources.jar" } | Select-Object -First 1
    Copy-Item $jar.FullName $mods
    $apiFile = Join-Path $mods "fabric-api-$FabricApi.jar"
    if (-not (Test-Path $apiFile)) {
        $encoded = $FabricApi -replace '\+', '%2B'
        Invoke-WebRequest -Uri "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$encoded/fabric-api-$encoded.jar" -OutFile $apiFile
    }
    Get-ChildItem $mods | ForEach-Object { Write-Host "  mods\$($_.Name)" }

    if ($Prototype) {
        Step "UI-Prototyp starten (Fenster schließen zum Beenden)"
        Run ".\gradlew.bat" @(":ui-prototype:run", "--console=plain")
    }
} finally {
    Pop-Location
}

Step "Fertig"
Write-Host @"
Nächste Schritte (manuell, siehe docs/11-windows-test-und-uebergabe.md):
  1. Fabric-Installer von https://fabricmc.net/use/installer/ starten: Client, Minecraft 26.3, Loader 0.20.1.
  2. Im offiziellen Minecraft Launcher die Installation "fabric-loader-0.20.1-26.3" bearbeiten
     und als Spielverzeichnis eintragen: $Instance
  3. Spielen, Einzelspielerwelt erstellen, danach in $Instance\logs\latest.log nach
     "FROSTHAIN geladen" suchen (erwartet: 12 Klassen … 0 Fehler).
UI-Prototyp jederzeit: cd "$Target"; .\gradlew.bat :ui-prototype:run
"@
