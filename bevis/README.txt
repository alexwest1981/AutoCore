Baslinjen för Wigell AutoCore 4.0
=================================

Tagen:  2026-10-20
Gren:   develop, commit ce1fde8 ("grindar: skriptet tas bort")
Java:   BellSoft Liberica 1.8.0_504 (Java 8, med JavaFX)

Varför ce1fde8 och inte toppen av develop: ce1fde8 är sista commit innan
Maven-bygget (#192) mergades in. Efter den finns ingen Java 8-miljö kvar i
grenen, och då går det inte längre att spela in hur koden betedde sig före
plattformsbytet.

Databasen är en kopia av data/autocore.db tagen samma dag. Den riktiga filen
rördes inte. Prov som behöver en databas fick kopian som första argument;
prov som bygger sin egen bas skapade en tillfällig fil.

Så kördes proven
----------------

Kompilering, från en arbetskopia på ce1fde8:

  javac -d out -sourcepath WigellAutoCore/autocore/src \
        -cp "WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar:WigellAutoCore/autocore/lib/json-20231013.jar" \
        $(find WigellAutoCore/autocore/src -name '*.java')
  (resurserna, alltså .sql, .css och .json, kopierades bredvid klasserna
   på samma sätt som start.sh gör)

Körning, en per prov:

  java -cp "out:<lib-jararna>" <provklass> [argument]

  WorkOrderProof          <kopia av databasen> 1 1 1
  RuleViolationProof      <kopia av databasen> 1 1 1
  DraftConfirmationProof  <kopia av databasen> 1 1 1
  NumberRulesProof        <kopia av databasen> 1
  WorkOrderRowsProof      <kopia av databasen>
  OldDatabaseProof        WigellAutoCore/autocore/test/data/autocore-original.db
  BookingChangeProof      (bygger sin egen databas)
  CancelledOrdersProof    (bygger sin egen databas)
  FlowProof               (bygger sin egen databas)
  StatusRoutesProof       (bygger sin egen databas)
  ErrorBoundaryProof      (bygger sin egen databas)

Alla elva avslutades med exitkod 0. Utskriften i varje fil är provets eget
svar, ordagrant som det skrevs.

Vad filerna används till
------------------------

E5-2 jämför de här filerna med samma prov körda på den nya plattformen. Det
som skiljer sig är det som behöver förklaras innan 4.0 går in i main.
