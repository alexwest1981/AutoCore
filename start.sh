#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# ==============================================================================
# The build belongs to Maven, the same way IntelliJ builds the project. JDK 25
# and Maven 3.9 or later are required; pom.xml stops the build with a readable
# message when the JDK is something else.
# ==============================================================================

POM="WigellAutoCore/autocore/pom.xml"
REQUIRED_JAVA=25

fail() {
    echo "Fel: $1" >&2
    exit 1
}

warn() {
    echo "Varning: $1" >&2
}

if [ $# -gt 0 ]; then
    fail "start.sh startar bara appen. Argumentet '$1' stöds inte; kör appen från IntelliJ eller 'mvn -f $POM spring-boot:run' direkt."
fi

command -v mvn >/dev/null 2>&1 || fail "hittar ingen mvn. Installera Maven 3.9 eller senare."

command -v java >/dev/null 2>&1 || fail "hittar ingen java. Installera JDK $REQUIRED_JAVA."

# On the old numbering the line reads 1.x, where x is the real major version.
JAVA_LINE="$(java -version 2>&1 | head -n 1)"
JAVA_VERSION="$(echo "$JAVA_LINE" | sed -E 's/.*version "([0-9]+)(\.([0-9]+))?.*/\1 \3/')"
set -- $JAVA_VERSION
JAVA_MAJOR="$1"
if [ "$JAVA_MAJOR" = "1" ]; then
    JAVA_MAJOR="$2"
fi

if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" -lt "$REQUIRED_JAVA" ]; then
    fail "kör Java ${JAVA_MAJOR:-okänd}, men bygget kräver JDK $REQUIRED_JAVA. Byt JDK, till exempel med 'mise use java@25' i projektmappen."
fi

# On Linux with Wayland, JavaFX needs XWayland (GDK_BACKEND=x11) to avoid a crash in GTK.
case "$(uname -s 2>/dev/null || echo unknown)" in
    Darwin*|CYGWIN*|MINGW*|MSYS*) ;;
    *) export GDK_BACKEND="${GDK_BACKEND:-x11}" ;;
esac

# JavaFX draws through GTK here, so GDK_SCALE decides the size of every button and
# every font. A session started before the monitor scale was lowered keeps the old
# value, and then the whole window is drawn twice its size.
if [ -n "${GDK_SCALE:-}" ] && [ "$GDK_SCALE" != "1" ]; then
    warn "GDK_SCALE=$GDK_SCALE ärvs in i JavaFX och ritar gränssnittet $GDK_SCALE gånger
         så stort. Stämmer det inte med skärmarnas scale är värdet kvar från en äldre
         session: logga ut och in, eller starta med 'GDK_SCALE=1 ./start.sh'."
fi

exec mvn -f "$POM" spring-boot:run \
    -Dspring-boot.run.jvmArguments="--enable-native-access=ALL-UNNAMED"
