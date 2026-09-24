#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# ==============================================================================
# VARFÖR JAVA 8 (JDK 8)?
# Projektets arkitektur- och kurskriterier kräver att den befintliga Java-
# versionen (Java 8) bibehålls. Den JDK som används måste dessutom innehålla
# JavaFX (t.ex. BellSoft Liberica JDK 8 Full eller motsvarande distribution
# med inbyggd JavaFX-runtime).
# ==============================================================================

# 1. Identifiera operativsystem och sätt rätt klassvägsseparator
IS_WINDOWS=false
IS_MACOS=false
CP_SEP=":"

case "$(uname -s 2>/dev/null || echo "unknown")" in
    CYGWIN*|MINGW*|MSYS*)
        IS_WINDOWS=true
        CP_SEP=";"
        ;;
    Darwin*)
        IS_MACOS=true
        CP_SEP=":"
        ;;
    *)
        CP_SEP=":"
        ;;
esac

# Hjälpfunktion för att kontrollera om en sökväg är en giltig Java 8 JDK
is_jdk8() {
    local home="$1"
    [ -n "$home" ] || return 1
    local javac="$home/bin/javac"
    local java="$home/bin/java"
    [ -x "$javac" ] || [ -x "$javac.exe" ] || return 1
    [ -x "$java" ] || [ -x "$java.exe" ] || return 1
    local ver
    ver="$("$java" -version 2>&1 | head -n 1)"
    echo "$ver" | grep -q '1\.8\|"8\.' || return 1
    return 0
}

# 2. Hitta en Java 8 JDK
# Prioritet:
# 1. Redan satt JDK8_HOME
# 2. Redan satt JAVA_HOME (om Java 8)
# 3. macOS /usr/libexec/java_home
# 4. Befintlig 'java' och 'javac' i PATH (om Java 8)
# 5. Kända installationsvägar per operativsystem (Linux, macOS, Windows)

FOUND_JDK=""

if [ -n "$JDK8_HOME" ] && is_jdk8 "$JDK8_HOME"; then
    FOUND_JDK="$JDK8_HOME"
elif [ -n "$JAVA_HOME" ] && is_jdk8 "$JAVA_HOME"; then
    FOUND_JDK="$JAVA_HOME"
fi

if [ -z "$FOUND_JDK" ] && [ "$IS_MACOS" = true ] && [ -x "/usr/libexec/java_home" ]; then
    MAC_CANDIDATE="$(/usr/libexec/java_home -v 1.8 2>/dev/null || /usr/libexec/java_home -v 8 2>/dev/null || true)"
    if [ -n "$MAC_CANDIDATE" ] && is_jdk8 "$MAC_CANDIDATE"; then
        FOUND_JDK="$MAC_CANDIDATE"
    fi
fi

if [ -z "$FOUND_JDK" ]; then
    if command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
        PATH_JAVA_VER="$(java -version 2>&1 | head -n 1)"
        if echo "$PATH_JAVA_VER" | grep -q '1\.8\|"8\.'; then
            JAVAC_REAL="$(command -v javac)"
            while [ -L "$JAVAC_REAL" ]; do
                JAVAC_DIR="$(cd "$(dirname "$JAVAC_REAL")" && pwd)"
                JAVAC_REAL="$(readlink "$JAVAC_REAL")"
                [[ "$JAVAC_REAL" != /* ]] && JAVAC_REAL="$JAVAC_DIR/$JAVAC_REAL"
            done
            BIN_DIR="$(cd "$(dirname "$JAVAC_REAL")" && pwd)"
            CANDIDATE_HOME="$(cd "$BIN_DIR/.." && pwd)"
            if is_jdk8 "$CANDIDATE_HOME"; then
                FOUND_JDK="$CANDIDATE_HOME"
            fi
        fi
    fi
fi

if [ -z "$FOUND_JDK" ]; then
    # Samla sökplatser utan att globb-mönster som inte matchar kraschar skriptet
    CANDIDATES=(
        # Linux & allmänna
        "$HOME/.jdks"/jdk8*
        "$HOME/.jdks"/*1.8*
        "$HOME/.jdks"/*8*
        "$HOME/jdks"/jdk8*
        "$HOME/jdks"/*1.8*
        "$HOME/jdks"/*8*
        "$HOME/.sdkman/candidates/java"/*1.8*
        "$HOME/.sdkman/candidates/java"/*8*
        /usr/lib/jvm/*1.8*
        /usr/lib/jvm/java-8*
        /usr/lib/jvm/*jdk-8*
        /usr/lib/jvm/*liberica*8*
        /usr/lib/jvm/*zulu*8*

        # macOS
        /Library/Java/JavaVirtualMachines/*/Contents/Home
        /opt/homebrew/opt/openjdk@8
        /opt/homebrew/opt/openjdk@8/libexec/openjdk.jdk/Contents/Home
        /usr/local/opt/openjdk@8
        /usr/local/opt/openjdk@8/libexec/openjdk.jdk/Contents/Home
        "$HOME/.asdf/installs/java"/*1.8*
        "$HOME/.asdf/installs/java"/*8*

        # Windows (Git Bash / MSYS)
        "/c/Program Files/BellSoft"/*8*
        "/c/Program Files/BellSoft"/*
        "/c/Program Files/Java"/*8*
        "/c/Program Files/Java"/jdk1.8*
        "/c/Program Files/Eclipse Adoptium"/*8*
        "/c/Program Files/Eclipse Adoptium"/*
        "/c/Program Files/Zulu"/*8*
        "/c/Program Files/Amazon Corretto"/*8*
        "/c/Program Files (x86)/BellSoft"/*8*
        "/c/Program Files (x86)/Java"/*1.8*
        "$LOCALAPPDATA/Programs/Eclipse Adoptium"/*8*
        "$USERPROFILE/.jdks"/*8*
        "$USERPROFILE/jdks"/*8*
    )

    for c in "${CANDIDATES[@]}"; do
        if is_jdk8 "$c"; then
            FOUND_JDK="$c"
            break
        fi
    done
fi

if [ -z "$FOUND_JDK" ]; then
    echo "Fel: Hittade ingen Java 8 (JDK 8)."
    echo "Sätt JDK8_HOME till din Java 8-katalog och kör igen, till exempel:"
    echo "  Linux:   export JDK8_HOME=\$HOME/.jdks/liberica-full-1.8.0_504"
    echo "  macOS:   export JDK8_HOME=/Library/Java/JavaVirtualMachines/liberica-jdk8-full.jdk/Contents/Home"
    echo "           (eller kör: export JDK8_HOME=\$(/usr/libexec/java_home -v 1.8))"
    echo "  Windows: export JDK8_HOME=\"/c/Program Files/BellSoft/LibericaJDK-8-Full\" (i Git Bash)"
    exit 1
fi

export JDK8_HOME="$FOUND_JDK"
export JAVA_HOME="$FOUND_JDK"

EXE=""
if [ -x "$FOUND_JDK/bin/javac.exe" ]; then
    EXE=".exe"
fi

JAVA_BIN="$FOUND_JDK/bin/java$EXE"
JAVAC_BIN="$FOUND_JDK/bin/javac$EXE"

SRC_DIR="WigellAutoCore/autocore/src"
RES_DIR="WigellAutoCore/autocore/src/resources"
OUT_DIR="out/production/Systemarkitektur"
JDBC_JAR="WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar"

mkdir -p "$OUT_DIR"

# Kopiera resurser (CSS-teman, JSON-språkfiler etc.) till out
if [ -d "$RES_DIR" ]; then
    cp -R "$RES_DIR/." "$OUT_DIR/" 2>/dev/null || cp -r "$RES_DIR"/* "$OUT_DIR"/ 2>/dev/null || true
fi

# Bygg källkodslistan via argumentfil (@sources.txt) för full kompabilitet med alla skal och Windows
SOURCES_FILE="$OUT_DIR/sources.txt"
find "$SRC_DIR" -name "*.java" > "$SOURCES_FILE"

# Kompilera med rätt plattformsseparator (: på Unix, ; på Windows)
"$JAVAC_BIN" -d "$OUT_DIR" -sourcepath "$SRC_DIR$CP_SEP$RES_DIR" -cp "$JDBC_JAR" @"$SOURCES_FILE"
rm -f "$SOURCES_FILE"

# Kör test-runner med rätt klassväg
"$JAVA_BIN" -cp "$OUT_DIR$CP_SEP$JDBC_JAR" com.wac.autocore.test.TestRunner "$@"
