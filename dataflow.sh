#!/usr/bin/env bash
# ==============================================================================
# DATAFLÖDESANALYS (AK-15) - EGET SKRIPT, VID SIDAN AV test.sh
# ==============================================================================
# DataFlowAuditTest följer var en sträng kommer ifrån, fram till dess att den blir
# en databasfråga eller ett processanrop. Den analyserar kompilatorns eget träd
# (com.sun.source) och behöver därför JDK:ns tools.jar på klassvägen.
#
# Skriptet ligger utanför testsviten med flit: analysen är långsammare än resten och
# känslig för vilken katalog den startas ifrån. Här sätts katalogen alltid till
# skriptets egen, och klassen kompileras till out/dataflow, så att sviten i
# out/production/Systemarkitektur inte rörs.
#
#   ./dataflow.sh                  kör analysen
#   ./dataflow.sh --jdk <sökväg>   ange Java 8 JDK manuellt
# ==============================================================================

set -o pipefail

# Analysen läser källkoden relativt arbetskatalogen: stå alltid i projektroten.
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

# ── Java 8: analysen behöver kompilatorn (javac), som bara finns i ett JDK ─────
CLI_JDK=""
if [ "$1" = "--jdk" ] || [ "$1" = "-j" ]; then
    CLI_JDK="$2"
fi

is_jdk8() {
    [ -n "$1" ] || return 1
    [ -x "$1/bin/javac" ] && [ -x "$1/bin/java" ] || return 1
    "$1/bin/java" -version 2>&1 | head -n 1 | grep -q '1\.8\|"8\.'
}

FOUND_JDK=""
for c in "$CLI_JDK" "$JDK8_HOME" "$JAVA_HOME" \
         "$HOME"/jdks/jdk8* "$HOME"/jdks/*1.8* "$HOME"/.jdks/*8* \
         "$HOME"/.sdkman/candidates/java/*8* /usr/lib/jvm/*8*; do
    if is_jdk8 "$c"; then
        FOUND_JDK="$c"
        break
    fi
done

if [ -z "$FOUND_JDK" ]; then
    echo "Fel: hittade ingen Java 8 (JDK). Ange den med: ./dataflow.sh --jdk \"/sökväg/till/jdk8\""
    exit 1
fi

JAVA_BIN="$FOUND_JDK/bin/java"
JAVAC_BIN="$FOUND_JDK/bin/javac"

SRC_DIR="WigellAutoCore/autocore/src"
RES_DIR="WigellAutoCore/autocore/src/resources"
JDBC_JAR="WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar"
OUT_DIR="out/dataflow"

TOOLS_JAR="$DIR/WigellAutoCore/autocore/lib/tools.jar"
[ -f "$TOOLS_JAR" ] || TOOLS_JAR="$FOUND_JDK/lib/tools.jar"
if [ ! -f "$TOOLS_JAR" ]; then
    echo "Fel: hittade ingen tools.jar (JDK:ns kompilatorklasser). Analysen kan inte köras utan den."
    exit 1
fi

CP_SEP=":"
case "$(uname -s 2>/dev/null || echo unknown)" in
    CYGWIN*|MINGW*|MSYS*) CP_SEP=";" ;;
esac

# ── Kompilering till en egen katalog (svitens out/production rörs inte) ────────
echo "Dataflödesanalys (AK-15) med $FOUND_JDK"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
cp -R "$RES_DIR/." "$OUT_DIR/" 2>/dev/null || cp -r "$RES_DIR"/* "$OUT_DIR"/ 2>/dev/null || true

SOURCES_FILE="$OUT_DIR/sources.txt"
find "$SRC_DIR" -name "*.java" > "$SOURCES_FILE"
if ! "$JAVAC_BIN" -encoding UTF-8 -d "$OUT_DIR" -sourcepath "$SRC_DIR$CP_SEP$RES_DIR" \
        -cp "$JDBC_JAR$CP_SEP$TOOLS_JAR" @"$SOURCES_FILE"; then
    rm -f "$SOURCES_FILE"
    echo "Fel: kompileringen misslyckades (utdata ovan)."
    exit 1
fi
rm -f "$SOURCES_FILE"

# ── Körning ───────────────────────────────────────────────────────────────────
ESC=$(printf '\033')
OUTPUT=$("$JAVA_BIN" -cp "$OUT_DIR$CP_SEP$JDBC_JAR$CP_SEP$TOOLS_JAR" \
    com.wac.autocore.test.TestRunner dataflow 2>&1 \
    | sed "s/${ESC}\[[0-9;]*[A-Za-z]//g")
EXIT_CODE=$?

printf '%s\n' "$OUTPUT"

RES_LINE=$(printf '%s\n' "$OUTPUT" | grep -E "Resultat: [0-9]+ tester körda" | head -n 1)
if [ "$EXIT_CODE" -eq 0 ] && [ -n "$RES_LINE" ]; then
    echo ""
    echo "GODKÄND: $RES_LINE"
    exit 0
fi

echo ""
echo "MISSLYCKAD: ${RES_LINE:-ingen resultatrad från analysen (javas slutkod $EXIT_CODE)}"
exit 1
