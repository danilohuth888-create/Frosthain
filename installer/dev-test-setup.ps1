<#
.SYNOPSIS
  FROSTHAIN – Entwicklungsumgebung unter Windows einrichten (ohne winget, ohne Git, ohne Adminrechte).

.DESCRIPTION
  1. Sucht ein JDK 25. Fehlt es, wird Eclipse Temurin 25 als ZIP von api.adoptium.net geladen,
     per SHA-256 (Prüfsumme aus der Adoptium-API) geprüft und nach %LOCALAPPDATA%\Frosthain\runtime entpackt.
  2. Lädt den Quellcode des Branches als ZIP von GitHub (vorhandener Ordner wird umbenannt, nicht gelöscht).
  3. Führt Tests und Inhaltsprüfung aus und baut die Mod (Gradle Wrapper, kein separates Gradle nötig).
  4. Optional: startet den UI-Prototyp (-Prototype).

  Für das reine Spielen wird dieses Skript NICHT gebraucht – dafür gibt es install-frosthain.ps1.
#>
[CmdletBinding()]
param(
    [string]$Branch = "claude/ui-prototyp",
    [string]$Target = (Join-Path $env:USERPROFILE "Frosthain"),
    [string]$RuntimeDir = (Join-Path $env:LOCALAPPDATA "Frosthain\runtime"),
    [switch]$Prototype,
    [switch]$JdkOnly
)

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

function Step([string]$Text) { Write-Host "`n==> $Text" -ForegroundColor Yellow }

function Get-JavaMajor([string]$Java) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { $out = (& $Java -version 2>&1 | Out-String) } catch { $out = "" } finally { $ErrorActionPreference = $previous }
    if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    return 0
}

function Ensure-Jdk25 {
    $onPath = Get-Command java -ErrorAction SilentlyContinue
    if ($onPath -and (Get-JavaMajor $onPath.Source) -eq 25) {
        return (Split-Path (Split-Path $onPath.Source -Parent) -Parent)
    }
    # Installierte JDKs, die nicht im PATH stehen (z. B. PATH zeigt auf JDK 17, JDK 25 liegt daneben).
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($vendor in @("Microsoft", "Eclipse Adoptium", "Java", "Zulu", "BellSoft")) {
        $candidates += Get-ChildItem (Join-Path $env:ProgramFiles $vendor) -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { $_.FullName }
    }
    foreach ($candidate in $candidates) {
        $java = Join-Path $candidate "bin\java.exe"
        if ((Test-Path $java) -and (Get-JavaMajor $java) -eq 25) { return $candidate }
    }
    $existing = Get-ChildItem $RuntimeDir -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($existing -and (Test-Path (Join-Path $existing.FullName "bin\java.exe"))) { return $existing.FullName }

    Write-Host "JDK 25 wird von Adoptium geladen (Eclipse Temurin, ca. 200 MB) …"
    $api = "https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse"
    $release = (Invoke-RestMethod -UseBasicParsing -Uri $api) | Select-Object -First 1
    if (-not $release) { throw "Adoptium lieferte kein JDK 25 für Windows x64." }
    $package = $release.binary.package
    New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null
    $zip = Join-Path $RuntimeDir $package.name
    Invoke-WebRequest -UseBasicParsing -Uri $package.link -OutFile $zip
    $actual = (Get-FileHash -Algorithm SHA256 $zip).Hash.ToLowerInvariant()
    if ($actual -ne ([string]$package.checksum).ToLowerInvariant()) {
        Remove-Item $zip -Force
        throw "Prüfsumme des JDK stimmt nicht (erwartet $($package.checksum), erhalten $actual). Abgebrochen."
    }
    Expand-Archive -Path $zip -DestinationPath $RuntimeDir -Force
    Remove-Item $zip -Force
    $jdk = Get-ChildItem $RuntimeDir -Directory -Filter "jdk-25*" | Select-Object -First 1
    if (-not $jdk) { throw "JDK konnte nicht entpackt werden." }
    return $jdk.FullName
}

try {
    Step "JDK 25"
    $jdkHome = Ensure-Jdk25
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$jdkHome\bin;$env:Path"
    Write-Host "JAVA_HOME = $jdkHome (Java $(Get-JavaMajor (Join-Path $jdkHome 'bin\java.exe')))"
    if ($JdkOnly) { exit 0 }

    Step "Quellcode laden ($Branch)"
    $zip = Join-Path ([System.IO.Path]::GetTempPath()) "frosthain-src.zip"
    Invoke-WebRequest -UseBasicParsing -Uri "https://codeload.github.com/danilohuth888-create/Frosthain/zip/refs/heads/$Branch" -OutFile $zip
    $extract = Join-Path ([System.IO.Path]::GetTempPath()) ("frosthain-src-" + [guid]::NewGuid().ToString("N"))
    Expand-Archive -Path $zip -DestinationPath $extract
    if (Test-Path $Target) {
        $old = "$Target.alt-" + (Get-Date -Format "yyyyMMdd-HHmmss")
        Rename-Item $Target $old
        Write-Host "Vorheriger Ordner umbenannt: $old"
    }
    Move-Item (Get-ChildItem $extract -Directory | Select-Object -First 1).FullName $Target
    Remove-Item $zip, $extract -Recurse -Force -ErrorAction SilentlyContinue

    Push-Location $Target
    try {
        Step "Tests, Inhaltsprüfung und Mod-Build"
        & .\gradlew.bat check ":mod:build" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle-Build fehlgeschlagen (Code $LASTEXITCODE)." }
        Write-Host "Mod: $(Get-ChildItem 'mod\build\libs' -Filter 'frosthain-*.jar' | Select-Object -First 1 -ExpandProperty FullName)"
        if ($Prototype) {
            Step "UI-Prototyp"
            & .\gradlew.bat ":ui-prototype:run" --console=plain
        }
    } finally {
        Pop-Location
    }
    Write-Host "`nFertig." -ForegroundColor Green
    exit 0
} catch {
    Write-Host "`nFehler: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
