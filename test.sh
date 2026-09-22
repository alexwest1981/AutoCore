#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK8_HOME="${JDK8_HOME:-/home/alex/jdks/jdk8u504-full}"

if [ ! -d "$JDK8_HOME" ]; then
    echo "Fel: Kunde inte hitta Liberica JDK 8 på $JDK8_HOME"
    exit 1
fi

export JAVA_HOME="$JDK8_HOME"
JAVA_BIN="$JAVA_HOME/bin/java"
JAVAC_BIN="$JAVA_HOME/bin/javac"
SRC_DIR="$DIR/WigellAutoCore/autocore/src"
RES_DIR="$DIR/WigellAutoCore/autocore/src/resources"
OUT_DIR="$DIR/out/production/Systemarkitektur"
JDBC_JAR="$DIR/WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar"

mkdir -p "$OUT_DIR"

# Kompilera alla källfiler inklusive tester
mapfile -t SOURCES < <(findProjektarbete feature/persistens-grund  ? ❯ git status
                           On branch feature/persistens-grund
                           Your branch is up to date with 'origin/feature/persistens-grund'.

                           Changes not staged for commit:
                             (use "git add <file>..." to update what will be committed)
                             (use "git restore <file>..." to discard changes in working directory)
                                   modified:   test.sh

                           Untracked files:
                             (use "git add <file>..." to include in what will be committed)
                                   WigellAutoCore/autocore/lib/

                           no changes added to commit (use "git add" and/or "git commit -a") "$SRC_DIR" -name "*.java")
"$JAVAC_BIN" -d "$OUT_DIR" -sourcepath "$SRC_DIR:$RES_DIR" -cp "$JDBC_JAR" "${SOURCES[@]}"

# Kör test-runner
exec "$JAVA_BIN" -cp "$OUT_DIR:$JDBC_JAR" com.wac.autocore.test.TestRunner "$@"