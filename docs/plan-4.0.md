# Plan för migreringen till AutoCore 4.0

WAC4_0 §10 ber om en kortfattad plan före större förändringar: nuvarande plattform, risker, vad som behöver ändras, hur information bevaras, hur autentisering och behörighet införs, hur loggning och felhantering förbättras, hur det fristående kravet uppfylls, och hur vi kontrollerar att tidigare funktionalitet håller.

Dokumentet skrevs under sprinten och uppdaterades när risker slog till, vilket kravet också säger att det ska. De arkitektoniska besluten bakom punkterna ligger i [arkitekturbeslut.md](arkitekturbeslut.md).

## 1. Nuvarande teknikplattform och viktigaste beroenden

Utgångsläget var Java 8, BellSoft Liberica 1.8.0_504, med JavaFX 8. Två jar-filer låg i `WigellAutoCore/autocore/lib/`: `sqlite-jdbc-3.53.4.0` och `json-20231013`. Det fanns ingen byggbeskrivning. `start.sh` kompilerade källan för hand med `javac` mot de två filerna och startade programmet, och bevisen var vanliga `main()`-program som kördes en och en.

Det finns alltså fyra saker att byta ut samtidigt: språkversionen, gränssnittsbiblioteket, sättet att hämta beroenden och sättet att bygga.

Målet enligt WAC4_0 §2 är Spring Boot 4.x på Java 17 eller senare. Vi valde Spring Boot 4.1.1 och Java 25, JavaFX 25.0.4, och Maven som hämtar allt, inklusive de två biblioteken som låg i `lib/`. Loggning kommer från `spring-boot-starter` och provkörningen från `spring-boot-starter-test`.

Golvet i praktiken är JavaFX, inte Spring. `spring-boot-4.0.6.jar` och `spring-core-7.0.7.jar` är byggda för Java 17 (class file 61), medan `javafx-base-25.0.4-linux.jar` är byggd för Java 23 (class file 67). Under 23 går bygget alltså inte, och `pom.xml` svarar först med ett läsbart meddelande i stället för ett fel om class file-versioner. Vi stannar på 25, som är den version projektet byggs och prövas på lokalt och i CI. Kravet "Java 17 eller senare" är därmed uppfyllt med marginal; att bygget kräver mer än kravet står i `README` under "Innan du bygger" och i `pom.xml`.

## 2. Risker vid migreringen

Det här är riskerna som de skrevs ner innan arbetet började, och vad som hände med dem. Hela
registret, inklusive det som hittades senare under sprinten och det som fortfarande är öppet,
ligger i [riskanalys.md](riskanalys.md).

De risker vi räknade upp innan arbetet började:

- Två byggvägar. `start.sh` kompilerade för hand vid sidan av Maven. Så länge båda finns kan det ena fungera medan det andra är trasigt.
- JavaFX 8 mot 25. Gränssnittet, temaarken och resurserna flyttar från filsystemet till classpath, och en del API:er har ändrats.
- Statisk `Db`. Lagringen låg i en statisk klass som både gränssnittet och proven muterade, vilket krockar med en kontext som äger sina bönor.
- Bevisen låg i produktionsträdet. `src/main/java/com/wac/autocore/test/` var program som ingen applikation anropar men som ändå följde med i bygget, och tio av dem byggde tjänsten förbi behörighetsspärren. De ligger nu i `src/test/java/com/wac/autocore/proof/`, utanför jar-filen.
- Hemligheter. Demokonton och konfiguration riskerar att hamna i källkoden eller i loggen.
- Behörighet som bara göms. Om kontrollen sitter i gränssnittet går den att gå runt.

### Risker som slog till

Två av de sex hände, och tre till som inte stod på listan.

`start.sh` blev farlig på det sätt vi befarat. Efter att Spring och JavaFX flyttat in i `pom.xml` föll den med `package org.springframework.boot does not exist`, eftersom den fortfarande kompilerade mot `lib/`. Den byggdes om till att köra Maven.

Statiska `Db` kostade mer än väntat. Varje repository läste en sökväg som sattes någon annanstans, och proven kunde bara köras för att klassen var statisk. Den blev en böna med sökvägen i `autocore.db.path`, och varje repository får sin `Db` via `RepositoryFactory`.

`GDK_SCALE=2` hade vi inte skrivit upp. Variabeln låg kvar i en befintlig session och fördubblade hela gränssnittet, eftersom JavaFX ritar genom GTK. Ingen kod hade ändrats. `Main` sätter nu `glass.gtk.uiScale` till 1 innan JavaFX startar, och `start.sh` sätter `GDK_SCALE` i stället för att ärva det.

Behörighetsspärren täckte skrivningarna men inte läsningarna, så en anropare utan identitet kunde läsa hela registret. Det upptäcktes vid genomläsning efteråt och rättades i E3-7 (SCRUM-320).

Det fristående programmet bokade alltid dagens datum plus trettio dagar, vilket är en lördag ibland. Det visade sig morgonen därpå, när samma kod och samma prov gav rött i stället för grönt.

## 3. Vilka delar av systemet som behöver förändras

- Bygget: `pom.xml` tillkommer och `start.sh` skrivs om.
- Datalagret: `Db` blir en böna, och `RepositoryFactory` fördelar den.
- Kärnan: `IdentityHolder` flyttar in, så att appen och det fristående programmet delar samma identitet.
- Gränssnittet: inloggningen blir ett kort i huvudfönstret i stället för ett eget fönster.
- Säkerheten: `Operation`, `RoleMatrix` och en dekoratör framför fasaden.
- Loggningen: `logback-spring.xml` med profil per miljö, och MDC med trace-id och operation.
- Bevisen: ut ur produktionsträdet, vilket var SCRUM-332 och är gjort.

## 4. Hur befintlig information ska bevaras

Migreringarna körs i ordning: `001_baslinjen.sql`, `002_nycklar.sql`, `003_ore.sql`, `004_users.sql`. 001 skapar grunden, 002 och 003 bygger om tabellerna för att få riktiga nycklar respektive öret som enhet, och 004 lägger till konton. Den som bygger om kopierar raderna in i den nya tabellen och släpper den gamla först efteråt.

`WigellAutoCore/autocore/test/data/autocore-original.db` är en riktig äldre databas som ligger i git. `OldDatabaseProof` lyfter den genom 4.0 och visar att innehållet står kvar: `customers=6 vehicles=6 bookings=6 work_orders=6 mechanics=3` före och efter, med `schema_version` 4 och tre konton som enda tillägg.

Att lösa migreringen genom att börja om med en tom databas är alltså uteslutet, både som princip och för att beviset då inte hade gått att köra.

## 5. Hur autentisering och behörighetsstyrning ska införas

Konton ligger i en egen tabell med roll och PBKDF2-hash. Hashen är 210 000 varv i migreringen och smälts om till 600 000 vid nästa lyckade inloggning. Lösenorden i klartext finns bara hos gruppen, och de kan sättas utifrån med miljövariabler som hashas vid start.

Inloggningen fyller en `IdentityHolder`. Rollmatrisen är data: 30 operationer och tre roller. Administratören har allt. Verkstadspersonalen arbetar med kunder, fordon, bokningar, arbetsorder, tjänster och mekaniker, och har varken borttagningarna, inställningarna eller ekonomin. En läsbehörig läser dagens arbete och väljer språk, och ser inte heller ekonomin. Fakturor och betalningar är administratörens, både att läsa, skapa och ta emot.

Kontrollen sitter i en dekoratör framför fasaden, inte i gränssnittet. Den täcker både skrivningar och läsningar, och den svarar olika för "ingen är inloggad" och "rollen får inte", eftersom kravet skiljer på dem. Samma dekoratör används i det fristående programmet.

## 6. Hur loggning och felhantering ska förbättras

Logback konfigureras i `logback-spring.xml` med två profiler. Utan profil, eller i dev, loggas vår egen kod på DEBUG till både konsol och fil. I prod loggas vår kod på INFO och allt annat på WARN, och bara till fil. Filen rullar vid 10 MB och sparas i fjorton dagar.

Varje rad bär tidpunkt, nivå, tråd och komponent. I MDC ligger ett trace-id och namnet på operationen, så en nekad åtkomst och raden som förklarar den hör ihop.

Verksamhetshändelser loggas när en arbetsorder skapas och slutförs. Säkerhetshändelser loggas när någon nekas, med användarnamn och operation men aldrig med det användaren skrev. Tekniska fel loggas med undantaget, medan användaren får en mening utan klassnamn och utan intern text. Loggningen hålls av två prov: `LogContentTest` läser loggfilen och letar trace-id, hemligheter och personuppgifter, och `EventLoggingTest` fångar raderna i minnet och prövar händelserna och användartexten. `SecretScanTest` hör till samma sak men läser källkoden i stället, och faller om en hemlighet skrivs in där.

## 7. Hur kravet på fristående verksamhetsfunktionalitet uppfylls

`StandaloneWorkOrders` är ett vanligt Java-program utan Spring, utan gränssnitt och utan webbserver. Det bokar och driver en arbetsorder hela vägen till `COMPLETED`, alltså två operationer där en är arbetsorderns livscykel.

Det använder samma `GarageSystem` och samma dekoratör som applikationen, så affärsreglerna är desamma. Identiteten sätts i hållaren på samma sätt som inloggningen gör det, och programmet kan köras utan identitet för att visa att spärren gäller även där.

Dagen väljs ur `OpeningHours` innan tjänsten tillfrågas, så programmet hoppar över helger i stället för att bli nekad, och det skriver ut vilken dag och timme bokningen fick. Valet ligger i en egen metod som tar startdagen som argument, så det går att pröva med ett fast datum i stället för dagens.

## 8. Hur vi kontrollerar att tidigare funktionalitet fortfarande fungerar

Enhetstesterna är 102 JUnit-prov: inloggning, lösenordshashning och rotation, identitetshållaren, rollmatrisen, spärren för läsning och skrivning, raderingsregeln för en avbruten order, det fristående programmet, bokning och drop-in, loggning och spårbarhet, hemlighetsskanning, och att Spring-kontexten startar med rätt databassökväg. Ingen av dem hoppas över: loggprovningen skriver sin egen loggfil i stället för att läsa en som råkar finnas.

Bevisprogrammen är femton program som skriver ut sitt svar, och tolv av dem avslutar med en exitkod så att en körning kan fällas maskinellt. Tillsammans täcker de verksamhetsreglerna: bokningar och arbetsordrar, utkast och validering, fakturor och betalningar, tal- och avrundningsregler, felhantering på båda språken, den gamla databasen, rollmatrisen, behörigheten, raderingen av en avbruten order och självkontrollen. De ligger i `src/test/java/com/wac/autocore/proof/`, alltså utanför jar-filen, och körs efter `mvn test-compile` med `target/test-classes` först i classpath:

```
java -cp "WigellAutoCore/autocore/target/test-classes:WigellAutoCore/autocore/target/classes:$(cat WigellAutoCore/autocore/target/cp.txt)" \
     com.wac.autocore.proof.StatusRoutesProof
```

Surefire hoppar över samma paket i `pom.xml`, eftersom `SelfTest` annars hade setts som ett JUnit-prov på namnet.

Jämförelsen mot baslinjen: elva bevis spelades in på Java 8 i commit [`b0ef2b9`](https://github.com/alexwest1981/AutoCore/commit/b0ef2b9), den sista som ändrade koden före Maven-bygget. Samma elva kördes på 4.0. Alla elva svarar lika, eller skiljer sig bara där indata gör det. `StatusRoutesProof` var den som avvek först, eftersom körningen gjordes efter stängning, och den är rättad i SCRUM-326.

Båda sidornas utskrifter ligger i repot, i [bevis/baslinje-java8/](bevis/baslinje-java8/) och [bevis/utskrifter-4.0/](bevis/utskrifter-4.0/), tillsammans med tabellen och förklaringen i [bevis/regressionsjamforelse.md](bevis/regressionsjamforelse.md). Den som bara har repot kan alltså läsa varje rad som skiljer sig och se vilket prov den kom från. Argumenten per prov står i [bevis/baslinje-java8/README.txt](bevis/baslinje-java8/README.txt).

Sviten körs vid varje ändring, och det var den som fångade att det fristående programmet bokade en dag som kunde vara stängd. Två saker hittades utanför den: den brutna `start.sh`, som föll först när någon startade den, och baslinjen som låg i `/tmp` och hade försvunnit när den skulle användas.
