# Genomgången: vad som visas, i vilken ordning

WAC4_0 §11 räknar upp åtta saker som ska visas vid slutredovisningen. Det här är ordningen de tas
i, kommandona som hör till, och vad som görs om något faller live. Dokumenten i [docs/](README.md)
är underlaget; den här sidan är det som ligger framme.

## Före ni börjar

Femton minuter innan, från repots rot:

```
git pull
mvn -o test -f WigellAutoCore/autocore/pom.xml     # 140 prov, 0 fel
```

Sedan classpath-filen som bevisprogrammen använder:

```
cd WigellAutoCore/autocore
mvn -o test-compile
mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt
```

Tre saker att ha öppna vid sidan av terminalen:

- [`kravtackning.md`](kravtackning.md), fliken med A–H-tabellen.
- [`arkitekturbeslut.md`](arkitekturbeslut.md), för punkten H.
- `logs/autocore.log` i en egen terminal med `tail -f`, för punkten F.

Ta bort gamla loggrader före demon, så att det som syns är det ni just gjorde:

```
rm -f logs/autocore.log
```

## A. Plattformsmigrering

Visa att appen byggs och startas med Spring Boot 4 och JDK 25.

```
./start.sh
```

Säg: startskriptet kontrollerar att JDK 25 och Maven 3.9 finns, och kör sedan samma
`mvn spring-boot:run` som IntelliJ gör. Inget installeras för hand.

Reservväg: `mvn -f WigellAutoCore/autocore/pom.xml package`, och visa att `BUILD SUCCESS` kommer
efter `Tests run: 140`.

## B. Bibehållen funktionalitet

Visa att den gamla databasen lyfts utan att något tappas.

```
cd WigellAutoCore/autocore
java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.proof.OldDatabaseProof test/data/autocore-original.db
```

Säg: det är en riktig databas från före 4.0, och den går genom migrering 004 med kunderna, fordonen
och arbetsordrarna i behåll.

Reservväg: utskrifterna ligger i [`bevis/`](bevis/README.md), och jämförelsen mot Java 8-baslinjen i
[`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md).

## C. Autentisering

Logga in i fönstret som `admin`, och visa att appen står på inloggningskortet tills någon gör det.

Säg: kontona ligger i tabellen `users` med PBKDF2-hash, aldrig i klartext, och appen går inte förbi
inloggningen.

Reservväg: konsolversionen, som frågar i terminalen och går genom samma spärr:

```
java -cp "target/classes:$(cat target/cp.txt)" com.wac.autocore.ConsoleApp data/autocore.db
```

## D. Behörighetskontroll med tre konton

Logga ut och in som `readonly`. Peka på att menyvalen och knapparna som hör till ekonomin och till
att skapa eller radera inte ritas alls.

Säg: tre roller (administratör, personal och läsbehörig), och den läsbehöriga ser dagens arbete men
varken ekonomin eller något som ändrar.

Reservväg: `RoleMatrixProof` skriver ut alla tre raderna, och `RoleMatrixTest` prövar dem.

## E. Ingen väg runt spärren

```
mvn -o test -Dtest=NoUnguardedConstructionTest
```

Säg: provet läser produktionskoden och fäller varje klass som skapar tjänsten direkt i stället för
genom spärren. Det kom till för att konsolen en gång tog just den genvägen.

Om någon frågar om gränssnittet: att dölja är artigheten, spärren är regeln. Ett klick som fyras
från kod konsumeras av filtret i `Actions`, och under båda lagren nekar tjänsten.

Reservväg: [`ADR-009`](arkitekturbeslut.md#adr-009-fönstret-frågar-spärren-innan-det-ritar) och
riskanalysens R14.

## F. Loggning: lyckad operation, nekad åtkomst, tekniskt fel

Två kommandon, och en läsning av filen. Alla tre raderna med utskrifter står i
[`demo-loggning.md`](demo-loggning.md).

```
# 1. lyckad operation
java -cp "target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/demo.db

# 2. nekad åtkomst, ingen inloggad
java -cp "target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/demo.db utan
```

```
tail -n 20 logs/autocore.log
```

Säg: varje rad bär tid, nivå, tråd, `trace` och `op`, så en nekad åtkomst och raden som förklarar
den hör ihop.

Det tekniska felet är tredje kommandot i [`demo-loggning.md`](demo-loggning.md), alltså appen mot en
databassökväg som inte går att öppna. Det kräver en bildskärm.

Reservväg: utskrifterna i [`demo-loggning.md`](demo-loggning.md) är kopierade ordagrant ur
körningarna.

## G. Fristående körläge

```
java --enable-native-access=ALL-UNNAMED -cp "target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/fristaende.db
```

Säg: två operationer, en bokning och en arbetsorder hela vägen till klar, utan Spring, utan
fönster och utan server. Sista raden är `STANDALONE_OK`.

Reservväg: `StandaloneWorkOrdersTest` prövar samma sak i sviten.

## H. Arkitektoniska beslut

Öppna [`arkitekturbeslut.md`](arkitekturbeslut.md) och peka på två ställen: tabellen med de nitton
besluten, och avsnittet
[Vad som underlättade och vad som försvårade](arkitekturbeslut.md#vad-som-underlättade-och-vad-som-försvårade).

Säg: det som underlättade var beslut som höll isär saker, som vyn från reglerna, reglerna från
lagringen och användarens text från den tekniska. Det som försvårade band ihop dem.

Reservväg: [`kravtackning.md`](kravtackning.md) har en rad per krav med bevis, om någon frågar
utanför de åtta punkterna.

## Om något faller live

Gå till dokumentet i stället för att felsöka framför publiken. Varje punkt ovan har en reservväg,
och varje påstående i dokumenten går att kontrollera i efterhand. Det är därför de ligger i repot
och inte i en presentationsmapp.

Tre saker att känna till i förväg:

Appen behöver en bildskärm. På Wayland sätter `start.sh` `GDK_BACKEND=x11` själv.

Fönstret ritas i skala 1 oavsett vad terminalen ärvde. Behöver skärmen en annan skala, kör
`AUTOCORE_UI_SCALE=2 ./start.sh`.

Bevisen bygger sina egna databaser. Det är bara appen som skriver i `data/autocore.db`, och
`SelfTest` kontrollerar att den filen är orörd.
