# PowerShell test runner för Windows
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# ==============================================================================
# VARFÖR JAVA 8 (JDK 8)?
# Projektets arkitektur- och kurskriterier kräver att den befintliga Java-
# versionen (Java 8) bibehålls. Den JDK som används måste dessutom innehålla
# JavaFX (t.ex. BellSoft Liberica JDK 8 Full eller motsvarande distribution
# med inbyggd JavaFX-runtime).
# ==============================================================================

# 1. Hitta Java 8 JDK
$foundJdk = $null

if ($env:JDK8_HOME -and (Test-Path "$env:JDK8_HOME\bin\javac.exe")) {
    $foundJdk = $env:JDK8_HOME
}

if (-not $foundJdk -and $env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $ver = & "$env:JAVA_HOME\bin\java.exe" -version 2>&1 | Select-Object -First 1
    if ($ver -match '1\.8|"8\.') {
        $foundJdk = $env:JAVA_HOME
    }
}

if (-not $foundJdk) {
    $whereJavac = Get-Command javac -ErrorAction SilentlyContinue
    if ($whereJavac) {
        $ver = & java -version 2>&1 | Select-Object -First 1
        if ($ver -match '1\.8|"8\.') {
            $foundJdk = Split-Path -Parent (Split-Path -Parent $whereJavac.Source)
        }
    }
}

if (-not $foundJdk) {
    $candidates = @(
        "C:\Program Files\BellSoft\LibericaJDK-8-Full",
        "C:\Program Files\BellSoft\LibericaJDK-8",
        "C:\Program Files\Eclipse Adoptium\jdk-8*",
        "C:\Program Files\Zulu\zulu-8*",
        "C:\Program Files\Java\jdk1.8*",
        "C:\Program Files\Amazon Corretto\jdk1.8*",
        "C:\Program Files (x86)\BellSoft\LibericaJDK-8-Full",
        "C:\Program Files (x86)\Java\jdk1.8*",
        "$HOME\.jdks\liberica-full-1.8*",
        "$HOME\.jdks\jdk1.8*",
        "$HOME\jdks\jdk8*",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-8*"
    )

    foreach ($c in $candidates) {
        $matched = Resolve-Path $c -ErrorAction SilentlyContinue
        if ($matched) {
            foreach ($m in $matched) {
                if (Test-Path "$m\bin\javac.exe") {
                    $ver = & "$m\bin\java.exe" -version 2>&1 | Select-Object -First 1
                    if ($ver -match '1\.8|"8\.') {
                        $foundJdk = $m.Path
                        break
                    }
                }
            }
        }
        if ($foundJdk) { break }
    }
}

if (-not $foundJdk) {
    Write-Host "Fel: Hittade ingen Java 8 (JDK 8)." -ForegroundColor Red
    Write-Host "Sätt JDK8_HOME till din installationskatalog för JDK 8, t.ex.:"
    Write-Host '  $env:JDK8_HOME = "C:\Program Files\BellSoft\LibericaJDK-8-Full"'
    exit 1
}

$javaBin = "$foundJdk\bin\java.exe"
$javacBin = "$foundJdk\bin\javac.exe"

$srcDir = "WigellAutoCore\autocore\src"
$resDir = "WigellAutoCore\autocore\src\resources"
$outDir = "out\production\Systemarkitektur"
$jdbcJar = "WigellAutoCore\autocore\lib\sqlite-jdbc-3.53.4.0.jar"

if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

if (Test-Path $resDir) {
    Copy-Item -Path "$resDir\*" -Destination $outDir -Recurse -Force -ErrorAction SilentlyContinue
}

$sourcesFile = "$outDir\sources.txt"
Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName } | Set-Content -Path $sourcesFile

& $javacBin -d $outDir -sourcepath "$srcDir;$resDir" -cp $jdbcJar "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Kompileringsfel!" -ForegroundColor Red
    Remove-Item $sourcesFile -Force -ErrorAction SilentlyContinue
    exit $LASTEXITCODE
}
Remove-Item $sourcesFile -Force -ErrorAction SilentlyContinue

& $javaBin -cp "$outDir;$jdbcJar" com.wac.autocore.test.TestRunner $args
exit $LASTEXITCODE
