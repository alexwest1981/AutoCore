# AutoCore

Ett verkstadssystem för Wigell Group. Kunder, fordon, bokningar, arbetsordrar, fakturor och betalningar, med ett JavaFX-gränssnitt och SQLite lokalt.

Planen för migreringen till 4.0 ligger i [docs/plan-4.0.md](docs/plan-4.0.md).

Sprintens sammanställning ligger i [docs/sprint-6-sammanstallning.md](docs/sprint-6-sammanstallning.md),
riskerna och hur de följdes upp i [docs/riskanalys.md](docs/riskanalys.md), och de arkitektoniska
besluten i [docs/arkitekturbeslut.md](docs/arkitekturbeslut.md). Alla delade dokument ligger i
[docs/README.md](docs/README.md).

Loggerna som demonstrationen kräver, med kommandona som ger dem, ligger i [docs/demo-loggning.md](docs/demo-loggning.md).

## Vad systemet gör

- Kunder och fordon, med ägarkoppling.
- Bokningar med en eller flera tjänster, tidsberäkning och skydd mot överlapp.
- Servicepaket: färdiga buntar av tjänster att utgå från i en bokning.
- Mekaniker med specialisering, schema och belastning.
- Tjänstekatalog med pris och beräknad tid.
- Arbetsordrar i tre typer: standard, reklamation och internt arbete.
- Utkast som fylls på efter hand, och drop-in utan bokning.
- Statusflöde från skapad till klar, med spärrar mot orimliga byten.
- Inloggning med tre roller — administratör, personal och läsbehörig — och en spärr i kärnan som gäller både läsning och skrivning.
- Loggning med trace-id per operation, så en nekad åtkomst och raden som förklarar den hör ihop.
- Fakturor med flera rader, rabatt och moms.
- Betalning via kort, Swish eller kontant.

![AutoCore](WigellAutoCore/docs/autocore-overview.png)

Siffrorna i menyn visar vad som väntar på hantering.

## Innan du bygger

JDK 25 och Maven 3.9 eller senare. Bygget kräver exakt JDK 25: `pom.xml` stannar med ett meddelande om du kör en annan version.

Skälet är JavaFX, inte Spring. `spring-boot-4.0.6.jar` och `spring-core-7.0.7.jar` är byggda för Java 17 (class file 61), men `javafx-base-25.0.4-linux.jar` är byggd för Java 23 (class file 67). Under 23 går bygget inte alls, och då svarar `pom.xml` med en läsbar rad i stället för ett fel om class file-versioner. Vi stannar på 25 eftersom det är den version projektet byggs och prövas på, både lokalt och i CI. WAC4_0 §2 säger "Java 17 eller senare", så kravet är uppfyllt; bygget begär bara mer än kravet gör.

```
mise use java@25
mvn -f WigellAutoCore/autocore/pom.xml package
```

JavaFX 25, sqlite-jdbc och json hämtas av Maven, så inget installeras för hand.

Öppna projektet i IntelliJ genom att högerklicka på `WigellAutoCore/autocore/pom.xml` och välja **Add as Maven Project**. Modulfilerna delas inte via git, eftersom de pekar på den plattform din maskin råkar ha. Maven väljer rätt JavaFX för Linux, Windows och macOS, så samma pull fungerar för alla.

## Bygg och kör

`./start.sh` kontrollerar att du har JDK 25 och Maven 3.9, sätter `GDK_BACKEND=x11` på Linux och kör sedan `mvn spring-boot:run` — samma bygge som IntelliJ gör. Starta appen med `./start.sh`, eller kör `Main.java` från IntelliJ.

Databasen skapas vid första starten i `data/autocore.db` i projektroten, med demodata om filen inte finns sedan tidigare.

### Får du "No suitable driver found for jdbc:sqlite"?

Drivrutinen ligger i `.idea/libraries/` och följer med i giten. Felet betyder att IntelliJ kör en gammal projektmodell: välj **File → Reload All from Disk**, eller stäng och öppna projektet. Går det inte, kör `mvn -f WigellAutoCore/autocore/pom.xml dependency:resolve` så att Maven hämtar filerna igen. `./start.sh` behöver inget av det.

Körningen behöver en bildskärm. På Wayland sätter `start.sh` `GDK_BACKEND=x11` själv, eftersom JavaFX annars kraschar i GTK.

### Skalan gränssnittet ritas i

JavaFX frågar GTK hur stort allt ska vara, och svaret kommer från `GDK_SCALE`. En session som startades innan skärmens skala sänktes behåller det gamla värdet, och då ritas varje knapp, varje typsnitt och hela fönstret dubbelt så stort. `Main` sätter därför `glass.gtk.uiScale` till 1 innan JavaFX startar, så ett ärvt värde inte når fönstret. Det gäller även när appen startas från IntelliJ, som aldrig går genom `start.sh`.

Behöver skärmen en annan skala går det att be om den:

```
AUTOCORE_UI_SCALE=2 ./start.sh
```

### Fristående program

Det fristående programmet startas utanför Maven, och då behöver JDK 25 veta att SQLite får använda inbyggd kod. Utan flaggan kommer `WARNING: Restricted methods will be blocked in a future release`:

```
java --enable-native-access=ALL-UNNAMED -cp "WigellAutoCore/autocore/target/classes:$(cat WigellAutoCore/autocore/target/cp.txt)" com.wac.autocore.standalone.StandaloneWorkOrders /tmp/fristaende.db
```

Jar-filen behöver ingen flagga. Raden `Enable-Native-Access: ALL-UNNAMED` står i manifestet genom `maven-jar-plugin` i `pom.xml`, så `java -jar WigellAutoCore/autocore/target/autocore-4.0.0.jar` startar utan varning.

### Konsolversionen

`ConsoleApp` är en meny i terminalen över samma tjänst. Den frågar efter användare och lösenord, loggar in med samma konton som fönstret och går genom samma behörighetsspärr, så den är inte en väg runt inloggningen. Tre försök, sedan avslutas den med felkod.

```
java -cp "WigellAutoCore/autocore/target/classes:$(cat WigellAutoCore/autocore/target/cp.txt)" com.wac.autocore.ConsoleApp data/autocore.db
```

### Var databasen ligger

Sökvägen är en inställning och inte kod. Den står som `autocore.db.path` i `src/main/resources/application.properties` och kan skrivas över vid körning:

```
mvn -f WigellAutoCore/autocore/pom.xml spring-boot:run \
    -Dspring-boot.run.arguments=--autocore.db.path=/tmp/kopia.db
```

Spring bygger `Db` med den sökvägen, och varje repository får sin `Db` via `RepositoryFactory` i stället för att läsa en statisk sökväg. Ett prov som körs utan Spring läser samma property med `-Dautocore.db.path=...`, eller tar filen som första argument.

### Hemligheter

Inga hemligheter ligger i git. Demokontona skapas av migrering 004, och kolumnen `password_hash` innehåller en PBKDF2-hash. En hash är inte en hemlighet: den går inte att räkna baklänges, och saltet är publik. Lösenorden i klartext finns bara hos gruppen.

Vill man sätta dem utifrån i stället, till exempel på en annan maskin, läses de från miljön:

```
AUTOCORE_ADMIN_PASSWORD=... \
AUTOCORE_STAFF_PASSWORD=... \
AUTOCORE_READONLY_PASSWORD=... \
./start.sh
```

Värdet hashas vid start med nytt salt och skrivs över bara om det skiljer sig från det som redan står i tabellen. Ett för kort lösenord stoppar starten med kontots namn, aldrig med värdet. `SecretScanTest` läser alla filer git spårar och faller om ett lösenord, en nyckel eller en token skrivs in.

## Inloggning, roller och behörighet

Appen börjar med en inloggning. Fönstret visar ett kort i huvudfönstret, konsolen frågar i
terminalen, och det fristående programmet sätter en identitet själv. Alla tre går genom samma
`GuardedGarageSystem`.

Kontona ligger i tabellen `users`, som migrering `004_users.sql` skapar. Det finns tre:

| Konto | Roll | Får | Får inte |
|---|---|---|---|
| `admin` | `ADMIN` | allt, alla 30 operationer | — |
| `staff` | `STAFF` | kunder, fordon, bokningar, arbetsorder, tjänster och mekaniker, och att välja språk | radera kund, fordon, mekaniker eller arbetsorder; ändra inställningar; hela ekonomin |
| `readonly` | `READ_ONLY` | `READ` och `CHANGE_LANGUAGE` | allt som ändrar något, och ekonomin |

Lösenordet ligger som en PBKDF2-hash med salt, aldrig i klartext. Migreringen skrev 210 000 varv,
och en rad som är svagare än 600 000 smälts om vid nästa lyckade inloggning. `LoginService` svarar
lika för "ingen sådan användare" och "fel lösenord", så fönstret inte berättar vilka namn som
finns.

Vem som är inloggad hålls av `IdentityHolder` i kärnan. Den fylls vid inloggning, töms vid
utloggning, och är vanlig Java utan Spring, så konsolen och det fristående programmet har egna.

Vad rollen får göra är data, inte villkor utspridda i koden. `RoleMatrix` har en rad per roll, och
`Operation` namnger varje sorts arbete. En roll eller operation som saknas ger nej, aldrig ja.
Administratören definieras som alla operationer, så en ny operation är dess till dess att någon
uttryckligen tar bort den från personalen.

Kontrollen sitter i spärren, inte i fönstret. `GuardedGarageSystem` ligger framför `GarageSystem`
och kräver en operation för varje anrop, både när något läses och när något skrivs. Den skiljer på
två nej, eftersom kravet gör det: ingen inloggad (`NotAuthenticatedException`) och rollen får inte
(`AccessDeniedException`).

Fönstret frågar samma objekt innan det ritar, genom `GarageSystem.may(Operation)`. `Actions` gömmer
en kontroll rollen inte får använda och tar bort dess plats i layouten, och kontrollen får ett
filter som konsumerar händelsen om någon ändå når den. Att dölja är artigheten; spärren är regeln,
och den ligger kvar under båda lagren. `NoUnguardedConstructionTest` fäller en klass som skapar
`new GarageSystem(` i stället för att gå genom spärren.

Skälen bakom de valen står i [docs/arkitekturbeslut.md](docs/arkitekturbeslut.md), ADR-006 till
ADR-009 och ADR-017.

## Loggning och spårbarhet

Loggningen går genom SLF4J och Logback. `logback-spring.xml` har två profiler: utan profil, eller
i `dev`, loggas vår egen kod på DEBUG till både konsol och fil; i `prod` loggas vår kod på INFO,
allt annat på WARN, och bara till fil.

Varje rad bär tidpunkt, nivå, tråd, `trace`, `op` och logger:

```
2026-10-22T14:45:18.391+02:00 INFO  [main] [trace=87daa17e op=CREATE_WORK_ORDER] c.w.a.service.WorkOrderService - Arbetsorder 7 skapad för bokning 7
```

`trace` är ett trace-id som sätts när en operation börjar, och `op` är operationens namn ur
`Operation`. Båda ligger i MDC, så varje rad som skrivs under anropet får dem utan att loggningen
behöver skicka med dem. En läsning som görs inuti en ändring ansluter till det pågående trace-id:t
i stället för att starta ett nytt.

Det som loggas är verksamhetshändelser, som att en arbetsorder skapas och slutförs, och
säkerhetshändelser, som en nekad åtkomst med användare, roll och operation — men aldrig med det
användaren skrev. Ett tekniskt fel loggas med undantaget och sin stapl, medan användaren får en
mening utan klassnamn och utan intern text.

Filen hamnar i `logs/autocore.log`. Katalogen kan flyttas med `AUTOCORE_LOG_DIR`. Filen rullar vid
10 MB, sparas i fjorton dagar och har ett tak på 200 MB. Det fristående programmet och konsolen har
ingen Spring-kontext och läser därför `logback-standalone.xml`, med samma mönster och samma fil.

Tre prov håller loggningen på plats: `LogContentTest` läser den fil provet själv skriver och letar
trace-id, hemligheter och personuppgifter, `EventLoggingTest` fångar raderna i minnet och prövar
händelserna och användartexten, och `SecretScanTest` läser källkoden. De tre loggraderna som demon
kräver, med kommandona som ger dem, står i [docs/demo-loggning.md](docs/demo-loggning.md).

## Arkitektur

```
WigellAutoCore/autocore/
├── pom.xml                    bygget: JDK 25, JavaFX 25, Spring Boot, SQLite och json
└── src/
    ├── main/java/com/wac/autocore/
    │   ├── AutoCoreApplication.java  Springs ingång
    │   ├── Main.java                 sätter skalan och startar fönstret
    │   ├── ConsoleApp.java           Konsolversionen
    │   ├── config/         Db som böna, sökvägen som property,
    │   │                   identiteten och inloggningen som bönor
    │   ├── data/           Databasen, migreringarna och demodata
    │   ├── exception/      Undantagen, med nyckel för texten
    │   ├── model/          Domänmodeller
    │   ├── repository/     Läsning och skrivning mot SQLite
    │   ├── security/       Identiteten, rollerna, operationerna och lösenordshashen
    │   ├── seed/           Demodatans texter, svenska och engelska
    │   ├── service/        Affärslogik, GarageSystem som fasad,
    │   │                   GuardedGarageSystem som spärr,
    │   │                   Printer för utskrifter,
    │   │                   gateway/ för arbetsorderns ingång
    │   ├── standalone/     Det fristående programmet
    │   ├── theme/          Teman och stilmallar
    │   ├── util/           Resursläsning och texterna
    │   └── ui/             Gränssnittet: dialoger, vyer, komponenter,
    │                       i18n, motion, navigation och teman
    ├── main/resources/
    │   ├── com/wac/autocore/db/migrations/  de fyra SQL-filerna
    │   ├── com/wac/autocore/i18n/           sv.json och en.json
    │   ├── com/wac/autocore/theme/themes/   stilmallarna
    │   ├── com/wac/autocore/images/         bilderna
    │   ├── application.properties           databassökvägen och lösenorden
    │   ├── logback-spring.xml               loggningen i appen
    │   └── logback-standalone.xml           loggningen utanför Spring
    └── test/java/com/wac/autocore/
        ├── ...             JUnit-proven
        └── proof/          de femton bevisprogrammen, utanför jar-filen
```

Resurserna, alltså SQL, JSON, CSS och bilderna, ligger under `src/main/resources`, aldrig inne i
paketmappen. Det var E1-2 som flyttade dem dit, så de följer med i jar-filen.

Lagren går i en riktning: gränssnittet pratar med `GarageSystem`, som pratar med serviceklasserna, som pratar med repositoryt. Ingen genväg förbi. Arbetsordern går genom `WorkOrderGateway`, som är ett smalare gränssnitt mot samma tjänster.

### Bokning och arbetsorder

En bokning registreras som `BOOKED` och skapar ingen arbetsorder. Arbetsordern skapas först när fordonet lämnas in, från Kanban-vyn eller arbetsorderdialogen.

Bokningsdialogen visar lediga och upptagna timmar per mekaniker. Tjänsternas tider summeras, så en bokning som sträcker sig över flera timmar kan inte läggas ovanpå en annan. Avbokar du en bokning frigörs tiderna direkt.

![Schemalägg bokning](WigellAutoCore/docs/autocore-booking-dialog.png)

Kalendern stryker de dagar och tider som inte går att boka.

Ett servicepaket är en färdig bunt av tjänster. Väljer man ett paket i bokningen hamnar paketets tjänster i bokningen som dess egna, och paketet kan ändras efteråt utan att bokningen rörs.

En arbetsorder har en typ: standard, reklamation eller internt arbete. Reklamationen utgår från en slutförd order och pekar tillbaka på den, och kunden betalar normalt ingenting. Är ordern inte färdig än kan den ligga som utkast med bara fordon och beskrivning, och fyllas på med tjänster, mekaniker och planerat datum efter hand. Drop-in skapas direkt, utan bokning.

![Drop-in](WigellAutoCore/docs/autocore-dropin.png)

Mekanikerns behörighet hänger på en nyckel. Tjänsten anger vilken specialisering den kräver, och en tjänst utan krav kan utföras av alla.

### Schemat

Översikten har en Kanban-tavla per mekaniker med dag, vecka och månad. Dagen visar tio timmar mellan 07:00 och 17:00, lediga timmar går att boka direkt, och ett klick på en bokad timme fäller ut detaljer om fordon, kund och arbetsorder. Veckovyn färgkodar belastningen i fyra steg.

### Fakturering

Fakturan bygger på de tjänster som markerats som utförda, och tar priset som gällde när arbetet gjordes. Rabatt och moms räknas per rad, och den går att granska innan den skickas.

![Faktura](WigellAutoCore/docs/autocore-invoice.png)

### Språk

Svenska och engelska byts med knappen i menyn, utan omstart. Datum, statusord och valutor följer språket.

## Prov och bevis

`mvn test` kör 110 JUnit-prov, och ingen av dem hoppas över: loggprovningen skriver sin egen loggfil i stället för att läsa en som råkar finnas.

```
mvn -f WigellAutoCore/autocore/pom.xml test
```

CI gör samma sak, med `package` i stället för `test`, på varje push och varje pull request mot
`develop` och `main`. Ett flöde per gren, så en sprintgren med öppen PR inte startar två körningar
som slåss om samma runners.

Utöver JUnit-proven ligger femton bevisprogram i `src/test/java/com/wac/autocore/proof/`, utanför
jar-filen, där var och ett skriver ut sitt svar. De är vanliga `main`-program, inte JUnit-prov, och
Surefire hoppar över paketet. Kör dem efter `test-compile`:

```
cd WigellAutoCore/autocore
mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt
mvn -o test-compile
java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.proof.SelfTest
```

Byt `SelfTest` mot något av de andra namnen i mappen. Argumenten per prov står i
[docs/bevis/baslinje-java8/README.txt](docs/bevis/baslinje-java8/README.txt), och de är samma på
båda sidorna av jämförelsen.

Jämförelsen mot Java 8-baslinjen, elva prov inspelade på `b0ef2b9` och samma elva körda på 4.0, ligger i [docs/bevis/](docs/bevis/README.md) tillsammans med utskrifterna från båda sidorna. Den som bara har repot kan läsa varje rad som skiljer sig.

### Ett kort stängs med ett bevis som går att köra om

Ett bevis är en av tre saker:

1. Ett prov som fäller när beteendet försvinner.
2. Ett dokument i `docs/` med ett kommando som går att köra om.
3. En fil i `docs/bevis/` med utskriften och hur den togs.

En muntlig genomgång räknas inte, och en commit som nämner nyckeln räknas inte. Automationen stänger kortet när en PR nämner det, så nyckeln i en commit säger ingenting om att arbetet finns. Skriv i kortet vilken av de tre det är och var den ligger, och flytta först därefter kortet till Done. Saknas beviset stannar kortet där det är.


## Team

Grupp C: Alex, Lucas, Daniel.
