#!/usr/bin/env bash
# Grindarna för Wigell AutoCore 4.0. Körs från repots rot.
#
#   bash grindar/kor.sh 0     bara grind 0
#   bash grindar/kor.sh 3     grind 0 till 3, i ordning
#   bash grindar/kor.sh       alla grindar
#
# Exitkod 0 betyder att den sista grinden är grön. Spara utskriften i resultatfilen:
#
#   bash grindar/kor.sh 1 | tee grindar/resultat/1.txt
#
# Fungerar på Linux, macOS och Windows. På Windows körs det i Git Bash, alltså det skal
# som följer med Git for Windows.
#
# Java 8 hittas via JAVA8_HOME. Sätt den innan du kör, till exempel:
#
#   Linux:    export JAVA8_HOME="$HOME/.jdks/liberica-full-1.8.0_504"
#   macOS:    export JAVA8_HOME="$(/usr/libexec/java_home -v 1.8)"
#   Windows:  export JAVA8_HOME="/c/Program Files/Java/jdk1.8.0_401"
#
# Grinden mäter det som är mergat, inte det som är skrivet. Är den röd är det rätt svar
# så länge arbetet inte är klart.

set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Sökvägsavskiljaren i classpath skiljer sig mellan Windows och de andra två.
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *)                    SEP=':' ;;
esac

SRC=WigellAutoCore/autocore/src
LIB="WigellAutoCore/autocore/lib/*"
OUT=out/production/Projektarbete
CP="$OUT$SEP$LIB$SEP$SRC"
OLD_DB=WigellAutoCore/autocore/test/data/autocore-original.db
DB="${AUTOCORE_DB:-data/autocore.db}"

if [ -x ./mvnw ]; then MVN=./mvnw; else MVN=mvn; fi

grön() { echo "GRÖN: $1"; }
röd() { echo "RÖD: $1"; exit 1; }

java8_home() {
  [ -n "$JAVA8_HOME" ] || röd "JAVA8_HOME är inte satt, se raden högst upp i den här filen"
  [ -d "$JAVA8_HOME" ] || röd "hittar ingen Java 8 i $JAVA8_HOME"
  "$JAVA8_HOME/bin/java" -version 2>&1 | grep -q 'version "1\.8' \
    || röd "$JAVA8_HOME är inte Java 8"
  echo "$JAVA8_HOME"
}

# En kopia, så att grinden aldrig rör databasen i repot.
kopia="${TMPDIR:-/tmp}/autocore-grind-$$.db"
trap 'rm -f "$kopia"' EXIT
[ -f "$DB" ] || röd "hittar ingen databas på $DB"
cp "$DB" "$kopia"

grind0() {
  echo "== Grind 0: baslinjen"
  J8=$(java8_home)
  "$J8/bin/javac" -cp "$LIB" -d "$OUT" $(find "$SRC" -name '*.java') \
    || röd "koden kompilerar inte på Java 8"
  antal=0
  prova() {
    echo "kör $1"
    "$J8/bin/java" -cp "$CP" "$@" || röd "$1"
    antal=$((antal + 1))
  }
  prova com.wac.autocore.test.WorkOrderProof "$kopia" 1 1 1
  prova com.wac.autocore.test.RuleViolationProof "$kopia" 1 1 1
  prova com.wac.autocore.test.DraftConfirmationProof "$kopia" 1 1 1
  prova com.wac.autocore.test.NumberRulesProof "$kopia" 1
  prova com.wac.autocore.test.WorkOrderRowsProof "$kopia"
  prova com.wac.autocore.test.OldDatabaseProof "$OLD_DB"
  prova com.wac.autocore.test.BookingChangeProof
  prova com.wac.autocore.test.CancelledOrdersProof
  prova com.wac.autocore.test.FlowProof
  prova com.wac.autocore.test.StatusRoutesProof
  prova com.wac.autocore.test.ErrorBoundaryProof
  [ "$antal" -eq 11 ] || röd "körde $antal prov, skulle vara 11"
  grön "elva prov på Java 8"
}

grind1() {
  echo "== Grind 1: bygget"
  [ -f pom.xml ] || röd "ingen pom.xml"
  "$MVN" -q -DskipTests package || röd "mvn package"
  [ -f target/classes/com/wac/autocore/Main.class ] || röd "Main.class saknas i target/classes"
  antal=$(find target/classes -name '*.sql' | wc -l | tr -d ' ')
  [ "$antal" -ge 3 ] || röd "$antal SQL-filer på classpath, skulle vara minst 3"
  antal=$(find target/classes -name '*.css' | wc -l | tr -d ' ')
  [ "$antal" -ge 11 ] || röd "$antal CSS-filer på classpath, skulle vara minst 11"
  grön "mvn package, och resurserna ligger på classpath"
}

grind2() {
  echo "== Grind 2: fristående läge utan Spring"
  ut=$("$J8/bin/java" -cp "target/classes$SEP$LIB" \
        com.wac.autocore.standalone.StandaloneProof "$kopia" 2>&1) \
    || { echo "$ut"; röd "programmet körde inte"; }
  echo "$ut"
  echo "$ut" | grep -q STANDALONE_OK || röd "programmet skrev inte STANDALONE_OK"
  grön "två operationer i kärnan, utan Spring på classpath"
}

grind3() {
  echo "== Grind 3: inloggning och behörighet"
  "$MVN" -q test -Dtest='LoginTest,AuthorizationTest' -DfailIfNoTests=true \
    || röd "inloggning eller behörighet"
  grön "tre konton loggar in, och nekad åtkomst nekas utanför fönstret"
}

grind4() {
  echo "== Grind 4: loggen"
  "$MVN" -q test -Dtest='LogContentTest' -DfailIfNoTests=true || röd "loggens innehåll"
  grön "trace-id i varje rad, och inga hemligheter i loggen"
}

grind5() {
  echo "== Grind 5: beviset"
  [ -f DEMO.md ] || röd "inget demomanus, DEMO.md saknas"
  grön "demomanuset finns"
}

mal="${1:-5}"
case "$mal" in [0-5]) ;; *) röd "vet inte vad grind $mal är, välj 0 till 5" ;; esac

J8=$(java8_home)
for i in $(seq 0 "$mal"); do "grind$i"; done
echo "Klart: grind 0 till $mal är gröna."
