# Inkompatibiliteter vid migreringen till 4.0

WAC4_0 §2 säger att eventuella inkompatibiliteter ska identifieras och hanteras, och §11 H att
teamet ska kunna redogöra för dem. Varje rad säger tre saker: vad som hände, var det syntes och hur
det hanterades. Raderna står i den ordning de dök upp under sprinten.

Listan ligger i repot, så den som bara har en klon kan läsa och kontrollera den.

| # | Vad som hände | Var det syntes | Hur det hanterades | Bevis |
|---|---|---|---|---|
| 1 | En intern JavaFX-klass fanns inte längre. `com.sun.javafx.scene.control.skin.DatePickerSkin` följde med JavaFX 8 men är borta i 25, och det som använde den kompilerade inte. | `BookingScheduleField` föll vid kompilering på JDK 25; `start.sh` slutade fungera samtidigt. | Den publika `javafx.scene.control.skin.DatePickerSkin` används i stället, med samma `getPopupContent()`. Veckokolumnen fanns inte i koden före bytet och slås nu på med `setShowWeekNumbers(true)`, alltså en ny synlig kolumn och inte en återställd. | Commit `ee17fc0`, commit `48c153f`, SCRUM-297, [R2](riskanalys.md) |
| 2 | JavaFX 25 är byggt för Java 23. `javafx-base-25.0.4-linux.jar` har class file 67, medan Spring Boot 4 har 61. Under Java 23 går bygget inte alls, och felmeddelandet handlar om class file-versioner i stället för om det som är fel. | `mvn package` på JDK 17 eller 21. | `pom.xml` kräver `[25,26)` med `maven-enforcer-plugin` och svarar med en läsbar rad i stället för ett kompileringsfel. Golvet står också i README. | Commit `0d3bb1f`, [ADR-003](arkitekturbeslut.md#adr-003-jdk-25-är-bygggolvet-trots-att-kravet-säger-17-eller-senare), SCRUM-340. Provat på JDK 21, se [nedan](#så-prövas-raderna-om) |
| 3 | SQLite-drivrutinen använder inbyggd kod. På JDK 25 ger en körning utan flagga varningen "Restricted methods will be blocked in a future release", och en framtida JDK kommer att blockera anropet. | Varje körning av appen, konsolen och det fristående programmet. | `--enable-native-access=ALL-UNNAMED` skickas från `start.sh`, står i `spring-boot-maven-plugin` och i jar-filens manifest. README visar kommandot för den som kör med `java -cp`. `BuildFlagsTest` faller om flaggan försvinner från någon startväg. | Commit `6ee586c`, [ADR-016](arkitekturbeslut.md#adr-016-native-access-deklareras-i-manifestet), SCRUM-334 |
| 4 | Resurserna låg inne i paketmappen och lästes från filsystemet. Det fungerade när programmet startades från projektmappen och inte alls ur en jar-fil. | SQL, JSON och CSS hittades inte när appen kördes från en annan katalog, och Maven hade inget att packa. | De flyttades till `src/main/resources` och läses via `Resources`, som söker på classpath i två former och inte bryr sig om var programmet startas. | Commit `3fb44f4`, SCRUM-295, [R2](riskanalys.md) |
| 5 | Lagringen låg i en statisk `Db` som läste en sökväg satt någon annanstans. Varje repository kom åt den, och proven kunde bara köras för att klassen var statisk. | Sökvägen gick inte att sätta per körning utan att mutera en global, och en Spring-kontext som äger sina bönor kan inte samexistera med det. | `Db` är en böna som får sökvägen från `autocore.db.path`, och varje repository får sin `Db` via `RepositoryFactory`. | Commit `13ee28c`, [ADR-004](arkitekturbeslut.md#adr-004-lagringen-är-en-böna-med-sökvägen-i-konfigurationen), SCRUM-298, [R3](riskanalys.md) |
| 6 | Två byggvägar sida vid sida. `start.sh` kompilerade för hand med `javac` mot två jar-filer i `lib/`, medan Maven byggde samma kod på annat sätt. | Efter att Spring och JavaFX flyttat in i `pom.xml` föll `start.sh` med `package org.springframework.boot does not exist`, alltså långt senare än ändringen som orsakade det. | `start.sh` kör `mvn spring-boot:run`, samma bygge som IntelliJ, och `lib/` används inte längre. | Commit `5043e3b`, SCRUM-296, [R1](riskanalys.md) |
| 7 | Spring läser bara `logback-spring.xml` när en kontext finns. Det fristående programmet och konsolen har ingen Spring-kontext och fick därför standardkonfiguration, utan vårt mönster och utan filen. | Loggraderna från det fristående programmet såg olika ut beroende på hur det startades, och hamnade inte i `logs/autocore.log`. | `logback-standalone.xml` med samma mönster och samma fil laddas först i programmets `main`, för både det fristående programmet och konsolen. | [ADR-010](arkitekturbeslut.md#adr-010-loggning-med-profil-per-miljö-och-trace-id-i-mdc), SCRUM-306, SCRUM-358 |
| 8 | Ärvd `GDK_SCALE` dubblerade hela gränssnittet. JavaFX frågar GTK hur stort allt ska vara, och en session som startats innan skärmens skala sänktes behöll det gamla värdet. Ingen kod hade ändrats. | Vid start i en befintlig terminal: varje knapp, varje typsnitt och hela fönstret ritades dubbelt så stort. Mätt på inloggningskortets yta: 714×810 i stället för 354×405. | `Main` sätter `glass.gtk.uiScale` till 1 innan JavaFX startar, även när appen startas från IntelliJ. `start.sh` sätter `GDK_SCALE` i stället för att ärva det, och `AUTOCORE_UI_SCALE` finns för en skärm som behöver en annan skala. | Commit `71571f8`, [ADR-015](arkitekturbeslut.md#adr-015-skalan-är-1-när-appen-startar), SCRUM-356, [R7](riskanalys.md) |
| 9 | Flera API:er som var i bruk är föråldrade på JDK 25: `Locale(String, String)`-konstruktorn, `TableView.CONSTRAINED_RESIZE_POLICY`, statiska hjälpmetoder som anropades via instansen, och oanvända importer. | 25 varningar i problemlistan, utöver de 46 som rättades vid plattformsbytet. | Rättade: `Locale.of`, den nya konstanten med samma beteende, anropen via klassnamn. En varning står kvar och var felaktig, se commiten. | Commit `4927cf3`, [R2](riskanalys.md) |

## Det som var en ändring och inte en inkompatibilitet

Tre saker såg ut som inkompatibiliteter men var något annat. De står här för att ingen ska leta
efter dem i listan ovan.

- `lib/`-jarna togs bort. De var kompatibla, men onödiga när Maven hämtar samma filer.
- Bevisprogrammen flyttade ur produktionsträdet. De kompilerade hela tiden, men låg i `src/main`
  och följde med i jar-filen, där tio av dem byggde tjänsten förbi spärren. Det var en strukturfråga
  ([ADR-014](arkitekturbeslut.md#adr-014-bevisen-ligger-utanför-produktionsträdet)).
- Veckokolumnen i kalendern är ny. Den fanns inte före bytet. Den tillkom när den publika
  `DatePickerSkin` togs i bruk.

## Så prövas raderna om

Rad 2 går att pröva med en äldre JDK på maskinen. Kommandot nedan startar Maven på JDK 21 i stället
för shimmen, som annars alltid väljer 25:

```
M2=~/.local/share/mise/installs/maven/latest/apache-maven-3.9.16
JDK21=~/.local/share/mise/installs/java/21
cd WigellAutoCore/autocore
$JDK21/bin/java -classpath $M2/boot/plexus-classworlds-2.11.0.jar \
  -Dclassworlds.conf=$M2/bin/m2.conf -Dmaven.home=$M2 \
  -Dmaven.multiModuleProjectDirectory=$PWD \
  org.codehaus.plexus.classworlds.launcher.Launcher -o -q validate
```

Får du ett fel om class file-versioner i stället betyder det att enforcer-regeln inte gjorde sitt jobb.

Rad 3 syns genom att köra det fristående programmet utan flaggan:

```
java -cp "WigellAutoCore/autocore/target/classes:$(cat WigellAutoCore/autocore/target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/utan-flagga.db
```

Rad 1, 4 och 9 syns i git-historiken, i commitarna som står i tabellen. Rad 5 till 8 prövas av
`SpringContextTest`, `MainScaleTest`, `LogContentTest` och `BuildFlagsTest`.
