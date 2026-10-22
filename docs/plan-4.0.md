# Plan för migreringen till AutoCore 4.0

WAC4_0 §10 ber om en kortfattad plan före större förändringar: nuvarande plattform, risker, vad som behöver ändras, hur information bevaras, hur autentisering och behörighet införs, hur loggning och felhantering förbättras, hur det fristående kravet uppfylls, och hur vi kontrollerar att tidigare funktionalitet håller.

Dokumentet skrevs under sprinten och uppdaterades när risker slog till, vilket kravet också säger att det ska.

## 1. Nuvarande teknikplattform och viktigaste beroenden

Utgångsläget var Java 8, BellSoft Liberica 1.8.0_504, med JavaFX 8. Två jar-filer låg i `WigellAutoCore/autocore/lib/`: `sqlite-jdbc-3.53.4.0` och `json-20231013`. Det fanns ingen byggbeskrivning. `start.sh` kompilerade källan för hand med `javac` mot de två filerna och startade programmet, och bevisen var vanliga `main()`-program som kördes en och en.

Det finns alltså fyra saker att byta ut samtidigt: språkversionen, gränssnittsbiblioteket, sättet att hämta beroenden och sättet att bygga.

Målet enligt WAC4_0 §2 är Spring Boot 4.x på Java 17 eller senare. Vi valde Spring Boot 4.1.1 och Java 25, JavaFX 25.0.4, och Maven som hämtar allt, inklusive de två biblioteken som låg i `lib/`. Loggning kommer från `spring-boot-starter` och provkörningen från `spring-boot-starter-test`.

## 2. Risker vid migreringen

De risker vi räknade upp innan arbetet började:

- Två byggvägar. `start.sh` kompilerade för hand vid sidan av Maven. Så länge båda finns kan det ena fungera medan det andra är trasigt.
- JavaFX 8 mot 25. Gränssnittet, temaarken och resurserna flyttar från filsystemet till classpath, och en del API:er har ändrats.
- Statisk `Db`. Lagringen låg i en statisk klass som både gränssnittet och proven muterade, vilket krockar med en kontext som äger sina bönor.
- Bevisen ligger i produktionsträdet. `src/main/java/com/wac/autocore/test/` är program som ingen applikation anropar men som ändå följer med i bygget.
- Hemligheter. Demokonton och konfiguration riskerar att hamna i källkoden eller i loggen.
- Behörighet som bara göms. Om kontrollen sitter i gränssnittet går den att gå runt.

### Risker som slog till

Två av de sex hände, och tre till som inte stod på listan.

`start.sh` blev farlig på det sätt vi befarat. Efter att Spring och JavaFX flyttat in i `pom.xml` föll den med `package org.springframework.boot does not exist`, eftersom den fortfarande kompilerade mot `lib/`. Den byggdes om till att köra Maven.

Statiska `Db` kostade mer än väntat. Varje repository läste en sökväg som sattes någon annanstans, och proven kunde bara köras för att klassen var statisk. Den blev en böna med sökvägen i `autocore.db.path`, och varje repository får sin `Db` via `RepositoryFactory`.

`GDK_SCALE=2` hade vi inte skrivit upp. Variabeln låg kvar i en befintlig session och fördubblade hela gränssnittet, eftersom JavaFX ritar genom GTK. Ingen kod hade ändrats. Det syns nu som en varning i `start.sh`.

Behörighetsspärren täckte skrivningarna men inte läsningarna, så en anropare utan identitet kunde läsa hela registret. Det upptäcktes vid genomläsning efteråt och rättades i E3-7.

Det fristående programmet bokade alltid dagens datum plus trettio dagar, vilket är en lördag ibland. Det visade sig morgonen därpå, när samma kod och samma prov gav rött i stället för grönt.

## 3. Vilka delar av systemet som behöver förändras

- Bygget: `pom.xml` tillkommer och `start.sh` skrivs om.
- Datalagret: `Db` blir en böna, och `RepositoryFactory` fördelar den.
- Kärnan: `IdentityHolder` flyttar in, så att appen och det fristående programmet delar samma identitet.
- Gränssnittet: inloggningen blir ett kort i huvudfönstret i stället för ett eget fönster.
- Säkerheten: `Operation`, `RoleMatrix` och en dekoratör framför fasaden.
- Loggningen: `logback-spring.xml` med profil per miljö, och MDC med trace-id och operation.
- Bevisen: ut ur produktionsträdet, vilket är SCRUM-332.

## 4. Hur befintlig information ska bevaras

Migreringarna körs i ordning: `001_baslinjen.sql`, `002_nycklar.sql`, `003_ore.sql`, `004_users.sql`. 001 skapar grunden, 002 och 003 bygger om tabellerna för att få riktiga nycklar respektive öret som enhet, och 004 lägger till konton. Den som bygger om kopierar raderna in i den nya tabellen och släpper den gamla först efteråt.

`WigellAutoCore/autocore/test/data/autocore-original.db` är en riktig äldre databas som ligger i git. `OldDatabaseProof` lyfter den genom 4.0 och visar att innehållet står kvar: `customers=6 vehicles=6 bookings=6 work_orders=6 mechanics=3` före och efter, med `schema_version` 4 och tre konton som enda tillägg.

Att lösa migreringen genom att börja om med en tom databas är alltså uteslutet, både som princip och för att beviset då inte hade gått att köra.

## 5. Hur autentisering och behörighetsstyrning ska införas

Konton ligger i en egen tabell med roll och PBKDF2-hash. Hashen är 210 000 varv i migreringen och smälts om till 600 000 vid nästa lyckade inloggning. Lösenorden i klartext finns bara hos gruppen, och de kan sättas utifrån med miljövariabler som hashas vid start.

Inloggningen fyller en `IdentityHolder`. Rollmatrisen är data: 29 operationer och tre roller, där administratören har allt, verkstadspersonalen allt utom fem saker, och en läsbehörig får läsa och välja språk.

Kontrollen sitter i en dekoratör framför fasaden, inte i gränssnittet. Den täcker både skrivningar och läsningar, och den svarar olika för "ingen är inloggad" och "rollen får inte", eftersom kravet skiljer på dem. Samma dekoratör används i det fristående programmet.

## 6. Hur loggning och felhantering ska förbättras

Logback konfigureras i `logback-spring.xml` med två profiler. Utan profil, eller i dev, loggas vår egen kod på DEBUG till både konsol och fil. I prod loggas vår kod på INFO och allt annat på WARN, och bara till fil. Filen rullar vid 10 MB och sparas i fjorton dagar.

Varje rad bär tidpunkt, nivå, tråd och komponent. I MDC ligger ett trace-id och namnet på operationen, så en nekad åtkomst och raden som förklarar den hör ihop.

Verksamhetshändelser loggas när en arbetsorder skapas och slutförs. Säkerhetshändelser loggas när någon nekas, med användarnamn och operation men aldrig med det användaren skrev. Tekniska fel loggas med undantaget, medan användaren får en mening utan klassnamn och utan intern text. Loggningen hålls av två prov: `LogContentTest` läser loggfilen och letar trace-id, hemligheter och personuppgifter, och `EventLoggingTest` fångar raderna i minnet och prövar händelserna och användartexten. `SecretScanTest` hör till samma sak men läser källkoden i stället, och faller om en hemlighet skrivs in där.

## 7. Hur kravet på fristående verksamhetsfunktionalitet uppfylls

`StandaloneWorkOrders` är ett vanligt Java-program utan Spring, utan gränssnitt och utan webbserver. Det bokar och driver en arbetsorder hela vägen till `COMPLETED`, alltså två operationer där en är arbetsorderns livscykel.

Det använder samma `GarageSystem` och samma dekoratör som applikationen, så affärsreglerna är desamma. Identiteten sätts i hållaren på samma sätt som inloggningen gör det, och programmet kan köras utan identitet för att visa att spärren gäller även där.

## 8. Hur vi kontrollerar att tidigare funktionalitet fortfarande fungerar

Enhetstesterna är 69 JUnit-prov: inloggning, lösenordshashning och rotation, identitetshållaren, rollmatrisen, spärren för läsning och skrivning, raderingsregeln för en avbruten order, det fristående programmet, loggning och spårbarhet, hemlighetsskanning, och att Spring-kontexten startar med rätt databassökväg.

Bevisprogrammen är femton program som skriver ut sitt svar, och tolv av dem avslutar med en exitkod så att en körning kan fällas maskinellt. Tillsammans täcker de bokningar och ändringar av dem, arbetsorderns vägar och rader, utkast och validering, fakturor och betalningar, tal- och avrundningsregler, felhantering och felmeddelanden på båda språken, den gamla databasen, rollmatrisen, behörigheten, raderingen av en avbruten order och en samlad självkontroll.

Jämförelsen mot baslinjen: elva bevis spelades in på Java 8 i commit `ce1fde8`, den sista innan Maven-bygget. Samma elva kördes på 4.0. Tio svarar lika. Den elfte, `StatusRoutesProof`, prövade tre vägar av fyra eftersom körningen gjordes efter stängning, vilket är ett tidsberoende i provet och inte i koden. Det ligger som eget ärende att rätta.

Sviten körs vid varje ändring, och det var den som fångade att det fristående programmet bokade en dag som kunde vara stängd. Två saker hittades utanför den: den brutna `start.sh`, som föll först när någon startade den, och baslinjen som låg i `/tmp` och hade försvunnit när den skulle användas.
