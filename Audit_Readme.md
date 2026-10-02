# Audit & Test Suite – Dokumentation och Arkitektur (`Audit_Readme.md`)

Här är dokumentationen för AutoCores test- och granskningssvit.  
Den går igenom **hur testskripten fungerar, vad varje kontroll gör, varför den finns och hur du kör sviten på din dator**, oavsett om du kör Windows, macOS eller Linux.

---

## 1. Syfte och Filosofi

Test- och auditsviten (`test.sh`, `test.ps1`, `test.bat`) ska visa att AutoCore uppfyller:
1. **Beställarens 12 acceptanskriterier** (se [`ACCEPTANSKRAV.md`](ACCEPTANSKRAV.md)).
2. **JIRA-beviskorten** för Sprint 3 (D1, D3, E4, F2, F3, F4, G1, G2, G3).
3. **Akademiska och professionella kvalitetskrav:** Ren arkitektur, flerspråkighet (I18n), WCAG 2.1 AAA tillgänglighet och hög applikationssäkerhet.
4. **Oberoende av externa verktyg:** Ingen utvecklare behöver ha Jira MCP eller särskilda IDE-plugins installerade för att köra och förstå testerna.

---

## 2. Filstruktur och Plattformsoberoende

Det finns tre varianter av skriptet, så att det fungerar på alla operativsystem i teamet:

| Fil | Operativsystem | Skaltyp / Miljö |
|---|---|---|
| `test.sh` | Linux & macOS | Bash (körs direkt i terminalen via `./test.sh`) |
| `test.ps1` | Windows | PowerShell (`.\test.ps1`) |
| `test.bat` | Windows | Kommandotolken (CMD) (`test.bat`) |

### Automatisk JDK-detektering & Manuell Konfiguration
Skripten letar automatiskt efter en installerad Java 8 / Liberica JDK i standardkataloger:
- **Linux:** `/usr/lib/jvm`, `/home/$USER/jdks`, `/opt/java`
- **macOS:** `/Library/Java/JavaVirtualMachines`, `/usr/local/opt/openjdk`
- **Windows:** `C:\Program Files\BellSoft\LibericaJDK-*`, `C:\Program Files\Java\*`

> **Tips om din JDK inte hittas:**  
> Öppna skriptet (`test.sh`, `test.ps1` eller `test.bat`) och ange sökvägen manuellt i variabeln `CUSTOM_JDK`:
> ```bash
> CUSTOM_JDK="/home/din-anvandare/jdks/jdk8u504-full"
> ```

### UTF-8 Teckenkodning vid Kompilering
Alla skript kör `javac` med flaggan `-encoding UTF-8`. Det hindrar `Cp1252`-fel på Windows när källkoden eller testerna innehåller symboler som `✔`, `❌` eller svenska tecken (`å`, `ä`, `ö`).

---

## 3. De Sex Granskningsmodulerna i Detalj

När skriptet körs går det igenom sex oberoende kontrollsteg i tur och ordning:

```
[0/6] Kompilering (javac med UTF-8 och SQLite-drivrutin)
  │
  ├── [1/6] Smoketest (Hälsa, schema, klassladdning, uppstartstid)
  ├── [2/6] Enhetstester (domänlogik och servicelager)
  ├── [3/6] JIRA Beviskort & Acceptanskrav (9 beviskort, omstart, migrering)
  ├── [4/6] Kodkvalitet & Arkitektur (I18n, temaintegritet, frikoppling)
  ├── [5/6] Säkerhetsanalys (SQL-injektion, hemligheter, processer)
  └── [6/6] WCAG 2.1 AAA Tillgänglighet (Kontrast >= 7.0:1, fokus, text)
```

---

### [1/6] Smoketest (`SmokeTest.java`)
**Mål:** Se att applikationen är tekniskt frisk innan de tyngre testerna startar.
* **Databasintegritet:** Kollar att SQLite-filen går att öppna och att alla **10 tabeller** finns där:
  `customers`, `vehicles`, `mechanics`, `service_items`, `bookings`, `booking_service_items`, `work_orders`, `work_order_service_items`, `invoices`, `invoice_lines`, `payments`.
* **Klassladdning:** Laddar 13 kritiska klasser (inklusive presentations- och servicelager) för att fånga saknade klassfiler och länkfel (`NoClassDefFoundError`).
* **Resursvalidering:** Ser att språkresurser (`sv.json`, `en.json`) och stilmallar (`style.css`, temafiler) finns och går att läsa som strömmar.
* **Prestanda vid uppstart:** Ser till att initialisering och schemavalidering sker på under **2,0 sekunder** (< 200 ms på moderna maskiner).

---

### [2/6] Enhetstester
**Mål:** Gå igenom domänlogik, beräkningar och servicelager i detalj.
* **Flertjänstbokning (`BookingServicesTest`):** Kollar att flera tjänster kan kopplas ihop, att statusen låses när arbetet startat (`IN_PROGRESS`), och att tidsåtgång (minuter) och baspris räknas samman.
* **Arbetsordrar (`WorkOrderTest`):** Följer livscykeln `CREATED` -> `IN_PROGRESS` -> `COMPLETED`, kopplingen till mekaniker och att tjänsterna hänger med från bokningen.
* **Fakturarader (`InvoiceLineTest`):** Kollar att varje utförd tjänst får en egen rad, att radsumman stämmer med fakturans delbelopp och att historiska rader fryses.
* **Schemakonflikter (`ScheduleConflictTest`):** Går igenom mekanikerscheman, tidsblockeringar och skyddet mot överlapp.
* **Persistens och Mätetal:** Enhetstester för repository-CRUD och KPI-rapportering.

---

### [3/6] JIRA Beviskort & Mätbara Acceptanskrav (`EvidenceVerificationTest.java`)
**Mål:** Lägga formella bevis för beställarens 12 acceptanskriterier och sprintens 9 beviskort:

1. **SCRUM-159 (D1 Beviskort – Prisändring kontrollerad hela vägen):**
   Höjer priset i katalogen från 899 kr till 1 299 kr. Skapar en ny bokning/faktura till det nya priset och bevisar att den äldre fakturan behåller 899 kr.
2. **SCRUM-161 (D3 Beviskort – Historiken syns i UI):**
   Bevisar att samma tjänst presenteras med två olika priser på två olika arbetsordrar och fakturor i gränssnittet.
3. **SCRUM-165 (E4 Beviskort – VIP och Rabattkoder):**
   Verifierar 10% VIP-rabatt, 10% med `WELCOME10`, 200 kr med `SERVICE200` samt golv vid 0.00 kr.
4. **SCRUM-168 (F2 Beviskort – Rundtur för nya tabeller):**
   Full livscykel (Create -> Read -> Update -> Read -> Delete) för `booking_service_items` och `invoice_lines`.
5. **SCRUM-169 (F3 Beviskort – Verkligt omstartsbevis):**
   Körs via [`RestartProofRunner.java`](file:///home/alex/Documents/Skolgrejer/Systemarkitektur/WigellAutoCore/autocore/src/com/wac/autocore/test/RestartProofRunner.java).
   - **Process 1 (Writer, PID X):** Startar i en separat JVM, skapar canary-poster i SQLite och anropar `System.exit(0)`.
   - **Process 2 (Verifier, PID Y där PID Y ≠ PID X):** Startar i en helt ny JVM från operativsystemet och verifierar 100% dataintegritet.
6. **SCRUM-170 (F4 Beviskort – Migreringsbevis med 0 dataförlust):**
   Skapar en ren AutoCore 2.0-databas (8 tabeller). Räknar rader före migrering (3 kunder, 3 fordon, 2 mekaniker, 4 tjänster, 3 bokningar, 2 arbetsorder, 2 fakturor, 2 betalningar). Kör migrering till 2.5 och bevisar att inte en enda rad förlorades, samt att kopplingstabellerna fylldes.
7. **SCRUM-171 (G1 Beviskort – Grön svit):**
   Verifierar att hela testsviten passerar sekventiellt med 100% pass rate.
8. **SCRUM-172 (G2 Beviskort – De nio kärnområdena):**
   Operationella tester för samtliga nio systemområden (Kunder, Fordon, Bokningar, Mekaniker, Arbetsordrar, Fakturering, Betalningar, Rabatter, Flerspråkighet).
9. **SCRUM-173 (G3 Beviskort – Demonstrationsflöde för utförda arbeten):**
   Demonstrerar att när en kund bokar 3 tjänster och 2 utförs, är det enbart de 2 utförda tjänsterna som hamnar på fakturan. Outförda moment debiteras ej.

---

### [4/6] Kodkvalitet & Arkitektur (`CodeQualityTest.java`)
**Mål:** Hålla koden och arkitekturen på en professionell nivå.
* **100% Språkparitet:** Kollar att varje nyckel i `sv.json` också finns i `en.json` (och tvärtom).
* **Teckenkodningsskydd (Mojibake):** Skannar JSON-språkfiler efter felkodade tecken (`Ã¥`, `Ã¤`, `Ã¶`, `Ã©`) och tomma översättningar `""`.
* **Arkitektonisk frikoppling:** Ser att servicelagret (`com.wac.autocore.service.*`) inte importerar eller på annat sätt hänger på presentationslagret (`javafx.*` eller `ui.*`).
* **Filstorleksbegränsning:** Kräver att ingen Java-källkodsfil går över 1 200 rader, som skydd mot monolitiska "God Objects" (största komponenten `MechanicKanbanCard.java` är 1 022 rader).
* **Statisk TODO/FIXME-analys:** Räknar kvarvarande TODO-noteringar i produktionskällkoden.

---

### [5/6] Säkerhetsanalys (`SecurityAuditTest.java` + `DataFlowAuditTest.java`)
**Mål:** Stoppa säkerhetshål och dataläckor.
* **SQL-injektionsskydd:** Statisk analys som kräver att alla SQL-satser i servicelager och repositories använder parametriserade `PreparedStatement` (`?`) istället för att klistra ihop strängar.
* **Dataflödesanalys (SQL och processer):** `DataFlowAuditTest` följer var en sträng kommer ifrån i stället för att titta på en rad i taget. En fråga som byggs med `+` på en rad och körs på en annan, vilket mönsterletningen ovan inte ser, fångas, eftersom analysen läser kompilatorns eget träd (`javac`). Kända konstanter (`private static final String TABELL = "payments"`) och literaler är ofarliga; allt annat som sammanfogas blir data och får inte nå `executeQuery`/`executeUpdate`/`prepareStatement` eller ett processanrop. Analysen har ett eget prov (`testTheAnalysisCatchesWhatThePatternMisses`) som bevisar att den fäller det mönstret och lämnar den ofarliga varianten i fred. En granskning som aldrig larmar ser annars lika grön ut som en ren kodbas.
* **Inga hårdkodade hemligheter:** Skannar efter råa lösenord, API-nycklar och hemliga tokens.
* **Skydd för känslig person- och betaldata:** Stoppar personnummer, lösenord och kreditkortsnummer från att skrivas ut till terminalen via `System.out` / `System.err`.
* **Process- och Runtime-skydd:** Verifierar att applikationskoden inte anropar `Runtime.getRuntime().exec` eller `ProcessBuilder` oskyddat.
* **Versionshanteringsskydd:** Kollar att `.gitignore` faktiskt skyddar lokala rapporter, loggböcker och databasfiler.

*Kända gränser för dataflödesanalysen:* ingen typanalys och ingen analys över metodgränser. Ett värde som byggs i en annan metod och skickas in följs inte. Det står också i `KONTROLLER.md`.

---

### [6/6] WCAG 2.1 AAA Tillgänglighet (`WcagAccessibilityTest.java`)
**Mål:** Se till att gränssnittet går att använda för alla, även personer med synnedsättning.
* **Färgkontrast >= 7.0:1:** Räknar ut färgkontrast enligt W3C:s formel för relativ luminans:
  $$\text{Kontrastkvot} = \frac{L_1 + 0.05}{L_2 + 0.05}$$
  Alla standardtextelement i aktivt tema (*Emerald*) måste ha en kontrastkvot på minst **7.0:1** mot sin bakgrund (WCAG 2.1 AAA).
* **Fokusindikatorer:** Kollar att interaktiva komponenter (knappar, textfält, tabeller) har tydliga fokusramar i CSS.
* **Minsta Textstorlek:** Ser till att all brödtext och alla kontrollelement håller minst 11 px fontstorlek.

---

## 4. Automatisk Rapportgenerering (`rapport.md`)

Varje gång du kör hela skriptet skapas eller uppdateras filen [`rapport.md`](file:///home/alex/Documents/Skolgrejer/Systemarkitektur/rapport.md) i projektets rot.  
Rapporten innehåller:
1. **Exekveringsmetadata:** Datum, klockslag, aktiv git-branch, git-commit, Java-version och operativsystem.
2. **Kvantitativ sammanfattning:** Resultat för samtliga 6 moduler.
3. **Acceptanskravsmatris:** Detaljerad tabell med status för Kriterium 1 till 12 (länkat till [`ACCEPTANSKRAV.md`](ACCEPTANSKRAV.md)).
4. **Detaljerade bevisutfall:** Tabeller med radantal före/efter för migrering, PID-värden för omstartstestet, rabattberäkningar och provdata.

> **Observera:** `.gitignore` ignorerar `rapport.md` och personliga analysfiler, och de ska aldrig pushas till det gemensamma repositoriet.

---

## 5. Så Kör Du Skriptet

### Köra hela sviten och generera `rapport.md`
```bash
# Linux / macOS:
./test.sh

# Windows (PowerShell):
.\test.ps1

# Windows (Kommandotolken):
test.bat
```

### Köra enskilda delmoduler
Om du bara vill testa en viss del medan du utvecklar:

```bash
# Kör enbart Smoketest:
./test.sh smoke

# Kör enbart Enhetstester:
./test.sh unit

# Kör enbart JIRA Beviskort & Acceptanskrav (9 bevis):
./test.sh bevis

# Kör enbart Kodkvalitet & Arkitektur:
./test.sh quality

# Kör enbart Säkerhetskontroll:
./test.sh security

# Kör enbart WCAG 2.1 AAA:
./test.sh wcag
```

---

## 6. Felsökning och Vanliga Frågor (FAQ)

### 1. "Kunde inte hitta javac / java"
* **Lösning:** Kontrollera att du har Java 8 installerat (helst BellSoft Liberica Full med JavaFX). Öppna `test.sh` eller `test.ps1` och sätt sökvägen manuellt i `CUSTOM_JDK="..."`.

### 2. "Cp1252-teckenfel vid kompilering på Windows"
* **Lösning:** Skripten använder automatiskt `-encoding UTF-8` på `javac`. Om du kompilerar manuellt via kommandoraden, lägg alltid till `-encoding UTF-8`.

### 3. "SQLite databas låst (database is locked)"
* **Lösning:** Se till att du inte har huvudapplikationen (`./start.sh` eller IntelliJ) igång samtidigt som testskriptet körs, då SQLite i fil-läge kan låsas av en pågående transaktion.

### 4. "Hur lägger jag till ett nytt test?"
* Skapa din testmetod i lämplig testklass under `WigellAutoCore/autocore/src/com/wac/autocore/test/`.
* Använd `TestRunner.assertEquals`, `TestRunner.assertTrue` eller `TestRunner.assertFalse`.
* En ny testklass hittas automatiskt om filnamnet slutar på `Test.java`, och metoderna körs om de börjar med `test` och inte tar några argument. Ska klassen höra till en annan modul än Enhetstester, lägg till den i `GROUP_OF` i `TestRunner.java`.

### 5. "Kompileringsfel: package com.sun.source does not exist"
* **Orsak:** `DataFlowAuditTest` läser kompilatorns eget träd, och de klasserna ligger i JDK:ns `lib/tools.jar`. Skripten lägger automatiskt till den filen på klassökvägen när den finns.
* **Lösning:** Kör med ett JDK och inte ett JRE. Kontrollera att `<JDK>/lib/tools.jar` finns; peka annars `CUSTOM_JDK` mot din JDK 8-installation.
