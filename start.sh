#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Hitta en Java 8. JDK8_HOME vinner om den är satt, annars letas en JDK 8 upp på
# de vanligaste platserna i Linux, macOS och Windows (körs då från Git Bash).
if [ -z "$JDK8_HOME" ]; then
    for candidate in \
        "$HOME/.jdks"/*1.8* \
        "$HOME/jdks"/jdk8* \
        "$HOME/.sdkman/candidates/java"/*1.8* \
        /usr/lib/jvm/*1.8* \
        /usr/lib/jvm/java-8* \
        /Library/Java/JavaVirtualMachines/*1.8*/Contents/Home \
        "/c/Program Files/Java"/jdk1.8* \
        "/c/Program Files (x86)/Java"/jdk1.8* ; do
        if [ -x "$candidate/bin/javac" ] || [ -x "$candidate/bin/javac.exe" ]; then
            JDK8_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "$JDK8_HOME" ]; then
    echo "Fel: Hittade ingen Java 8."
    echo "Sätt JDK8_HOME till din JDK 8 och kör igen, till exempel:"
    echo "  Linux:   export JDK8_HOME=\$HOME/.jdks/liberica-full-1.8.0_504"
    echo "  macOS:   export JDK8_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_412.jdk/Contents/Home"
    echo "  Windows: export JDK8_HOME=\"/c/Program Files/Java/jdk1.8.0_412\"   (i Git Bash)"
    exit 1
fi

# På Windows heter filerna java.exe och javac.exe
EXE=""
if [ -x "$JDK8_HOME/bin/javac.exe" ]; then
    EXE=".exe"
fi

export JAVA_HOME="$JDK8_HOME"
JAVA_BIN="$JDK8_HOME/bin/java$EXE"
JAVAC_BIN="$JDK8_HOME/bin/javac$EXE"
SRC_DIR="$DIR/WigellAutoCore/autocore/src"
RES_DIR="$DIR/WigellAutoCore/autocore/src/resources"
OUT_DIR="$DIR/out/production/Systemarkitektur"
JDBC_JAR="$DIR/WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar"

# Startklassen går att välja: ./start.sh kör GUI:t, ./start.sh ConsoleApp kör textversionen.
MAIN_CLASS="${1:-Main}"
shift || true

mkdir -p "$OUT_DIR"

# Kopiera resurser (CSS-teman, JSON-språkfiler etc.) till out
if [ -d "$RES_DIR" ]; then
    cp -r "$RES_DIR"/* "$OUT_DIR"/ 2>/dev/null || true
fi

# Samla källfilerna i en lista. Tål mellanslag i sökvägen, och fungerar även på
# macOS inbyggda bash 3.2 som saknar mapfile.
SOURCES=()
while IFS= read -r source_file; do
    SOURCES+=("$source_file")
done < <(find "$SRC_DIR" -name "*.java")

# Drivrutinen måste ligga på klassvägen både vid kompilering och vid körning,
# annars svarar appen "No suitable driver found for jdbc:sqlite".
"$JAVAC_BIN" -d "$OUT_DIR" -sourcepath "$SRC_DIR:$RES_DIR" -cp "$JDBC_JAR" "${SOURCES[@]}"

exec "$JAVA_BIN" -cp "$OUT_DIR:$JDBC_JAR" "$MAIN_CLASS" "$@"
