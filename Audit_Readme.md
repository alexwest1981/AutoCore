# Audit & Test Suite – Dokumentation och Arkitektur (`Audit_Readme.md`)

Välkommen till den officiella dokumentationen för AutoCores test- och granskningssvit.  
Detta dokument förklarar i detalj **hur testskripten fungerar, vad varje kontroll gör, varför den finns och hur du kör sviten på din dator** (oavsett om du kör Windows, macOS eller Linux).

---

## 1. Syfte och Filosofi

Test- och auditsviten (`test.sh`, `test.ps1`, `test.bat`) är byggd för att garantera att AutoCore uppfyller:
1. **Beställarens 12 acceptanskriterier** (se [`ACCEPTANSKRAV.md`](ACCEPTANSKRAV.md)).
2. **JIRA-beviskorten** för Sprint 3 (D1, D3, E4, F2, F3, F4, G1, G2, G3).
3. **Akademiska och professionella kvalitetskrav:** Ren arkitektur, flerspråkighet (I18n), WCAG 2.1 AAA tillgänglighet och hög applikationssäkerhet.
4. **Oberoende av externa verktyg:** Ingen utvecklare behöver ha Jira MCP eller särskilda IDE-plugins installerade för att köra och förstå testerna.

---

## 2. Filstruktur och Plattformsoberoende

Skriptet finns i tre varianter för att fungera sömlöst på alla operativsystem i teamet:

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
Samtliga skript anropar `javac` med flaggan `-encoding UTF-8`. Detta förhindrar `Cp1252`-teckenfel på Windows när källkoden eller testerna innehåller symboler som `✔`, `❌` eller svenska tecken (`å`, `ä`, `ö`).

---

## 3. De Sex Granskningsmodulerna i Detalj

När skriptet körs sekventiellt genomförs sex oberoende kontrollsteg:

```
[0/6] Kompilering (javac med UTF-8 och SQLite-drivrutin)
  │
  ├── [1/6] Smoketest (Hälsa, schema, klassladdning, uppstartstid)
  ├── [2/6] Enhetstester (56 tester för domänlogik och servicelager)
  ├── [3/6] JIRA Beviskort & Acceptanskrav (9 beviskort, omstart, migrering)
  ├── [4/6] Kodkvalitet & Arkitektur (I18n, temaintegritet, frikoppling)
  ├── [5/6] Säkerhetsanalys (SQL-injektion, hemligheter, processer)
  └── [6/6] WCAG 2.1 AAA Tillgänglighet (Kontrast >= 7.0:1, fokus, text)
```

---

### [1/6] Smoketest (`SmokeTest.java`)
**Mål:** Validera att applikationen är tekniskt frisk innan tyngre tester startas.
* **Databasintegritet:** Verifierar att SQLite-filen kan öppnas och att alla **10 tabeller** existerar:
  `customers`, `vehicles`, `mechanics`, `service_items`, `bookings`, `booking_service_items`, `work_orders`, `work_order_service_items`, `invoices`, `invoice_lines`, `payments`.
* **Klassladdning:** Laddar 13 kritiska klasser (inklusive presentations- och servicelager) för att avslöja saknade klassfiler eller länkfel (`NoClassDefFoundError`).
* **Resursvalidering:** Kontrollerar att språkresurser (`sv.json`, `en.json`) och stilmallar (`style.css`, temafiler) finns och kan läsas som strömmar.
* **Prestanda vid uppstart:** Säkerställer att initialisering och schemavalidering sker på under **2,0 sekunder** (< 200 ms på moderna maskiner).

---

### [2/6] Enhetstester (56 tester)
**Mål:** Verifiera domänlogik, beräkningar och servicelager i detalj.
* **Flertjänstbokning (`BookingServicesTest`):** Validerar flertjänstassociationer, statuslåsning vid startat arbete (`IN_PROGRESS`), samt sammanlagd beräkning av tidsåtgång (minuter) och baspris.
* **Arbetsordrar (`WorkOrderTest`):** Validerar livscykeln `CREATED` -> `IN_PROGRESS` -> `COMPLETED`, koppling till mekaniker samt att tjänsterna överförs från bokningen.
* **Fakturarader (`InvoiceLineTest`):** Validerar att varje utförd tjänst får en separat rad, att radsumman matchar fakturans delbelopp och att historiska rader fryses.
* **Schemakonflikter (`ScheduleConflictTest`):** Verifierar mekanikerscheman, tidsblockeringar och överlappningsskydd.
* **Persistens och Mätetal:** Enhetstester för repository-CRUD och KPI-rapportering.

---

### [3/6] JIRA Beviskort & Mätbara Acceptanskrav (`EvidenceVerificationTest.java`)
**Mål:** Formellt bevisa beställarens 12 acceptanskriterier och sprintens 9 beviskort:

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
**Mål:** Säkra professionell kodstandard och arkitektur.
* **100% Språkparitet:** Kontrollerar att samtliga nycklar i `sv.json` återfinns i `en.json` (och vice versa).
* **Teckenkodningsskydd (Mojibake):** Skannar JSON-språkfiler efter felkodade tecken (`Ã¥`, `Ã¤`, `Ã¶`, `Ã©`) och tomma översättningar `""`.
* **Arkitektonisk frikoppling:** Verifierar att servicelagret (`com.wac.autocore.service.*`) inte importerar eller har några beroenden mot presentationslagret (`javafx.*` eller `ui.*`).
* **Filstorleksbegränsning:** Validerar att ingen Java-källkodsfil överskrider 800 rader för att motverka "God Objects".
* **Statisk TODO/FIXME-analys:** Räknar kvarvarande TODO-noteringar i produktionskällkoden.

---

### [5/6] Säkerhetsanalys (`SecurityAuditTest.java`)
**Mål:** Förhindra säkerhetssårbarheter och dataläckor.
* **SQL-injektionsskydd:** Statisk analys som säkerställer att alla SQL-satser i servicelager och repositories använder parametriserade `PreparedStatement` (`?`) istället för dynamisk strängkonkatenering.
* **Inga hårdkodade hemligheter:** Skannar efter mönster för råa lösenord, API-nycklar och hemliga tokens.
* **Skydd för känslig person- och betaldata:** Förhindrar att personnummer, lösenord eller kreditkortsnummer skrivs ut till terminalen via `System.out` / `System.err`.
* **Process- och Runtime-skydd:** Verifierar att applikationskoden inte anropar `Runtime.getRuntime().exec` eller `ProcessBuilder` oskyddat.
* **Versionshanteringsskydd:** Validerar att `.gitignore` aktivt skyddar lokala rapporter, loggböcker och databasfiler.

---

### [6/6] WCAG 2.1 AAA Tillgänglighet (`WcagAccessibilityTest.java`)
**Mål:** Garantera att gränssnittet är tillgängligt för alla användare, inklusive personer med synnedsättningar.
* **Färgkontrast >= 7.0:1:** Beräknar färgkontrast enligt W3C:s formel för relativ luminans:
  $$\text{Kontrastkvot} = \frac{L_1 + 0.05}{L_2 + 0.05}$$
  Alla standardtextelement i aktivt tema (*Emerald*) måste ha en kontrastkvot på minst **7.0:1** mot sin bakgrund (WCAG 2.1 AAA).
* **Fokusindikatorer:** Verifierar att interaktiva komponenter (knappar, textfält, tabeller) har tydliga fokusramar definierade i CSS.
* **Minsta Textstorlek:** Validerar att all brödtext och alla kontrollelement använder minst 11 px fontstorlek.

---

## 4. Automatisk Rapportgenerering (`rapport.md`)

Varje gång hela skriptet körs skapas eller uppdateras filen [`rapport.md`](file:///home/alex/Documents/Skolgrejer/Systemarkitektur/rapport.md) i projektets rot.  
Rapporten innehåller:
1. **Exekveringsmetadata:** Datum, klockslag, aktiv git-branch, git-commit, Java-version och operativsystem.
2. **Kvantitativ sammanfattning:** Resultat för samtliga 6 moduler (88/88 tester).
3. **Acceptanskravsmatris:** Detaljerad tabell med status för Kriterium 1 till 12 (länkat till [`ACCEPTANSKRAV.md`](ACCEPTANSKRAV.md)).
4. **Detaljerade bevisutfall:** Tabeller med radantal före/efter för migrering, PID-värden för omstartstestet, rabattberäkningar och provdata.

> **Observera:** `rapport.md` och personliga analysfiler ignoreras av `.gitignore` och ska aldrig pushas till det gemensamma repositoriet.

---

## 5. Så Kör Du Skriptet

### Köra hela sviten (Alla 88 tester och generera `rapport.md`)
```bash
# Linux / macOS:
./test.sh

# Windows (PowerShell):
.\test.ps1

# Windows (Kommandotolken):
test.bat
```

### Köra enskilda delmoduler
Om du bara vill testa en specifik del under pågående utveckling:

```bash
# Kör enbart Smoketest:
./test.sh smoke

# Kör enbart Enhetstester (56 tester):
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
* Registrera metoden i `TestRunner.java` under rätt modul för att den automatiskt ska ingå i `./test.sh`.
