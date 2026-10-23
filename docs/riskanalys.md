# Riskanalys för sprint 6, AutoCore 4.0

Registret börjar med de sex risker som skrevs ner innan arbetet började i
[plan-4.0.md](plan-4.0.md), fortsätter med dem som dök upp under sprinten och slutar med dem som
fortfarande är öppna när sprinten stänger 23 oktober 2026.

Planen är fortfarande den löpande berättelsen om migreringen. Här står en rad per risk, med vad som
hände, vad som gjordes och var det går att kontrollera.

Registret har 25 rader. Sju rör säkerhet och behörighet, sex rör bevis och process, och resten
bygget, lagringen, loggningen, gränssnittet och testmiljön. Nio risker är fortfarande öppna och
står i avsnitt 3.

## Hur listan är gjord

Riskerna R1–R6 stod i planen innan någon rad kod ändrades. När en risk slog till uppdaterades
planen samma dag, vilket var kravet i E5-4 (SCRUM-316).

Riskerna R7 och framåt hittades under arbetet. Varje rad har en kort hänvisning till kortet i
Jira, commiten eller filen som visar att risken fanns och att åtgärden gör det den säger. Den som
läser behöver alltså inte lita på sammanställningen, utan kan gå till beviset.

Kolumnerna säger vad risken var, vad som hände och vad som gjordes åt den. En risk som inte är
stängd upprepas i avsnitt 3 med ett O-nummer, så att det som är kvar står på ett ställe.

## 1. Riskerna som skrevs ner före arbetet

| ID | Risk | Utfall | Åtgärd | Bevis |
|---|---|---|---|---|
| R1 | Två byggvägar. `start.sh` kompilerade för hand vid sidan av Maven, så den ena kunde fungera medan den andra var trasig. | **Slog till.** Efter att Spring och JavaFX flyttat in i `pom.xml` föll `start.sh` med `package org.springframework.boot does not exist`. | `start.sh` kör nu `mvn spring-boot:run`, samma bygge som IntelliJ. | [`start.sh`](../start.sh), commit `0d3bb1f`, `BuildFlagsTest` läser startvägarna. |
| R2 | JavaFX 8 mot 25. Gränssnittet, temaarken och resurserna flyttar från filsystemet till classpath, och en del API:er har ändrats. | **Slog delvis till.** Resurserna lästes från filsystemet, `com.sun.javafx.scene.control.skin.DatePickerSkin` fanns inte i JavaFX 25, och 46 varningar fanns i koden. | Resurserna flyttades till `src/main/resources` (SCRUM-295), den publika `javafx.scene.control.skin.DatePickerSkin` används (SCRUM-297), och varningarna rättades. | `util/Resources.java`, commit `3fb44f4`, commit `ee17fc0`, commit `48c153f`. |
| R3 | Statisk `Db`. Lagringen låg i en statisk klass som både gränssnittet och proven muterade, vilket krockar med en kontext som äger sina bönor. | **Slog till.** Varje repository läste en sökväg som sattes någon annanstans, och proven kunde bara köras för att klassen var statisk. | `Db` är en böna med sökvägen i `autocore.db.path`, och varje repository får sin `Db` via `RepositoryFactory` (SCRUM-298). | [`DatabaseConfig.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/config/DatabaseConfig.java), `SpringContextTest`. |
| R4 | Bevisen låg i produktionsträdet. Program som ingen applikation anropar följde med i bygget, och tio av dem byggde tjänsten förbi behörighetsspärren. | **Var sant.** Klasserna låg i `src/main/java/com/wac/autocore/test/`. | De flyttades till `src/test/java/com/wac/autocore/proof/`, utanför jar-filen (SCRUM-332). Surefire hoppar över paketet eftersom `SelfTest` annars hade setts som ett JUnit-prov. | `pom.xml`, [`bevis/README.md`](bevis/README.md). |
| R5 | Hemligheter. Demokonton och konfiguration riskerade att hamna i källkoden eller i loggen. | **Slog inte till.** Migrering 004 skriver PBKDF2-hashar, inte lösenord. | Lösenord kan sättas med `AUTOCORE_ADMIN_PASSWORD`, `AUTOCORE_STAFF_PASSWORD` och `AUTOCORE_READONLY_PASSWORD` och hashas vid start. `SecretScanTest` läser allt git spårar och letar efter formen en hemlighet har när den skrivs ner, och `LogContentTest` letar hemligheter och personuppgifter i loggen. Provet kan inte avgöra om ett värde är en hemlighet, så uppgiften kontrollerades också för hand. | [`application.properties`](../WigellAutoCore/autocore/src/main/resources/application.properties), `SecretScanTest`, `LogContentTest`. |
| R6 | Behörighet som bara göms. Om kontrollen sitter i gränssnittet går den att gå runt. | **Slog delvis till.** Spärren byggdes i kärnan, men täckte först bara skrivningarna. Läsningarna gick fritt till E3-7, och konsolen var en egen väg förbi ända till SCRUM-357. | Dekoratören `GuardedGarageSystem` framför fasaden täcker både läsning och skrivning (SCRUM-304, SCRUM-320, SCRUM-337). Konsolen loggar in och går genom samma spärr. `NoUnguardedConstructionTest` fäller en klass som skapar `new GarageSystem(` direkt. | [`GuardedGarageSystem.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/service/GuardedGarageSystem.java), `NoUnguardedConstructionTest`, commit `6575338`. |

## 2. Riskerna som hittades under sprinten

| ID | Risk | Vad som hände | Åtgärd | Bevis |
|---|---|---|---|---|
| R7 | Ärvd skala dubblerar gränssnittet. `GDK_SCALE` från en äldre session fick JavaFX att rita allt dubbelt stort, utan att någon kod hade ändrats. | Upptäckt vid start i en session som satt kvar med `GDK_SCALE=2`. | `Main` sätter `glass.gtk.uiScale` till 1 innan JavaFX startar, och `start.sh` sätter `GDK_SCALE` i stället för att ärva det. `AUTOCORE_UI_SCALE` finns för en skärm som behöver annan skala (SCRUM-356). | `MainScaleTest`, commit `71571f8`, [README](../README.md) under "Skalan gränssnittet ritas i". |
| R8 | Det fristående programmet antog att dagen om trettio dagar är öppen. | Samma kod och samma prov gav grönt en dag och rött nästa, eftersom dagen kunde vara en lördag. | Programmet väljer en öppen dag ur `OpeningHours` i stället för att anta (SCRUM-338, SCRUM-339). | `StandaloneWorkOrdersTest`, [`StandaloneWorkOrders.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/standalone/StandaloneWorkOrders.java). |
| R9 | Ett bevis var beroende av klockan och veckodagen. | `StatusRoutesProof` kördes 20:13, efter stängning, och hoppade då över drop-in-vägen. Det såg ut som en regression men var indata. | `createDropInBooking` har fått en överlagring som tar dag och timme, och provet använder en fast onsdag (SCRUM-326). | [`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md). |
| R10 | Baslinjen låg utanför repot, i `/tmp`, och hade försvunnit när den skulle användas. | Jämförelsen gick inte att göra om från en färsk klon. | Båda sidornas utskrifter och argumenten ligger nu i [`bevis/`](bevis/README.md) (SCRUM-342). | `bevis/baslinje-java8/`, `bevis/utskrifter-4.0/`. |
| R11 | Ett loggprov hoppades över i stället för att pröva. | Provet letade efter en loggfil som råkade finnas, och svarade grönt när den inte fanns. | Provet skriver sin egen loggfil (SCRUM-341). | `LogContentTest`. |
| R12 | En ny startväg gick förbi spärren. | `ConsoleApp` flyttades in i bygget i SCRUM-336 och byggde `new GarageSystem(...)` direkt, alltså samma genväg som SCRUM-332 hade tagit bort. Kontrollen i 332 var ett grep för hand, så ingen märkte att dörren öppnades igen. | Konsolen frågar efter användare och lösenord, loggar in med `LoginService` och bygger `GuardedGarageSystem`. `NoUnguardedConstructionTest` läser produktionskoden och fäller en direkt konstruktion (SCRUM-357). | [`ConsoleApp.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/ConsoleApp.java), commit `6575338`. |
| R13 | Konsolen visade råa felmeddelanden och loggade inget. | Ett tekniskt nej skrevs som `NEKAD:` med tom mening, medan orsaken bara fanns i en loggrad utan text. | `DatabaseInitException` har en textnyckel, och raden skrivs där undantaget kastas. Konsolen använder `logback-standalone.xml` (SCRUM-358). | Commit `d74a169`, `AppException`, `logback-standalone.xml`, [`demo-loggning.md`](demo-loggning.md). |
| R14 | En gömd funktion gick att nå ändå. | Att dölja en knapp räcker inte om någon kan fyra dess händelse från kod. | `Actions` lägger ett filter före knappens handler som konsumerar händelsen och loggar operationen. Under det ligger spärren kvar (SCRUM-362). | [`Actions.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/ui/util/Actions.java), commit `12577e4`. |
| R15 | Rollen erbjöds funktioner den inte får använda. | En läsbehörig såg "+ New booking", "+ Create invoice" och raderaknappar som först svarade vid tryck. | Fönstret frågar `GarageSystem.may(Operation)` innan det ritar, och `Actions` tar bort kontrollens plats i layouten när den göms (SCRUM-361). | Commit `e4716eb`, `Actions`, `RoleMatrix`. |
| R16 | Ekonomin låg öppen för den som bara får läsa. | En läsbehörig kunde se fakturor och betalningar. | `READ_FINANCE` är en egen operation som bara administratören har. `PageRouter` frågar bara efter faktura- och betalningsantal när den är tillåten, och `EntityLookup` släpper inte ut en faktura till den som inte får se den (SCRUM-362). | [`RoleMatrix.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/security/RoleMatrix.java), commit `0ba8ab9`. |
| R17 | En avbruten order avbokade en bokning som fortfarande användes. | Avbokningen frigjorde tider även när en annan aktiv order låg på samma bokning. | Bokningen avbokas bara när ingen annan order på den är aktiv (SCRUM-343, R1-1). | Commit `5a16c05`, `WorkOrderRulesTest`. |
| R18 | En avbruten order höll kvar sina tjänster, och meny, notis och dialog räknade olika. | Tjänsterna såg upptagna ut och antalet bokningar skilde mellan vyerna. | En avbruten order släpper sina tjänster, och de tre ställena räknar samma bokningar (SCRUM-344, R1-2). | Commit `ebc6e64`. |
| R19 | Gränssnittet flyttade sig när man klickade och när sidor laddades. | Focus- och hoverregler ändrade `-fx-border-width`, som är en del av kontrollens insets, så kontrollen blev större och allt bredvid flyttade sig. `-fx-font-weight: bold` på menyvalet gjorde samma sak. | Reglerna ändrar nu bara färg, radie och effekt. Fokusringen är färg plus en yttre skugga. Animationerna pressar inte ihop layouten (SCRUM-355). | `LayoutStillnessTest`, commit `4353fd6`. |
| R20 | Bygget krävde mer än kravet. | `pom.xml` kräver JDK 25, medan WAC4_0 §2 säger Java 17 eller senare. | Golvet förklaras i `pom.xml` och i README: JavaFX 25 är byggt för Java 23, Spring Boot 4 för Java 17. Enforcer-plugin svarar med en läsbar rad i stället för class file-fel (SCRUM-340). | [`pom.xml`](../WigellAutoCore/autocore/pom.xml), [README](../README.md) under "Innan du bygger". |
| R21 | Kort stängdes utan bevis. | Genomgången av sprintens 32 kort med nyckel i commiten hittade sju som saknade bevis i sin egen commit. Fem hade fått ett senare; två fick nya prov. Ett kort, SCRUM-354, har fortfarande inget. | Regeln står i README: ett kort stängs bara med ett prov, ett dokument med ett körbart kommando eller en fil i `bevis/` (SCRUM-360). | [README](../README.md) under "Ett kort stängs med ett bevis som går att köra om", commit `e76f79a`. |
| R22 | Två `org.json.JSONObject` ligger på testklasspath. | `mvn test` varnar: både `org.json:json` och `android-json` (via testberoendena) finns. Bara testklasspath, inte jar-filen. | Ingen åtgärd än. | Varningen syns vid `mvn -o test` 23 oktober 2026. |
| R23 | Mockito laddar en agent dynamiskt på JDK 25. | `mvn test` varnar att självattach görs och att det inte kommer att tillåtas i en framtida JDK. Bara provkörningen. | Ingen åtgärd än. | Varningen syns vid `mvn -o test` 23 oktober 2026. |
| R24 | Siffrorna i dokumenten glider isär från koden. | Provantalet har rättats flera gånger under sprinten, och planens antal operationer rättades till 30 när `READ_FINANCE` tillkom. | Siffrorna kontrolleras mot koden före delning; kommandona står i [sprintsammanställningen](sprint-6-sammanstallning.md). | Commit `66d2f43`, commit `12577e4`. |
| R25 | Dokumenten pekade på commits som inte fanns efter en historieomskrivning. | Baslinjens README sa `ce1fde8`, som var samma commit som `b0ef2b9` före omskrivningen. | Dokumenten pekar nu på commits som finns, och bevisen ligger som filer i repot så att en hash inte behövs för att läsa dem. | Commit `158dd6e`, [`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md). |

## 3. Kvarstående risker

De här är öppna när sprinten stänger. Ingen av dem hindrar en demo, men var och en behöver ett
beslut eller en anteckning i redovisningen.

| ID | Risk | Varför den är kvar | Nästa steg |
|---|---|---|---|
| O1 | Inga JavaFX-körningsprov i CI. | CI kör `mvn package`, och de gränssnittsprov som finns prövar CSS, `pom.xml` och skalegenskapen. De startar inget fönster. Beteenden som utloggning, klick och layout prövas bara manuellt. | Skriv en kort manuell demogenomgång före redovisningen, eller inför TestFX i en egen CI-körning. Anteckna i redovisningen att gränssnittet är manuellt prövat. |
| O2 | SCRUM-354, utloggning utan att stänga appen, saknar bevis. | Genomgången i SCRUM-360 säger att kortet öppnades igen, eftersom det inte finns något prov för fönsterbeteendet. Tavlan visar ändå Done. | Antingen skriv provet eller för kortet tillbaka och redovisa det som manuellt prövat. |
| O3 | Två `JSONObject` på testklasspath (R22). | Ingen åtgärd gjord. | Uteslut `android-json` i `pom.xml` om den inte behövs, och kör om sviten. |
| O4 | Mockito självattach (R23). | Ingen åtgärd gjord. | Följ Mockitos rekommendation och registrera agenten när JDK:t kräver det. Inte akut på JDK 25. |
| O5 | Siffrorna i dokumenten kan glida (R24). | De kontrolleras för hand. | Kör kontrollkommandona i [sprintsammanställningen](sprint-6-sammanstallning.md) innan dokumenten delas, och rätta alla ställen samtidigt. |
| O6 | Många grenar och kort är öppna vid stängning. | Tavlan har kort i In Review och To Do, och det finns 57 fjärrgrenar med `SPRINT-6` i namnet. | Merga i en ordning som följer beroendena, och radera grenar som är inmergade efter 15.00. |
| O7 | Migreringen gör ingen automatisk säkerhetskopia. | Den fil som körs rullas tillbaka om den faller, men databasfilen kopieras inte före första ändringen. | Kör mot en kopia av den riktiga databasen, som E5-3 gör. Skriv i redovisningen att det är så, och lägg en kopia bredvid om någon kör mot skarp data. |
| O8 | CI prövar bara Linux. | `ci.yml` kör på `ubuntu-latest` med Temurin 25. Maven hämtar rätt JavaFX för Windows och macOS, men de vägarna är inte prövade i CI. | Anteckna i redovisningen. Kör `./start.sh` på en Windows- och en macOS-maskin om någon i gruppen har tillgång. |
| O9 | `AUTOCORE_*_PASSWORD` ligger i miljön när den används. | Värdet syns för den som kan läsa processens miljö eller skalhistoriken. Koden loggar det aldrig, och hashen i tabellen går inte att räkna baklänges. | Behandla variablerna som hemligheter, sätt dem i sessionen och inte i ett skript som checkas in. `SecretScanTest` fångar det som hamnar i git. |

## Vad som inte längre är en risk

Fem saker som såg ut som risker under arbetet och som är avgjorda:

- Att den gamla databasen inte skulle gå att lyfta. `OldDatabaseProof` visar `customers=6 vehicles=6 bookings=6 work_orders=6 mechanics=3` före och efter, med `schema_version` 4 och tre konton som enda tillägg.
- Att plattformsbytet skulle tappa funktionalitet. Elva bevis kördes på både Java 8 och 4.0. Alla svarar lika, eller skiljer sig bara där indata gör det.
- Att någon skulle behöva en extern identitetsleverantör. Kravet ställer inget sådant krav i den här etappen, och beslutet med skäl står i [ADR-001](arkitekturbeslut.md#adr-001-egen-inloggning-mot-users-ingen-extern-identitetsleverantör).
- Att JavaFX-skalan skulle vara ett maskinproblem som inte går att lösa i koden. Den går att lösa i `Main`, och `MainScaleTest` prövar egenskapen.
- Att dokumentationen bara fanns utanför repot. ADR-001 låg i gruppens gemensamma mapp och baslinjen i `/tmp`. Besluten ligger nu i [arkitekturbeslut.md](arkitekturbeslut.md), och bevisen i [`bevis/`](bevis/README.md).

## Hur registret hålls levande

En ny risk får nästa lediga R-nummer och en rad i tabellen för den del av systemet den hör till.
När risken stängs flyttas beviset in i raden, inte till ett separat dokument. Öppna risker flyttas
till avsnitt 3 och får ett O-nummer.

Planen uppdateras när en risk slår till, enligt E5-4. Den här filen uppdateras när sprinten
stänger, och varje rad ska gå att kontrollera från en färsk klon.
