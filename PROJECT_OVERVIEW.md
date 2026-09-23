# Projektöversikt & Systemarkitektur: Wigell AutoCore

Detta dokument ger en fullständig, uppdaterad genomgång av projektet **Wigell AutoCore**, dess arkitektur, katalogstruktur, domänmodeller, servicelager, UI-komponenter, internationella flerspråksmotor samt det omfattande test- och auditsystemet. 

Dokumentet fungerar som teknisk referens och arkitekturhandledning för utvecklingsteamet (**Grupp C: Alex, Lucas, Daniel, Vivianne**).

---

## Innehållsförteckning

1. [Övergripande syfte & Domän](#1-övergripande-syfte--domän)
2. [Aktuell fil- och katalogstruktur](#2-aktuell-fil--och-katalogstruktur)
3. [Detaljerad komponent- och filgenomgång](#3-detaljerad-komponent--och-filgenomgång)
   - 3.1 Startpunkter & Presentation (JavaFX & CLI)
   - 3.2 Fasad & Servicelager (Facade Pattern)
   - 3.3 Internationell Flerspråksmotor (I18n)
   - 3.4 Datalager & Databasintegration (SQLite)
   - 3.5 Domänmodeller
4. [Arkitektonisk analys: Från monolit till skiktad arkitektur](#4-arkitektonisk-analys-från-monolit-till-skiktad-arkitektur)
5. [Implementerade Design Patterns (GoF)](#5-implementerade-design-patterns-gof)
6. [Kvalitetssäkring, Säkerhetsanalys & WCAG 2.1 AAA](#6-kvalitetssäkring-säkerhetsanalys--wcag-21-aaa)
7. [Färdplan, Arbetsfördelning & Git-rutiner](#7-färdplan-arbetsfördelning--git-rutiner)

---

## 1. Övergripande syfte & Domän

**Wigell AutoCore** är ett affärs- och verkstadssystem (Core ERP / Garage Management System) utvecklat för koncernen *Wigell Group*. Systemet hanterar den dagliga operativa verksamheten på en bilverkstad med fokus på hög driftsäkerhet, tydlig separation of concerns, modern tillgänglighet (WCAG 2.1 AAA) och realtidsstöd för flera språk.

### Kärnfunktioner i domänen:
* **Kunder & Fordon:** Registrering, uppslagning och koppling av ägare till fordon. Full validering vid inmatning.
* **Bokningar (Bookings):** Tidsbokning av fordon för service, felsökning eller reparation på angivna datum.
* **Mekaniker & Schemaläggning:** Register över mekaniker, specialiteter, tillgänglighet samt en timme-för-timme schemaläggningsmotor (`MechanicSchedule`) med visuell belastningsprogression.
* **Priskatalog & Tjänster (ServiceItems):** Standardiserade verkstadsmoment (oljebyte, bromsbyte, hjulinställning, etc.) med fasta baspriser och estimerad tidsåtgång.
* **Arbetsordrar (Work Orders):** Konvertering av bokningar till skarpa arbetsordrar med tilldelad mekaniker och valda tjänster. Följer en kontrollerad livscykel: `CREATED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`.
* **Fakturering & Rabattsystem:** Automatisk generering av fakturor vid avslutad arbetsorder. Beräkning av subtotal, moms, VIP-rabatter (10%) och kampanjkoder (`WELCOME10`, `SERVICE200`).
* **Betalningar:** Registrering av transaktioner via kort (`CARD`), Swish (`SWISH`) och kontant (`CASH`). Kvittohistorik och markering av betalstatus.

---

## 2. Aktuell fil- och katalogstruktur

Projektet är strukturerat enligt ren skiktad arkitektur (**Layered Architecture**) under `WigellAutoCore/autocore/`:

```
Systemarkitektur/
├── check.sh                                  # Automatiserad audit-runner (50 kontroller i färg)
├── test.sh                                   # Snabbkörningsskript för enhetstester
├── run.sh                                    # Körskript för GUI och CLI
├── README.md                                 # Projekt-README med snabbstart och modulöversikt
├── PROJECT_OVERVIEW.md                       # Detta arkitektur- och översiktsdokument
├── STYLEGUIDE.html                           # Interaktiv webbstyleguide med live-komponenter
│
└── WigellAutoCore/
    └── autocore/
        ├── lib/
        │   └── sqlite-jdbc-3.53.4.0.jar      # JDBC-drivrutin för lokal SQLite-persistens
        │
        ├── src/
        │   ├── Main.java                     # Startpunkt: AutoCore modernt JavaFX-gränssnitt
        │   ├── ConsoleApp.java               # Startpunkt: Textbaserat terminalgränssnitt (CLI)
        │   ├── DesignSelectorApp.java        # Visuell väljare för designprototyper
        │   │
        │   └── com/wac/autocore/
        │       ├── data/
        │       │   └── Database.java         # Datalager & seed data (förberett för SQLite)
        │       │
        │       ├── model/                    # Domänentiteter (POJO / Beans)
        │       │   ├── Customer.java         # Kunduppgifter & VIP-status
        │       │   ├── Vehicle.java          # Fordonsdata kopplat till kund
        │       │   ├── Booking.java          # Tidsbokning och status
        │       │   ├── Mechanic.java         # Mekaniker, specialisering och tillgänglighet
        │       │   ├── ServiceItem.java      # Priskatalog/arbetsmoment med tidsestimat
        │       │   ├── WorkOrder.java        # Arbetsorder med tilldelad mekaniker & tjänster
        │       │   ├── Invoice.java          # Fakturaunderlag, rabatter och moms
        │       │   └── Payment.java          # Transaktionslogg och betalmetod
        │       │
        │       ├── service/                  # Affärs- och domänservicelager
        │       │   ├── GarageSystem.java     # Huvudfasad (Facade Pattern) mot alla delsystem
        │       │   ├── CustomerService.java  # Kundvalidering & sökning
        │       │   ├── VehicleService.java   # Fordonsregistrering & ägarkoppling
        │       │   ├── BookingService.java   # Tidsbokning & validering
        │       │   ├── WorkOrderService.java # Arbetsorderns tillstånd och mekanikerlåsning
        │       │   ├── BillingService.java   # Fakturaberäkning, rabatter & momshantering
        │       │   ├── PaymentService.java   # Betalningstransaktioner (Kort, Swish, Kontant)
        │       │   └── MechanicSchedule.java # Timme-för-timme schema & dubbelbokningsskydd
        │       │
        │       ├── theme/                    # Temamotor & stildefinitioner
        │       │   ├── ThemeManager.java     # Hanterar aktivt tema och CSS-laddning
        │       │   └── ThemeCatalog.java     # Katalog över appens officiella tema (Emerald)
        │       │
        │       ├── ui/                       # Presentationslager (JavaFX GUI & CLI)
        │       │   ├── AutoCoreApp.java      # Huvudfönster, layout och övergripande ramverk
        │       │   ├── ActionDialogs.java    # Modala formulär och dialoger för CRUD
        │       │   ├── ConsolePrinter.java   # Frikopplad formaterad utskriftsmotor för CLI
        │       │   ├── components/           # Återanvändbara gränssnittskomponenter
        │       │   │   ├── UiComponents.java # Kort, badges, knappar och varningsrutor
        │       │   │   ├── TableFactory.java # Tabellbyggare med filter & zebramönster
        │       │   │   └── MechanicKanbanCard.java # Kanban-kort för mekanikerschema
        │       │   ├── i18n/                 # Flerspråksstöd
        │       │   │   └── I18n.java         # Dynamisk realtidsöversättningsmotor
        │       │   ├── navigation/           # Navigationsstruktur
        │       │   │   ├── SidebarView.java  # Luftig sidomeny med sektioner och språkknapp
        │       │   │   └── PageRouter.java   # Sidrouter med händelselyssnare
        │       │   ├── util/                 # Gränssnittshjälpare
        │       │   │   ├── UiFormatters.java # Valuta, datum, statusord och badge-mappning
        │       │   │   ├── EntityLookup.java # Uppslagning av namn mot ID:n via GarageSystem
        │       │   │   └── GlobalSearch.java # Systemomfattande granulär sökning
        │       │   └── views/                # Sidspecifika vyer
        │       │       ├── OverviewView.java # Dashboard med KPI:er och snabböversikt
        │       │       └── EntityPages.java  # Vyer för Kunder, Fordon, Ordrar, etc.
        │       │
        │       └── test/                     # Komplett automatiserad test- och auditsvit
        │           ├── TestRunner.java       # Fristående testmotor (körs utan externa ramverk)
        │           ├── EntityLookupTest.java # Tester för relations- och ID-uppslagning
        │           ├── GlobalSearchTest.java # Tester för granulär sökning och prefix
        │           ├── I18nTest.java         # Tester för realtidsöversättning och paritet
        │           ├── MechanicScheduleTest.java # Tester för schemaläggning och kapacitet
        │           ├── OverviewMetricsTest.java # Tester för KPI-beräkningar och intäkter
        │           ├── TableFactoryTest.java # Tester för tabellfilter och indexintegritet
        │           ├── UiFormattersTest.java # Tester för valuta, datum och statusord
        │           ├── CodeQualityTest.java  # Kodkvalitet, arkitekturgränser & språkparitet
        │           ├── SecurityAuditTest.java# Sårbarhetsscanning (SQLi, hemligheter, PII)
        │           └── WcagAccessibilityTest.java # WCAG 2.1 AAA kontrast- och tillgänglighetstest
        │
        └── resources/
            └── com/wac/autocore/
                ├── i18n/
                │   ├── sv.json               # Svensk språkordbok (396 nycklar)
                │   └── en.json               # Engelsk språkordbok (396 nycklar)
                └── theme/
                    ├── components.css        # Återanvändbara komponent- och layoutstilar
                    └── themes/emerald/
                        └── emerald.css       # Officiellt tema: modernt mörkgrönt verkstadstema
```

---

## 3. Detaljerad komponent- och filgenomgång

### 3.1 Startpunkter & Presentation (JavaFX & CLI)

1. **`Main.java` (JavaFX GUI):**
   - Startar det moderna AutoCore-skrivbordsgränssnittet via `AutoCoreApp`.
   - Initialiserar `ThemeManager` (standard: `emerald`) och `I18n` (standard: svenska, snabbt växlingsbar).
   - Tillhandahåller responsiv sidonavigation (`SidebarView`), global sökning (`SearchResultsView`) samt modala transaktionsdialoger (`ActionDialogs`).
2. **`ConsoleApp.java` (Textbaserat CLI):**
   - Erbjuder full funktionalitet för terminalanvändare och automatiserad drift.
   - Kör en 17-vals meny för fullständig hantering av verkstadens flöden.
   - Använder uteslutande `ConsolePrinter` för formatering och delegerar all logik till fasaden `GarageSystem`.
3. **`ConsolePrinter.java`:**
   - Separerar all utskriftsformatering (ANSI-boxar, tabeller, statuskoder) från affärslogiken. Servicelagret är helt befriat från `System.out.println`.

### 3.2 Fasad & Servicelager (Facade Pattern)

Systemet tillämpar **Facade Pattern** genom klassen `com.wac.autocore.service.GarageSystem`:
- **`GarageSystem`** exponerar ett enhetligt och förenklat API för både GUI och CLI.
- Bakom fasaden delegeras alla anrop till specialiserade domäntjänster med enskilt ansvar (**Single Responsibility Principle**):
  - **`CustomerService`:** Registrering, sökning och validering av kunduppgifter och VIP-status.
  - **`VehicleService`:** Koppling av registreringsnummer, bilmodell och ägare. Validerar att kund existerar.
  - **`BookingService`:** Skapande av tidsbokningar, statusövergångar och fordonsvalidering.
  - **`WorkOrderService`:** Ansvarar för arbetsorderns tillståndsmaskin (`CREATED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`). Låser mekanikerns tillgänglighet när order påbörjas och frigör mekanikern när ordern avslutas.
  - **`BillingService`:** Genererar fakturor baserat på utförda tjänster, beräknar 10% VIP-rabatt samt hanterar kampanjkoder (`WELCOME10` ger 10%, `SERVICE200` drar av 200 kr).
  - **`PaymentService`:** Hanterar betalningstransaktioner med validering av betalmetod (`CARD`, `SWISH`, `CASH`), sätter fakturan som betald och sparar transaktionen.
  - **`MechanicSchedule`:** Avancerad schemaläggningsmotor som beräknar mekanikers arbetsbelastning timme-för-timme, förhindrar dubbelbokningar och genererar visuell färgprogression för Kanban-korten.

### 3.3 Internationell Flerspråksmotor (I18n)

- **Klass:** `com.wac.autocore.ui.i18n.I18n`
- **Resursfiler:** `resources/com/wac/autocore/i18n/sv.json` och `en.json`.
- **Egenskaper:**
  - **100 % nyckelparitet:** Båda filerna innehåller exakt 396 språknycklar (verifieras automatiskt i `CodeQualityTest` och `I18nTest`).
  - **Realtidsväxling utan omstart:** Gränssnittet uppdateras dynamiskt i realtid via lyssnare (`I18n.addListener()`) när användaren klickar på språkknappen i sidomenyn.
  - **Parameterstöd:** Metoden `I18n.t("key", arg1, arg2)` ersätter dynamiska parametrar (`{0}`, `{1}`) direkt.
  - **Robust felhantering:** Vid saknad nyckel returneras nyckeln själv i fallback utan att applikationen kraschar.

### 3.4 Datalager & Databasintegration (SQLite)

- **Nuläge:** `Database.java` hanterar in-memory-samlingar med standardiserad testdata för kunder, bilar, mekaniker, ordrar och fakturor.
- **Pågående databasarbete:** 
  - JDBC-drivrutinen `sqlite-jdbc-3.53.4.0.jar` ligger integrerad under `lib/`.
  - Daniel i teamet leder implementeringen av SQLite-lagret för att flytta systemet från in-memory-listor till persistent lokal lagring.
  - Befintliga servicelager och fasaden är konstruerade så att databasskiftet kan ske transparent utan att påverka presentationslagret.

### 3.5 Domänmodeller

Alla modeller är rena, välkapslade klasser under `com.wac.autocore.model`:
- **`Customer`:** `id`, `name`, `phone`, `email`, `vip`
- **`Vehicle`:** `id`, `registrationNumber`, `brand`, `model`, `year`, `customerId`
- **`Booking`:** `id`, `vehicleId`, `date`, `description`, `status`
- **`Mechanic`:** `id`, `name`, `phone`, `specialization`, `available`
- **`ServiceItem`:** `id`, `name`, `description`, `price`, `estimatedMinutes`
- **`WorkOrder`:** `id`, `bookingId`, `mechanicId`, `serviceItemIds` (`List<Integer>`), `status`
- **`Invoice`:** `id`, `workOrderId`, `invoiceDate`, `amount`, `discount`, `totalAmount`, `paid`
- **`Payment`:** `id`, `invoiceId`, `amount`, `paymentType`, `paymentDate`, `successful`

---

## 4. Arkitektonisk analys: Från monolit till skiktad arkitektur

Vid projektets start innehöll koden flera typiska "Code Smells" som nu har refaktorerats:

| Ursprunglig brist (Sprint 1) | Genomförd åtgärd & Ny arkitektur |
| :--- | :--- |
| **Monolitisk "God Class"** (`GarageSystem` hade 400+ rader och skötte allt från utskrifter till rabattregler). | **Fasadmönstret implementerat:** `GarageSystem` delegerar nu till 7 specialiserade tjänster (`BillingService`, `CustomerService`, etc.). |
| **UI sammanflätat med affärslogik** (`System.out.println` spridda i domänmetoder). | **Skiktseparation:** All utskriftslogik flyttad till `ConsolePrinter`. Alla servicemetoder returnerar rena domänobjekt eller felkoder. |
| **Hårdkodad och duplicerad sökning** i linjära for-loopar. | **Granulär sökmotor:** `GlobalSearch` och `EntityLookup` indexerar och söker över alla entiteter med prefixstöd och skiftlägesokänslighet. |
| **Frånvaro av tester:** Inga automatiserade tester existerade i startpaketet. | **Omfattande test- och audit-svit:** 50 automatiserade tester för enhet, kvalitet, säkerhet och WCAG 2.1 AAA med 100% pass rate. |
| **Hårdkodat språk och texter:** Svenska texter hårdkodade i Java-strängar. | **Full I18n-motor:** Extern ordbok i JSON med 396 nycklar och momentan språkväxling mellan svenska och engelska. |
| **Ingen tillgänglighetsstandard:** Konsolfönster utan kontrastkrav. | **WCAG 2.1 AAA certifiering:** Färgkontrast $\ge 7.0:1$ för all löpande text, tangentbordsfokus (`:focused`), zebramönstrade tabeller och dynamisk layout. |

---

## 5. Implementerade Design Patterns (GoF)

1. **Facade Pattern (`GarageSystem`):**
   - Ger en förenklad och sammanhållen ingång till de underliggande delsystemen: bokning, arbete, fakturering och betalning. Både GUI och CLI anropar fasaden utan kännedom om delsystemens interna beroenden.
2. **Layered Architecture (Skiktad arkitektur):**
   - Tydlig separation mellan Presentationslager $\rightarrow$ Fasad $\rightarrow$ Servicelager $\rightarrow$ Domänmodeller $\rightarrow$ Datalager.
3. **Observer Pattern (Händelselyssnare):**
   - `I18n.addListener()`: Registrerar vyer och komponenter som automatiskt uppdaterar sina texter när språket växlas i realtid.
   - `PageRouter`: Notifierar navigationsmenyn vid sidbyten så att rätt sida ritas ut och aktiveras.
4. **Factory Pattern (`TableFactory`):**
   - Centraliserar skapandet av JavaFX `TableView` med inbyggd sökfiltrering, sortering, zebramönstrade rader (`.table-row-cell:odd / :even`) och anpassade cellformatters.
5. **Strategy / Specialiserade Beräkningstjänster:**
   - Rabattlogik och betalningsvalidering är isolerade i `BillingService` respektive `PaymentService`, vilket gör det enkelt att addera nya kampanjregler eller betalsätt utan att ändra fasad eller GUI.

---

## 6. Kvalitetssäkring, Säkerhetsanalys & WCAG 2.1 AAA

Hela systemet kvalitetssäkras med det automatiska verifieringsskriptet `./check.sh` (eller `./audit.sh`). Skriptet exekverar 50 kontroller fördelade på 4 moduler:

```bash
./check.sh          # Kör hela audit-sviten (Alla 4 moduler)
./check.sh --unit   # Endast enhetstester (37 tester)
./check.sh --wcag   # Endast WCAG 2.1 AAA tillgänglighet (5 tester)
```

### De fyra modulerna:

1. **Modul 1: Enhetstester (37 tester – 100% godkända)**
   - `UiFormattersTest` (8 tester): Valutaformatering, datum, trunkering och CSS-badgeklasser.
   - `EntityLookupTest` (4 tester): ID-till-namn-uppslagning för kund, fordon, mekaniker och tjänster.
   - `OverviewMetricsTest` (3 tester): KPI-mätetal (aktiva ordrar, omsättning, lediga mekaniker).
   - `TableFactoryTest` (2 tester): Filtrering och regressionsskydd mot indexförskjutningar.
   - `GlobalSearchTest` (8 tester): Granulär sökning, prefix, skiftläge och cross-entity-matchning.
   - `I18nTest` (7 tester): Växling, engelska/svenska, fallback, parameterersättning och nyckelparitet.
   - `MechanicScheduleTest` (5 tester): Timme-för-timme slots, färgprogression och skydd mot dubbelbokningar.
2. **Modul 2: Kodkvalitet & Arkitektur (4 kontroller – 100% godkända)**
   - Språkordböckernas integritet och 100% nyckelparitet (396 nycklar).
   - Temaintegritet för det officiella temat i `ThemeCatalog`.
   - Frikoppling av servicelager från GUI-beroenden.
   - Maxgränser för källkodsfilers komplexitet samt 0 aktiva TODO/FIXME-noteringar.
3. **Modul 3: Säkerhetsgranskning (4 kontroller – 100% godkända)**
   - Scanning mot hårdkodade lösenord, tokens och hemligheter.
   - SQL-injektionsskydd: Verifierar att framtida SQL-anrop är förberedda för parameterized queries (`PreparedStatement`).
   - Förbud mot farliga processkörningar (`Runtime.getRuntime().exec`).
   - Skydd mot loggning av känsliga personuppgifter (PII).
4. **Modul 4: WCAG 2.1 AAA Tillgänglighet (5 kontroller – 100% godkända)**
   - Färgkontrast på text och dämpad text mot kort- och sidbakgrunder $\ge 7.0:1$ (WCAG 1.4.6 Contrast Enhanced Level AAA: upp till 16.5:1).
   - Färgkontrast på accentknappar $\ge 7.0:1$ (8.37:1 i temat Emerald).
   - Färgkontrast i sidonavigationen $\ge 7.0:1$ (12.86:1 aktiv text, 8.00:1 dämpad text).
   - Tydliga fokusindikatorer (`:focused`) på alla interaktiva kontroller (WCAG 2.4.7).
   - Minsta tillåtna teckenstorlek ($\ge 11$ px på all löpande text, WCAG 1.4.4).

---

## 7. Färdplan, Arbetsfördelning & Git-rutiner

### Status & Ansvarsområden i Grupp C:

| Gruppmedlem | Huvudfokus & Arbetsområde | Aktuell status |
| :--- | :--- | :--- |
| **Alex** | Systemarkitektur, Fasad, I18n flerspråksmotor, Test- & Auditsvit (`check.sh`) | **Klart & Integrerat i develop** |
| **Daniel** | Databasintegration (SQLite-persistens via `lib/sqlite-jdbc-...`) | **Pågående arbete** |
| **Lucas** | Domänmodeller, affärsregler för ordrar och bokningsflöden | **Klart & Integrerat i develop** |
| **Vivianne** | JavaFX GUI-vyer, layout, styling, WCAG-anpassning & teman | **Klart & Integrerat i develop** |

### Git-rutiner & Branch-strategi:

> [!IMPORTANT]
> **Strikt regel gällande brancher:**  
> Allt aktivt arbete, integration och feature-merger ska ske uteslutande mot **`develop`**.  
> **`main` är reserverad för slutlig produktionsrelease och får INTE pushas till under pågående sprint.**

#### Så synkar du ditt arbete:
```bash
# Se till att stå på develop
git checkout develop

# Hämta in det senaste från teamet
git pull origin develop

# Kör hela audit-kontrollen innan du pushar nya ändringar
./check.sh
```

När alla 50 kontroller är gröna kan koden säkert pushas till `origin/develop`.
