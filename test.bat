@echo off
setlocal enabledelayedexpansion

set "DIR=%~dp0"
cd /d "%DIR%"

:: 1. Hitta Java 8 JDK på Windows
set "FOUND_JDK="

if defined JDK8_HOME if exist "%JDK8_HOME%\bin\javac.exe" set "FOUND_JDK=%JDK8_HOME%"
if not defined FOUND_JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /R "1\.8 \"8\." >nul && set "FOUND_JDK=%JAVA_HOME%"
)

if not defined FOUND_JDK (
    for %%p in (
        "C:\Program Files\BellSoft\LibericaJDK-8-Full"
        "C:\Program Files\BellSoft\LibericaJDK-8"
        "C:\Program Files\Eclipse Adoptium\jdk-8*"
        "C:\Program Files\Zulu\zulu-8*"
        "C:\Program Files\Java\jdk1.8*"
        "C:\Program Files\Amazon Corretto\jdk1.8*"
        "C:\Program Files (x86)\BellSoft\LibericaJDK-8-Full"
        "C:\Program Files (x86)\Java\jdk1.8*"
        "%USERPROFILE%\.jdks\liberica-full-1.8*"
        "%USERPROFILE%\.jdks\jdk1.8*"
        "%USERPROFILE%\jdks\jdk8*"
        "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-8*"
    ) do (
        if exist "%%~fp\bin\javac.exe" (
            "%~fp\bin\java.exe" -version 2>&1 | findstr /R "1\.8 \"8\." >nul && (
                set "FOUND_JDK=%%~fp"
                goto :jdk_found
            )
        )
    )
)

:jdk_found
if not defined FOUND_JDK (
    echo Fel: Hittade ingen Java 8 ^(JDK 8^).
    echo Satt JDK8_HOME till din installationskatalog for JDK 8, t.ex.:
    echo   set JDK8_HOME=C:\Program Files\BellSoft\LibericaJDK-8-Full
    exit /b 1
)

set "JAVA_BIN=%FOUND_JDK%\bin\java.exe"
set "JAVAC_BIN=%FOUND_JDK%\bin\javac.exe"

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
