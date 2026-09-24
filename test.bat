@echo off
setlocal enabledelayedexpansion

set "DIR=%~dp0"
cd /d "%DIR%"

:: ==============================================================================
:: VARFOR JAVA 8 (JDK 8)?
:: Projektets arkitektur- och kurskriterier kraver att den befintliga Java-
:: versionen (Java 8) bibehalls. Den JDK som anvands maste dessutom innehalla
:: JavaFX (t.ex. BellSoft Liberica JDK 8 Full eller motsvarande distribution
:: med inbyggd JavaFX-runtime).
:: ==============================================================================

:: 1. Hitta Java 8 JDK på Windows
set "FOUND_JDK="
set "JAVA_BIN="
set "JAVAC_BIN="

:: Kontrollera explicit miljövariabel JDK8_HOME
if defined JDK8_HOME if exist "%JDK8_HOME%\bin\javac.exe" (
    set "FOUND_JDK=%JDK8_HOME%"
    goto :jdk_found
)

:: Kontrollera om aktiv java och javac i PATH är Java 8
where javac >nul 2>&1 && (
    java -version 2>&1 | findstr /R "1\.8 \"8\." >nul && (
        set "JAVA_BIN=java"
        set "JAVAC_BIN=javac"
        goto :compile_and_run
    )
)

:: Kontrollera JAVA_HOME
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /R "1\.8 \"8\." >nul && (
        set "FOUND_JDK=%JAVA_HOME%"
        goto :jdk_found
    )
)

:: Sök i vanliga fasta installationskataloger
for %%p in (
    "C:\Program Files\BellSoft\LibericaJDK-8-Full"
    "C:\Program Files\BellSoft\LibericaJDK-8"
    "C:\Program Files (x86)\BellSoft\LibericaJDK-8-Full"
) do (
    if exist "%%~p\bin\javac.exe" (
        "%%~p\bin\java.exe" -version 2>&1 | findstr /R "1\.8 \"8\." >nul && (
            set "FOUND_JDK=%%~p"
            goto :jdk_found
        )
    )
)

:: Sök i kataloger med mönstermatchning (wildcards)
for /d %%p in (
    "C:\Program Files\Eclipse Adoptium\jdk-8*"
    "C:\Program Files\Zulu\zulu-8*"
    "C:\Program Files\Java\jdk1.8*"
    "C:\Program Files\Amazon Corretto\jdk1.8*"
    "C:\Program Files (x86)\Java\jdk1.8*"
    "%USERPROFILE%\.jdks\liberica-full-1.8*"
    "%USERPROFILE%\.jdks\jdk1.8*"
    "%USERPROFILE%\jdks\jdk8*"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-8*"
) do (
    if exist "%%~p\bin\javac.exe" (
        "%%~p\bin\java.exe" -version 2>&1 | findstr /R "1\.8 \"8\." >nul && (
            set "FOUND_JDK=%%~p"
            goto :jdk_found
        )
    )
)

:jdk_found
if defined FOUND_JDK (
    set "JAVA_BIN=%FOUND_JDK%\bin\java.exe"
    set "JAVAC_BIN=%FOUND_JDK%\bin\javac.exe"
)

if not defined JAVA_BIN (
    echo Fel: Hittade ingen Java 8 (JDK 8).
    echo Satt JDK8_HOME till din installationskatalog for JDK 8, t.ex.:
    echo   set JDK8_HOME=C:\Program Files\BellSoft\LibericaJDK-8-Full
    exit /b 1
)

:compile_and_run
set "SRC_DIR=WigellAutoCore\autocore\src"
set "RES_DIR=WigellAutoCore\autocore\src\resources"
set "OUT_DIR=out\production\Systemarkitektur"
set "JDBC_JAR=WigellAutoCore\autocore\lib\sqlite-jdbc-3.53.4.0.jar"

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
if exist "%RES_DIR%" xcopy /E /I /Y "%RES_DIR%\*" "%OUT_DIR%\" >nul 2>&1

set "SOURCES_FILE=%OUT_DIR%\sources.txt"
dir /s /b "%SRC_DIR%\*.java" > "%SOURCES_FILE%"

"%JAVAC_BIN%" -d "%OUT_DIR%" -sourcepath "%SRC_DIR%;%RES_DIR%" -cp "%JDBC_JAR%" @"%SOURCES_FILE%"
if %ERRORLEVEL% neq 0 (
    echo Kompileringsfel!
    del "%SOURCES_FILE%" >nul 2>&1
    exit /b %ERRORLEVEL%
)
del "%SOURCES_FILE%" >nul 2>&1

"%JAVA_BIN%" -cp "%OUT_DIR%;%JDBC_JAR%" com.wac.autocore.test.TestRunner %*
exit /b %ERRORLEVEL%
