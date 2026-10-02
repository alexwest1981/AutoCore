# Wigell AutoCore 2.5 – Specifikation av Mätbara Acceptanskrav

Här hittar du samtliga **mätbara acceptanskrav (AK-01 t.o.m. AK-16)** för AutoCore 2.5 (Sprint 3).  
Alla i utvecklingsteamet har inte Jira MCP eller en extern Jira-koppling i sin lokala miljö. Därför är det här dokumentet den **fristående källan för krav och mätetal**.

Vem som helst kan verifiera och mäta samtliga krav automatiskt via testskripten:
* **Linux / macOS:** `./test.sh`
* **Windows (PowerShell):** `.\test.ps1`
* **Windows (CMD):** `test.bat`
* **IntelliJ / Eclipse:** Körning av `com.wac.autocore.test.TestRunner`

---

## 1. Mätprinciper och Definition av Godkänd (Pass/Fail)

Vi mäter varje acceptanskrav på tre nivåer:
1. **Deterministiskt testfall:** Ett eller flera automatiserade Java-testfall som kör exakt den affärslogik eller databasoperation som kravet pekar på.
2. **Mätbart utfall (Kvantitativt kriterium):** Exakta värden (t.ex. radantal före vs efter, tidsåtgång i minuter, belopp i kronor och ören, kontrastkvot >= 7.0:1) som ska stämma ända ned på decimalen.
3. **Auditstatus:** Alla 115 tester i testsviten måste passera med 100% grönt utfall innan systemet räknas som godkänt för release.

---

## 2. Kärn-Acceptanskriterier (Kriterium 1–12)

### AK-01: Bokning med flera tjänster
* **Krav:** En kundbokning ska kunna innehålla en eller flera olika verkstadstjänster samtidigt (flertjänstbokning).
* **Mätmetod & Kriterium:**
  - Skapa en bokning och associera 3 distinkta tjänster: *Oljebyte (id 1)*, *Bromsservice (id 2)* och *Däckbyte (id 3)*.
  - Läsa tillbaka bokningen från databasen (`booking_service_items`).
  - **Godkänt mätvärde:** Antal associerade tjänster == 3, och samtliga IDs matchar de valda tjänsterna.
* **Implementation:** Tabell `booking_service_items`, modell `Booking.serviceItemIds`, repository `BookingRepository`.
* **Kopplat testfall:** `BookingServicesTest.testBookingWithMultipleServices`
* **Jira-spårbarhet:** SCRUM-147, SCRUM-148, SCRUM-151
* **Status:** GODKÄND (100%)

---

### AK-02: Tjänster kan ändras innan arbetet påbörjats
* **Krav:** Så länge bokningens status är `BOOKED` ska man kunna lägga till eller ta bort tjänster på en bokning. Så fort arbetet har påbörjats (`IN_PROGRESS`) eller slutförts (`COMPLETED`) ska systemet avvisa varje försök till ändring med ett undantag, och gränssnittet ska inaktivera ändringsknapparna.
* **Mätmetod & Kriterium:**
  - Lägga till en tjänst när bokningen har status `BOOKED` -> Ska lyckas utan undantag.
  - Sätta status till `IN_PROGRESS` och anropa `addServiceItem` / `removeServiceItem`.
  - **Godkänt mätvärde:** `IllegalStateException` måste kastas vid försök till ändring i status `IN_PROGRESS` eller `COMPLETED`.
* **Implementation:** `BookingService.addServiceToBooking`, `BookingService.removeServiceFromBooking`, `Booking.canModifyServices`.
* **Kopplat testfall:** `BookingServicesTest.testCannotModifyServicesWhenWorkStarted`
* **Jira-spårbarhet:** SCRUM-150, SCRUM-154
* **Status:** GODKÄND (100%)

---

### AK-03: Total beräknad arbetstid visas
* **Krav:** Systemet ska automatiskt beräkna och visa den sammanlagda tidsåtgången (i minuter) för alla tjänster som ingår i bokningen.
* **Mätmetod & Kriterium:**
  - Välj tjänster med kända tidsestimat: *Oljebyte (45 min)*, *Bromsservice (90 min)*, *Däckbyte (30 min)*.
  - Anropa `Booking.getTotalEstimatedMinutes()`.
  - **Godkänt mätvärde:** Summan ska vara exakt `45 + 90 + 30 = 165` minuter.
* **Implementation:** `Booking.getTotalEstimatedMinutes`, `BookingDialogs`, `EntityPages`.
* **Kopplat testfall:** `BookingServicesTest.testEstimatedTimeAndCostCalculationsAndFormatting`
* **Jira-spårbarhet:** SCRUM-153
* **Status:** GODKÄND (100%)

---

### AK-04: Total beräknad kostnad visas
* **Krav:** Systemet ska i realtid beräkna och presentera den totala uppskattade grundkostnaden för alla valda tjänster i bokningsvyn och översikten.
* **Mätmetod & Kriterium:**
  - Välj tjänster: *Oljebyte (899.00 kr)*, *Bromsservice (1 495.00 kr)*, *Däckbyte (399.00 kr)*.
  - Anropa `Booking.getTotalEstimatedCost()`.
  - **Godkänt mätvärde:** Summan ska vara exakt `899.0 + 1495.0 + 399.0 = 2 793.00 kr` (tolerans < 0.001 kr).
* **Implementation:** `Booking.getTotalEstimatedCost`, `BookingDialogs`.
* **Kopplat testfall:** `BookingServicesTest.testEstimatedTimeAndCostCalculationsAndFormatting`
* **Jira-spårbarhet:** SCRUM-153
* **Status:** GODKÄND (100%)

---

### AK-05: Arbetsordern innehåller och visar arbeten som ska utföras
* **Krav:** När en arbetsorder skapas utifrån en bokning ska alla beställda tjänster följa med automatiskt. Arbetsordern ska i gränssnittet visa samtliga ingående moment, priser, beräknad arbetstid och status (utförd vs att utföra), så att mekanikern exakt ser vilka moment som ska genomföras.
* **Mätmetod & Kriterium:**
  - Skapa arbetsorder från bokning med 3 tjänster (id 1, 2, 3).
  - Läsa tillbaka arbetsordern från databasen (`work_order_service_items`) och verifiera visning i UI (`EntityLookup`, `WorkOrderDialogs`).
  - **Godkänt mätvärde:** Arbetsorderns tjänstelista innehåller samtliga tjänster med namn, frysta/aktuella priser, total beräknad arbetstid summerad, samt individuell statusmarkering per moment.
* **Implementation:** Tabell `work_order_service_items`, modell `WorkOrder`, `WorkOrderDialogs.showWorkOrderDetailsDialog`, `EntityLookup.workOrderTotalMinutes`, `EntityLookup.workOrderServicesWithStatus`.
* **Kopplat testfall:** `WorkOrderServiceTest.testWorkOrderDisplaysServicesToBePerformed`
* **Jira-spårbarhet:** SCRUM-156, SCRUM-157, SCRUM-158
* **Status:** GODKÄND (100%)

---

### AK-06: Fakturan har flera fakturarader
* **Krav:** När en arbetsorder faktureras ska fakturan inte bara visa ett klumpbelopp, utan specificera en separat fakturarad per utförd tjänst med namn, baspris, rabatt och radbelopp.
* **Mätmetod & Kriterium:**
  - Slutföra en arbetsorder med 2 utförda tjänster och generera faktura via `BillingService.createInvoice()`.
  - Hämta fakturaraderna från tabellen `invoice_lines`.
  - **Godkänt mätvärde:** Antal fakturarader == 2. Rad 1 har namn och pris för tjänst 1; Rad 2 har namn och pris för tjänst 2. Summan av raderna matchar fakturans delsumma före rabatt.
* **Implementation:** Tabell `invoice_lines`, modell `InvoiceLine`, repository `InvoiceRepository`.
* **Kopplat testfall:** `InvoiceLineTest.testOneLinePerPerformedService`
* **Jira-spårbarhet:** SCRUM-162, SCRUM-163
* **Status:** GODKÄND (100%)

---

### AK-07: Pris på en tjänst kan ändras i katalogen
* **Krav:** Verkstadsadministratören ska kunna ändra baspriset på en tjänst i tjänstekatalogen när som helst. Det nya priset ska gälla direkt för alla framtida bokningar och faktureringar.
* **Mätmetod & Kriterium:**
  - Ändra pris på tjänst ID 1 från 899.00 kr till 1 099.00 kr via `GarageSystem.updateServiceItem()`.
  - Skapa en ny bokning och arbetsorder för tjänst ID 1.
  - Generera ny faktura.
  - **Godkänt mätvärde:** Fakturaraden för den nya fakturan ska visa det nya priset `1 099.00 kr`.
* **Implementation:** `ServiceItemRepository.update`, `GarageSystem.updateServiceItem`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum159PriceChangeControlledAllTheWay`
* **Jira-spårbarhet:** SCRUM-159 (D1 Beviskort)
* **Status:** GODKÄND (100%)

---

### AK-08: Prisändring påverkar inte gamla arbeten eller fakturor (Prisfrysning)
* **Krav:** En prishöjning i katalogen får ALDRIG ändra beloppet på tidigare slutförda arbetsordrar eller utfärdade fakturor. Historiska priser ska ligga orörda både i databasen och i gränssnittet.
* **Mätmetod & Kriterium:**
  - Faktura A skapas för Oljeservice till pris 899.00 kr.
  - Priset höjs i katalogen till 1 299.00 kr.
  - Faktura B skapas för Oljeservice till nya priset 1 299.00 kr.
  - Läsa ut Faktura A och Faktura B via UI-uppslag (`EntityLookup.workOrderServicesWithPrices`).
  - **Godkänt mätvärde:** Faktura A och Arbetsorder A visar fortfarande exakt `899.00 kr`, medan Faktura B och Arbetsorder B visar `1 299.00 kr`. Samma tjänst visas med två olika priser på två olika ordrar.
* **Implementation:** Frysta priser i `invoice_lines.price`, `EntityLookup.workOrderServicesWithPrices`.
* **Kopplat testfall:** `InvoiceLineTest.testPriceChangeDoesNotChangeSavedLines`, `EvidenceVerificationTest.testScrum161HistoricalPricesVisibleInUi`
* **Jira-spårbarhet:** SCRUM-160, SCRUM-161 (D3 Beviskort)
* **Status:** GODKÄND (100%)

---

### AK-09: Rabattfunktioner fungerar med den nya fakturamodellen
* **Krav:** Systemet ska tillämpa befintliga rabattregler (VIP-rabatt 10%, rabattkod `WELCOME10` 10%, kampanjkod `SERVICE200` 200 kr) korrekt på summan av fakturaraderna. Slutbeloppet får aldrig bli negativt.
* **Mätmetod & Kriterium:**
  - Testfall 1 (VIP): Kund med `vip=true` -> Delsumma 1 295.00 kr ger exakt 10% rabatt (129.50 kr) -> Slutbelopp `1 165.50 kr`.
  - Testfall 2 (`WELCOME10`): Rabattkod ger exakt 10% avdrag på fakturan -> Slutbelopp `1 165.50 kr`.
  - Testfall 3 (`SERVICE200`): Kampanjkod ger exakt 200.00 kr fast avdrag -> Slutbelopp `1 095.00 kr`.
  - Testfall 4 (Spärr): Rabatt som överstiger delsumman spärras vid `totalAmount == 0.00 kr`.
* **Implementation:** `BillingService.calculateDiscount`, `BillingService.createInvoice`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum165VipAndDiscountCodesWorkAsBefore`
* **Jira-spårbarhet:** SCRUM-165, SCRUM-166 (E4 Beviskort)
* **Status:** GODKÄND (100%)

---

### AK-10: Ny information sparas permanent i databasen
* **Krav:** All data rörande flertjänstkopplingar (`booking_service_items`) och fakturarader (`invoice_lines`) ska sparas relationellt i SQLite via JDBC och stödja fullständig livscykel (Create, Read, Update, Delete).
* **Mätmetod & Kriterium:**
  - Genomföra en fullständig rundtur för båda tabellerna: Infoga canary-poster, läsa tillbaka och verifiera fält, uppdatera relationen, verifiera ändringen och slutligen radera posterna.
  - **Godkänt mätvärde:** Samtliga JDBC-anrop lyckas utan SQLite-undantag och radavstämning är 100% konsistent.
* **Implementation:** DDL i `Db.java`, repository-klasser med `PreparedStatement`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum168RoundtripForNewEntities`
* **Jira-spårbarhet:** SCRUM-167, SCRUM-168 (F2 Beviskort)
* **Status:** GODKÄND (100%)

---

### AK-11: Informationen finns kvar efter verklig processomstart (Omstartsbevis & Noll Dataförlust)
* **Krav:** 
  1. Vi ska bevisa dataintegriteten över en **verklig processomstart i operativsystemet** (inte bara via minnescache eller nya Java-objekt i samma JVM).
  2. Migrering från AutoCore 2.0 till 2.5 ska ske med **noll dataförlust** över alla befintliga tabeller.
* **Mätmetod & Kriterium:**
  - **Del A (Tvåprocessomstart):** `RestartProofRunner` startar Process 1 (Writer, PID X) som sparar canary-relationer och terminerar (`exit 0`). Därefter startas Process 2 (Verifier, PID Y där PID Y ≠ PID X) i en helt ny JVM som verifierar att alla poster och relationer kvarstår orörda.
  - **Del B (Migrering):** Skapa legacy AutoCore 2.0-databas med 8 tabeller. Räkna rader före migrering (`countsBefore`). Kör AutoCore 2.5-migrering. Räkna rader efter migrering (`countsAfter`).
  - **Godkänt mätvärde:**
    - Process 1 exitkod == 0, Process 2 exitkod == 0, PID X ≠ PID Y.
    - För samtliga 8 tabeller: `countsBefore[table] == countsAfter[table]` (exakt 0 rader förlorade).
* **Implementation:** `RestartProofRunner.java`, `EvidenceVerificationTest.testScrum169RestartEvidence`, `EvidenceVerificationTest.testScrum170ExistingDataRetained`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum169RestartEvidence`, `testScrum170ExistingDataRetained`
* **Jira-spårbarhet:** SCRUM-169 (F3 Beviskort), SCRUM-170 (F4 Beviskort)
* **Status:** GODKÄND (100%)

---

### AK-12: Befintlig funktionalitet fungerar intakt (De 9 Kärnområdena)
* **Krav:** Införandet av flertjänster och fakturarader får inte bryta eller degradera någon av de nio befintliga kärndomänerna i AutoCore.
* **Mätmetod & Kriterium:**
  Ett samlat verifieringstest genomför konkreta operationer och assertions mot samtliga 9 områden:
  1. **Kunder:** Skapa, hämta, uppdatera kontaktuppgifter & validera VIP-flagg.
  2. **Fordon:** Registrera fordon med registreringsnummer och verifiera koppling till ägare.
  3. **Bokningar:** Skapa flertjänstbokning och verifiera tidsberäkning (135 min) och kostnad.
  4. **Mekaniker:** Schemastatus, kompetensområde och tillgänglighetslåsning.
  5. **Arbetsordrar:** Livscykelövergång `CREATED` -> `IN_PROGRESS` -> `COMPLETED`.
  6. **Fakturering:** Raduppdelning, historisk prisfrysning och beloppsberäkning.
  7. **Betalningar:** Registrera korttransaktion, kvittoutfärdande och matchning mot fakturasumma.
  8. **Rabatter:** VIP 10%, WELCOME10, SERVICE200 samt golv vid 0.00 kr.
  9. **Flerspråkighet (I18n):** Svenska och engelska termer laddas och matchar 100%.
  - **Godkänt mätvärde:** Samtliga 9 deltester rapporterar `OK` med 0 felaktiga assertions.
* **Implementation:** `EvidenceVerificationTest.testScrum172NineCoreAreasVerified`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum172NineCoreAreasVerified`
* **Jira-spårbarhet:** SCRUM-171 (G1 Beviskort), SCRUM-172 (G2 Beviskort)
* **Status:** GODKÄND (100%)

---

## 3. Kompletterande Kvalitets-, Säkerhets- och Tillgänglighetskrav

### AK-13: Fakturering enbart av utförda arbeten (C3 & Demonstrationsflöde)
* **Krav:** Om en kund bokar flera tjänster, men mekanikern av någon anledning endast slutför en delmängd av dem, ska enbart de *utförda* tjänsterna debiteras på fakturan. Ej utförda moment ska varken faktureras eller debiteras kunden.
* **Mätmetod & Kriterium:**
  - Boka 3 tjänster: *Oljeservice (1 295 kr)*, *Bromsservice (2 495 kr)* och *Diagnostik (995 kr)*.
  - Markera Oljeservice och Bromsservice som `completed=1`, medan Diagnostik förblir outförd (`completed=0`).
  - Generera faktura med rabattkod `WELCOME10`.
  - **Godkänt mätvärde:**
    - Antal fakturarader == 2.
    - Fakturan innehåller endast Oljeservice och Bromsservice.
    - Fakturans delsumma == 3 790.00 kr (1295 + 2495). Diagnostik (995 kr) finns EJ med.
    - Rabatt (10%) == 379.00 kr -> Slutbelopp == 3 411.00 kr.
* **Implementation:** `work_order_service_items.completed`, `WorkOrder.markServiceAsCompleted`, `BillingService.createInvoice`.
* **Kopplat testfall:** `EvidenceVerificationTest.testScrum173PresentationEndToEndFlow`
* **Jira-spårbarhet:** SCRUM-173 (G3 Beviskort), C3/WorkOrder
* **Status:** GODKÄND (100%)

---

### AK-14: Språkparitet och Kodkvalitet (I18n & Arkitektur)
* **Krav:** 
  1. 100% paritet mellan språkfilerna `sv.json` och `en.json` (varje nyckel på svenska måste ha en motsvarighet på engelska och vice versa).
  2. Noll mojibake (teckenkodningsfel för å, ä, ö, é) och noll tomma översättningssträngar.
  3. Strikt separation mellan servicelager och GUI (servicelagret får inte importera JavaFX- eller presentationspaket).
  4. Högst 1 200 rader per Java-källkodsfil (det håller koden modulär och stoppar monolitiska "God Objects"; systemets största GUI-komponent `MechanicKanbanCard.java` ligger på 1 022 rader).
* **Mätmetod & Kriterium:**
  - Kör `CodeQualityTest`.
  - **Godkänt mätvärde:** Saknade nycklar == 0, mojibake == 0, felaktiga GUI-beroenden i servicelagret == 0, max filrader <= 1 200.
* **Kopplat testfall:** `CodeQualityTest` (4 tester)
* **Status:** GODKÄND (100%)

---

### AK-15: Säkerhetsanalys (SQL-injektion, Hemligheter & Exekvering)
* **Krav:**
  1. Noll SQL-injektionsrisker (all SQL mot SQLite ska använda parametriserade `PreparedStatement` med `?`).
  2. Inga hårdkodade lösenord, API-nycklar eller personnummer i källkoden.
  3. Ingen loggning av känsliga uppgifter i råtext till konsolen.
  4. Inga oskyddade externa processanrop i applikationskoden.
  5. `.gitignore` ska aktivt skydda personliga rapporter, analyser och hemligheter.
* **Mätmetod & Kriterium:**
  - Kör `SecurityAuditTest` (mönster i källkoden) och `DataFlowAuditTest` (dataflödesanalys på kompilatorns träd).
  - **Godkänt mätvärde:** 0 sårbarheter, 0 hårdkodade hemligheter funna.
* **Kopplat testfall:** `SecurityAuditTest` + `DataFlowAuditTest` (7 tester)
* **Status:** GODKÄND (100%)

---

### AK-16: WCAG 2.1 AAA Tillgänglighet (Kontrast, Fokus & Text)
* **Krav:**
  1. Textkontrast i det aktiva temat (*Emerald*) ska uppfylla WCAG 2.1 AAA-nivå med en kontrastkvot på minst **7.0:1** mot bakgrunden.
  2. Alla interaktiva kontroller ska ha tydliga fokusindikatorer.
  3. Ingen text i applikationen får vara mindre än 11 px (inga oläsliga mikrotexter).
* **Mätmetod & Kriterium:**
  - `WcagAccessibilityTest` beräknar den relativa luminansen för alla accent-, sidopanels- och kortfärger enligt W3C:s formel `(L1 + 0.05) / (L2 + 0.05)`.
  - **Godkänt mätvärde:** Kontrastkvot >= 7.00:1 för samtliga kontrollerade textelement.
* **Kopplat testfall:** `WcagAccessibilityTest` (5 tester)
* **Status:** GODKÄND (100%)

---

## 4. Spårbarhetsmatris (Acceptanskrav vs Testmodul vs Jira)

| Krav ID | Kravbeskrivning | Mätvärde / Bevis | Testklass & Metod | Jira-kort | Status |
|---|---|---|---|---|---|
| **AK-01** | Flertjänstbokning | 3 tjänster länkade & återlästa | `BookingServicesTest.testBookingWithMultipleServices` | SCRUM-147, 148, 151 | **GODKÄND** |
| **AK-02** | Låsning vid startat arbete | Exception vid modifikation i `IN_PROGRESS` | `BookingServicesTest.testCannotModifyServicesWhenWorkStarted` | SCRUM-150, 154 | **GODKÄND** |
| **AK-03** | Total tidsberäkning | 45 + 90 + 30 = 165 minuter | `BookingServicesTest.testEstimatedTimeAndCostCalculationsAndFormatting` | SCRUM-153 | **GODKÄND** |
| **AK-04** | Total kostnadsberäkning | 899 + 1495 + 399 = 2 793.00 kr | `BookingServicesTest.testEstimatedTimeAndCostCalculationsAndFormatting` | SCRUM-153 | **GODKÄND** |
| **AK-05** | Arbetsorder bär tjänster | Tjänste-IDs `[1, 2]` i arbetsorder | `EvidenceVerificationTest.testScrum169RestartEvidence` | SCRUM-156, 157, 158 | **GODKÄND** |
| **AK-06** | Flera fakturarader | 2 rader genererade med delbelopp | `InvoiceLineTest.testOneLinePerPerformedService` | SCRUM-162, 163 | **GODKÄND** |
| **AK-07** | Prisändring i katalog | Nya ordrar får nytt pris (1 099 kr) | `EvidenceVerificationTest.testScrum159PriceChangeControlledAllTheWay` | SCRUM-159 (D1) | **GODKÄND** |
| **AK-08** | Historisk prisfrysning | Gamla fakturor behåller 899 kr trots prishöjning | `EvidenceVerificationTest.testScrum161HistoricalPricesVisibleInUi` | SCRUM-160, 161 (D3) | **GODKÄND** |
| **AK-09** | Rabattregler (VIP, koder) | VIP 10%, WELCOME10, SERVICE200, min 0 kr | `EvidenceVerificationTest.testScrum165VipAndDiscountCodesWorkAsBefore` | SCRUM-165, 166 (E4) | **GODKÄND** |
| **AK-10** | Persistent databasrundtur | Full CRUD i SQLite för alla nya entiteter | `EvidenceVerificationTest.testScrum168RoundtripForNewEntities` | SCRUM-167, 168 (F2) | **GODKÄND** |
| **AK-11A** | Processomstartsbevis | Writer (PID X) -> Terminerar -> Verifier (PID Y) | `EvidenceVerificationTest.testScrum169RestartEvidence` | SCRUM-169 (F3) | **GODKÄND** |
| **AK-11B** | Noll dataförlust vid migrering | 8 tabeller: 100% radparitet före vs efter | `EvidenceVerificationTest.testScrum170ExistingDataRetained` | SCRUM-170 (F4) | **GODKÄND** |
| **AK-12** | 9 Kärnområden intakta | Kunder, Fordon, Bokning, Mekaniker, mm. OK | `EvidenceVerificationTest.testScrum172NineCoreAreasVerified` | SCRUM-171, 172 (G1, G2) | **GODKÄND** |
| **AK-13** | Debitera enbart utfört arbete | 3 beställda -> 2 utförda -> 2 på faktura | `EvidenceVerificationTest.testScrum173PresentationEndToEndFlow` | SCRUM-173 (G3), C3 | **GODKÄND** |
| **AK-14** | Språkparitet & Arkitektur | 0 saknade nycklar, 0 mojibake, ren arkitektur | `CodeQualityTest` (4 tester) | Kvalitetskrav | **GODKÄND** |
| **AK-15** | Säkerhet & Injektionsskydd | 0 sårbarheter, 100% PreparedStatement | `SecurityAuditTest` + `DataFlowAuditTest` (7 tester) | Säkerhetskrav | **GODKÄND** |
| **AK-16** | WCAG 2.1 AAA Tillgänglighet | Kontrast >= 7.0:1, fokusindikatorer, min 11px | `WcagAccessibilityTest` (5 tester) | WCAG AAA | **GODKÄND** |

---

## 5. Så kör och mäter du kraven själv

Så här kör du allt och får en automatisk rapport i [`rapport.md`](rapport.md):

```bash
# På Linux / macOS (--all kör både appens egna tester och granskningarna):
./test.sh --all

# På Windows (PowerShell):
.\test.ps1

# På Windows (Kommandotolken):
test.bat
```

Vill du bara köra acceptanskraven och beviskorten:
```bash
./test.sh bevis
```

Alla 115 tester körs automatiskt och verifierar varje mätpunkt. Du behöver varken externa verktyg eller Jira-inloggning.

---

## 6. Lägga till ett nytt acceptanskrav

Sviten hämtar kraven ur den här filen, så ett nytt krav behöver bara tre saker:

1. **Kravet:** lägg en ny rubrik `### AK-17: <namn>` i avsnitt 2 med mätmetod och godkänt mätvärde.
2. **Testet:** skriv en publik metod `testXxx()` i valfri `*Test`-klass under `com/wac/autocore/test/`.
   `TestRunner` hittar klasserna själv, så ingen lista behöver uppdateras. Nämns en klass inte i
   `TestRunner.GROUP_OF` körs den i modulen `Enhetstester`.
3. **Raden:** lägg kravet i spårbarhetsmatrisen i avsnitt 4 med exakt `KlassNamn.metodNamn` i testkolumnen.

Kör sedan `./test.sh` (Windows: `.\test.ps1`). Då gäller:

* varje hänvisning i matrisen måste finnas som testmetod, annars blir körningen röd (`BEVISKOPPLING`),
* rapportens kravtabell fylls på från matrisen, så nya krav skrivs inte in i skriptet,
* antalet tester och statusen i rapporten kommer från de moduler som faktiskt kördes.
