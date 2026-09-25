# AutoCore

AutoCore (Wigell AutoCore) är ett affärs- och verkstadssystem (Core ERP / Garage Management System) utvecklat för koncernen Wigell Group.

## Översikt
Systemet hanterar den dagliga operativa verksamheten på en bilverkstad:
- **Kunder & Fordon:** Registrering och koppling av ägare till fordon.
- **Bokningar:** Tidsbokning för service och felsökning.
- **Mekaniker & Tjänster:** Register över mekaniker, specialiteter och priskatalog för verkstadstjänster.
- **Arbetsordrar:** Hantering av arbetsordrar, tilldelning av mekaniker och statusflöde.
- **Betalningar:** Betalningshantering via kort, Swish och kontant.

## Skärmbilder
JavaFX-gränssnittet (AutoCore Overview, tema *Emerald*):

![AutoCore JavaFX-gränssnitt](WigellAutoCore/docs/autocore-overview.png)

## Teknisk stack
- Java (JDK 8, BellSoft Liberica Full med JavaFX)
- JavaFX-gränssnitt (GUI) med modulära komponenter och WCAG 2.1 AAA designsystem
- Konsolgränssnitt (CLI)
- SQLite med JDBC (`sqlite-jdbc-3.53.4.0.jar`) för lokal persistens

## Kom igång & Synka med Develop

För att synka din lokala miljö med den gemensamma koden i `develop`:

```bash
# 1. Se till att du inte har osparade ändringar, växla sedan till develop
git checkout develop

# 2. Hämta och slå ihop alla senaste ändringar från teamet
git pull origin develop
```

> **Jobbar du i en egen feature-branch?**  
> Uppdatera din branch med det senaste från `develop`:
> ```bash
> git checkout din-feature-branch
> git merge develop
> ```

### Köra och testa applikationen

* **Huvudapplikationen (AutoCore GUI):** Kör `./start.sh` i terminalen, eller `Main.java` i IntelliJ
* **Total systemaudit (50 kontroller):** Kör `./check.sh` (eller `./audit.sh`) för en komplett rapport över enhetstester, kodkvalitet, säkerhet och WCAG 2.1 AAA.
* **Snabba enhetstester (57 tester):** Kör `./test.sh` (eller `com.wac.autocore.test.TestRunner`)
* **Konsolversionen (CLI):** Kör `./start.sh ConsoleApp` i terminalen, eller `ConsoleApp.java` i IntelliJ
* **Modulvisa testappar:**
  - **Kunder:** `com.wac.autocore.gui.customers.TestCustomer`
  - **Fordon:** `com.wac.autocore.gui.vehicle.VehicleTest`
  - **Bokningar:** `com.wac.autocore.gui.booking.TestBookingApp`
  - **Arbetsordrar:** `com.wac.autocore.gui.workorder.TestWorkOrderApp`
  - **Fakturor:** `com.wac.autocore.gui.invoice.TestInvoiceApp`
  - **Betalningar:** `com.wac.autocore.gui.payment.TestPaymentApp`
  - **Mekaniker:** `com.wac.autocore.gui.mechanic.TestMechanicApp`
  - **Tjänster:** `com.wac.autocore.gui.serviceItem.TestServiceItemApp`

> **Får du `No suitable driver found for jdbc:sqlite`?**
> Databasdrivrutinen ligger som ett projektberoende i `.idea/libraries/`, så den följer med i giten.
> Felet betyder i så fall att din IntelliJ kör med en gammal projektmodell: välj
> **File → Reload All from Disk**, eller stäng och öppna projektet igen.
> Går det ändå inte, lägg till `WigellAutoCore/autocore/lib/sqlite-jdbc-3.53.4.0.jar` manuellt via
> **File → Project Structure → Modules → Dependencies → `+` → JARs or directories**.
> `./start.sh` behöver inget av detta.

## Arkitektur & Modularisering

Systemet är uppbyggt enligt ren skiktad arkitektur (Layered Architecture) och SOLID-principerna:

```
WigellAutoCore/autocore/
├── lib/
│   └── sqlite-jdbc-3.53.4.0.jar      # Databasdrivrutin för lokal SQLite-persistens
├── src/
│   ├── Main.java                     # JavaFX GUI Startpunkt
│   ├── ConsoleApp.java               # Textbaserat CLI-gränssnitt
│   └── com/wac/autocore/
│       ├── data/
│       │   └── Database.java         # Datalager & seed data (SQLite-persistens)
│       ├── model/                    # Domänmodeller (Customer, Vehicle, Booking, etc.)
│       ├── service/                  # Servicelager & Fasad
│       │   ├── GarageSystem.java     # Huvudfasad (Facade Pattern) mot delsystem
│       │   ├── CustomerService.java  # Kundhantering & validering
│       │   ├── VehicleService.java   # Fordonsregistrering & ägarkoppling
│       │   ├── BookingService.java   # Tidsbokning & validering (strikt BOOKED)
│       │   ├── WorkOrderService.java # Arbetsorderns livscykel (CREATED -> IN_PROGRESS -> COMPLETED)
│       │   ├── BillingService.java   # Fakturaberäkning, rabatter & momshantering
│       │   ├── PaymentService.java   # Betalningstransaktioner (Kort, Swish, Kontant)
│       │   └── MechanicSchedule.java # Schemaläggning, krockanalys & beläggningsberäkning
│       ├── ui/                       # Presentationslager (JavaFX GUI)
│       │   ├── AutoCoreApp.java      # Fönsterram, layout & sidnavigation
│       │   ├── ActionDialogs.java    # Lättviktig fasad (Facade) för modala formulärdialoger
│       │   ├── CustomerDialogs.java  # Kunddialoger (skapa, redigera, ta bort)
│       │   ├── VehicleDialogs.java   # Fordonsdialoger med detaljerad fältvalidering
│       │   ├── BookingDialogs.java   # Bokningsdialoger (modulär dialogsamordnare)
│       │   ├── WorkOrderDialogs.java # Arbetsorderdialoger med tjänstekoppling
│       │   ├── BillingDialogs.java   # Faktura- och betalningsdialoger
│       │   ├── MechanicDialogs.java  # Mekanikerdialoger (specialisering, tillgänglighet)
│       │   ├── ServiceItemDialogs.java # Verkstadstjänster & prissättning (> 0 validering)
│       │   ├── SlotDetailsDialog.java # Detaljdialoger för schema- och arbetsordrar
│       │   ├── components/           # Återanvändbara UI-komponenter & formulärpaneler
│       │   │   ├── BookingFormPane.java   # Komposit formulär för tidsbokning
│       │   │   ├── TimeSlotCell.java      # Cell med färgkodad ledig/upptagen-indikator
│       │   │   ├── MechanicKanbanCard.java# Interaktivt Kanban-kort för mekanikerscheman
│       │   │   ├── TableFactory.java      # Typad tabellfabrik med flerkolumnsfiltrering
│       │   │   └── UiComponents.java      # Knappar, KPI-kort och standardiserade sidhuvuden
│       │   ├── i18n/                 # Flerspråksmotor (I18n.java) med realtidsväxling
│       │   ├── navigation/           # Sidomeny (SidebarView) & Sidrouter (PageRouter)
│       │   ├── util/                 # Formatering (UiFormatters), BookingAvailability, sök och uppslag
│       │   └── views/                # Översikt, Dashboard och entitetsvyer
│       └── test/                     # Automatiserad testsvit (57 tester)
└── resources/
    └── com/wac/autocore/
        ├── i18n/                     # Dictionaries (sv.json, en.json) med 100% paritet (358 nycklar)
        └── theme/                    # CSS-designsystem & officiellt tema (Emerald)
```

### Bokningsflöde & Frikoppling från Arbetsorder
* **Strikt separation mellan Bokning och Arbetsorder:** När en kund bokar en tid registreras bokningen renodlat som `BOOKED`. Systemet skapar inte längre en arbetsorder i förtid; arbetsordern startas först när fordonet faktiskt lämnas in på verkstaden via verkstadens Kanban-vy eller arbetsorderdialogen.
* **Visuell tidsvalidering i realtid:** I bokningsdialogen visualiseras mekanikerns tillgänglighet för varje timme via `TimeSlotCell`:
  - 🟢 **Grön punkt:** Tiden är ledig för bokning.
  - 🔴 **Röd punkt:** Tiden är redan upptagen av en annan bokning eller pågående arbetsorder.
* **Skydd mot dubbelbokningar:** `BookingAvailability` räknar automatiskt samman tjänstens tidsåtgång och förhindrar överlappande bokningar över flera timmar.
* **Avbokningssynkronisering:** När en bokning avbokas eller raderas frigörs mekanikerns tidsluckor omedelbart i schemat via `MechanicSchedule.cancelSlotForBooking`.

### Formulärvalidering & Förbättrad UX
* **Tydlig fältvalidering vid fordonsregistrering:** Om obligatoriska fält saknas meddelas användaren exakt vilket eller vilka fält som behöver fyllas i (registreringsnummer, märke, modell eller årsmodell), istället för svårbegripliga generiska nummerfel.
* **Rimlighetskontroll för årsmodell:** Årsmodeller valideras mot intervallet 1900–2100.
* **Distinkta platshållartexter:** Platshållare/prompttexter är formaterade som tydliga exempel (t.ex. `"T.ex. ABC123"`, `"T.ex. Volvo"`, `"T.ex. V60"`, `"T.ex. 2022"`) och har dämpad kontrastfärg (`-wac-muted`) för att förhindra förväxling med ifyllda fält.
* **Tjänstevalidering (SCRUM-104):** Både pris och beräknad tidsåtgång för verkstadstjänster valideras strikt och måste vara större än 0.

### Flerspråksstöd i realtid (SV / EN)
* **Realtidsväxling:** Växla sömlöst mellan svenska och engelska med switch-knappen i sidomenyn utan att behöva starta om applikationen.
* **100 % Nyckelparitet:** Både `sv.json` och `en.json` innehåller samtliga 358 språknycklar för menyer, dialoger, tabeller, statusar, valideringar och felmeddelanden.
* **Dynamisk formatering:** Datum formateras automatiskt på rätt språk (t.ex. *"Måndag 23 september 2026"* vs *"Monday 23 September 2026"*) och statusord mappas via `UiFormatters`.

### Ren Sidomenynavigering (SidebarView)
* **Permanent Sidebar:** Navigeringen är uteslutande placerad i den vänstra sidomenyn med tydlig sektionsindelning, mjuka hover-effekter och integrerad språkväxlingsknapp.
* **Granulär tabellsökning:** Samtliga entitetsvyer har integrerad filtrering och sökning via `TableFactory` med omedelbar filtrering över alla kolumner.

### Interaktiv Mekaniker-Kanban & Schemaläggning (MechanicKanbanCard)
Översiktspanelen (`OverviewView`) innehåller en fullt interaktiv, realtidsstyrd Kanban-tavla för verkstadens mekaniker:
* **Trelägesvyer (Dag, Vecka, Månad):**
  - **Dagsvy (07:00–16:00):** Visar 9 distinkta timboxar. Lediga tider har en klickbar SVG-plusknapp (`+`) som öppnar bokningsdialogen direkt förvald på mekanikern och vald timme.
  - **Inline Drawer:** Ett klick på en bokad timme fäller mjukt ut en inline detaljlåda under slotten med fordonets registreringsnummer, kundnamn och fullständig arbetsorderbeskrivning samt snabbval för att skapa arbetsorder.
  - **Veckovy & Beläggningsgrad:** 7-dagarsvy med färgkodad 4-stegs belastningsprogression:
    - 🟢 **Ledig (0–2 h):** Grön belastningsindikator.
    - 🟡 **Måttlig (3–4 h):** Gul belastningsindikator.
    - 🟠 **Hög (5–6 h):** Orange belastningsindikator.
    - 🔴 **Fullbokad (7+ h):** Röd belastningsindikator.
  - **Månadsvy:** Interaktiv månadskalender som visualiserar tjänstgöringsdagar och tillgänglighet för framtida bokningar.
* **Smart Snabbnavigering till nästa bokning:**
  - Om mekanikern saknar bokade timmar på vald dag visas en klickbar genväg (`📅 Nästa bokning: [Dag] [Datum] →`) som med ett klick hoppar direkt till nästa dag då mekanikern har ett inbokat arbete.
* **Kontextuell Kebabmeny (`⋮`):**
  - Diskret 3-prickars meny uppe till höger på varje mekanikerkort med alternativ för:
    - **Redigera mekaniker:** Uppdatera namn, specialisering och tillgänglighet via modal dialog.
    - **Ta bort mekaniker:** Raderar mekanikern ur systemet, skyddad av en säkerhetsspärr i `GarageSystem` som förhindrar borttagning om mekanikern har pågående aktiva arbetsordrar.
* **Realtidssynkronisering mot SQLite-databasen:**
  - `MechanicSchedule.syncFromDatabase()` läser automatiskt in aktiva arbetsordrar och bokningar från SQLite (`work_orders`, `bookings`, `vehicles`, `customers`) och mappar in dem i lediga timluckor. Nya bokningar syns direkt utan omstart.
* **Filtrering & Horisontell Navigering:**
  - Snabbfilter för mekanikernas specialiseringar (t.ex. *All*, *Motor*, *Bromsar*, *El & Diagnostik*) och horisontella rullningskontroller (`<`, `>`) som möjliggör smidig hantering av obegränsat antal mekaniker utan layout-hopp.
* **Plattforms- & Tillgänglighetsoptimerad:**
  - Vektorbaserade `SVGPath`-ikoner, standardiserade Unicode-pilar (`<`, `>`, `\u25BC`) och justerad typografi förhindrar avhuggna symboler och överlappande text på macOS. Fullt förenlig med WCAG 2.1 AAA.

### Automatiserade tester & Audit (`com.wac.autocore.test`)
Systemet skyddas av **57 automatiserade tester och 50 systemkontroller** samt automatisk GitHub Actions CI:
* **`GlobalSearchTest`**: Verifierar granulär sökning över kunder, fordon, mekaniker, ordrar, skiftlägesokänslighet och prefix.
* **`TableFactoryTest`**: Verifierar flerkolumnssökning och regressionsskyddar mot indexbuggar vid filtrering.
* **`UiFormattersTest`**: Valuta (long/double), trunkering, statusöversättning, datum och badge-CSS-klasser.
* **`EntityLookupTest`**: Uppslagning mot `GarageSystem` för kundnamn, fordonsreg, mekaniker, tjänster och kvalificeringskontroll.
* **`OverviewMetricsTest`**: Verifiering av KPI-mätetal (aktiva ordrar, omsättning, tillgänglighet).
* **`I18nTest`**: Språkväxling i realtid, parameteriserade strängar, fallback och komplett paritet mellan språkfiler.
* **`MechanicScheduleTest`**: Dagslots, veckobelastning, färgprogression, krockkontroller, avbokning och `getNextBookingDate`.
* **`PersistenceRestartTest`**: Säkerställer att sparade kunder och bokningar bevaras i SQLite och överlever app-omstart utan dubblering.
* **`CodeQualityTest`**: 100% språkparitet, temaintegritet, frikoppling av servicelager och komplexitetsgränser (< 1200 rader).
* **`SecurityAuditTest`**: Skanning mot hårdkodade hemligheter, SQL-injektionsmönster, processkörning och PII-loggning.
* **`WcagAccessibilityTest`**: WCAG 2.1 AAA kontrastmätningar (>= 7.0:1 för normal text, >= 4.5:1 för UI), fokusindikatorer och minsta teckenstorlek.
* **GitHub Actions CI (`.github/workflows/ci.yml`)**: Körs automatiskt vid varje push/PR med Liberica JDK 8 (med JavaFX) och virtuell framebuffer (`xvfb-run`).
* **Kör tester:**
  - `./check.sh` för komplett grafisk auditrapport (Alla 4 moduler, 50 kontroller).
  - `./test.sh` för snabb enhetstestkörning (57 tester).

### UI & Tillgänglighet (WCAG 2.1 AAA)
* **Zebramönstrade tabeller:** Varannan rad har dämpad kontrastfärg för snabbare och behagligare läsning.
* **Luftig och ren sidomeny:** Tydliga sektionsrubriker med 22 px avstånd och inga förvirrande dragspelsprickar.
* **Naturlig textvisning i schemat:** Kanban-kortens tidsrader expanderar naturligt och klipper endast med `…` när texten når kanten.
* **Färgtema (Emerald - Level AAA):** Designsystemet är låst till det officiella temat **`emerald`** med skarp grafitgrå list på tabeller och ultrahög kontrast (7.0:1 till 16.5:1) som uppfyller WCAG 2.1 Level AAA.
* **Tangentbordsfokus (WCAG 2.4.7):** Tydliga `:focused`-stilar och fokusringar på alla interaktiva kontroller.

## Design & Styleguide
Projektets visuella riktlinjer, komponentbibliotek och färgteman finns sammanställda i den interaktiva styleguiden:
- [STYLEGUIDE.html](STYLEGUIDE.html) (öppnas i valfri webbläsare för live-förhandsgranskning och tematester)

## Utvecklingsteam
Grupp C: Alex, Lucas, Daniel, Vivianne

