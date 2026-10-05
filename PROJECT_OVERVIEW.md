# Projektöversikt & Systemarkitektur: Wigell AutoCore

Här får du en fullständig och uppdaterad genomgång av **Wigell AutoCore**: arkitekturen, katalogstrukturen, domänmodellerna, servicelagret, UI-komponenterna, flerspråksmotorn och hela test- och auditsystemet. 

Dokumentet är tänkt som teknisk referens och handledning i arkitekturen för oss i utvecklingsteamet (**Grupp C: Alex, Lucas, Daniel**).

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

**Wigell AutoCore** är ett affärs- och verkstadssystem (Core ERP / Garage Management System) som vi bygger för koncernen *Wigell Group*. Det sköter den dagliga driften på en bilverkstad och lägger vikten vid hög driftsäkerhet, tydlig separation of concerns, modern tillgänglighet (WCAG 2.1 AAA) och stöd för flera språk i realtid.

### Kärnfunktioner i domänen:
* **Kunder & Fordon:** Registrera och slå upp kunder, och koppla ägare till fordon. Allt valideras vid inmatning.
* **Bokningar (Bookings):** Boka in ett fordon på service, felsökning eller reparation ett visst datum.
* **Mekaniker & Schemaläggning:** Håll koll på mekaniker, deras specialiteter och tillgänglighet, och låt schemaläggningsmotorn (`MechanicSchedule`) jobba timme för timme med visuell belastningsprogression.
* **Priskatalog & Tjänster (ServiceItems):** Standardiserade verkstadsmoment (oljebyte, bromsbyte, hjulinställning, etc.) med fasta baspriser och uppskattad tidsåtgång.
* **Arbetsordrar (Work Orders):** Gör om bokningar till skarpa arbetsordrar med tilldelad mekaniker och valda tjänster. De följer en kontrollerad livscykel: `CREATED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`.
* **Fakturering & Rabattsystem:** Fakturan skapas automatiskt när en arbetsorder avslutas, med subtotal, moms, VIP-rabatt (10%) och kampanjkoder (`WELCOME10`, `SERVICE200`).
* **Betalningar:** Registrera transaktioner via kort (`CARD`), Swish (`SWISH`) eller kontant (`CASH`), med kvittohistorik och markering av betalstatus.

---

## 2. Aktuell fil- och katalogstruktur

Projektet följer en ren skiktad arkitektur (**Layered Architecture**) under `WigellAutoCore/autocore/`:

```
Systemarkitektur/
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
        │       ├── config/
        │       │   └── FeatureFlags.java     # Funktionsväxlar från config/features.properties
        │       │
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
        │       │   │   ├── SearchDropdown.java # Granulär sökdropdown med kategoriserade träffar
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
        │
        └── resources/
            └── com/wac/autocore/
                ├── config/
                │   └── features.properties   # Av/på-växlar för utveckling (globalSearch)
                ├── i18n/
                │   ├── sv.json               # Svensk språkordbok (453 nycklar)
                │   └── en.json               # Engelsk språkordbok (453 nycklar)
                └── theme/
                    ├── components.css        # Återanvändbara komponent- och layoutstilar
                    └── themes/emerald/
                        └── emerald.css       # Officiellt tema: modernt mörkgrönt verkstadstema
```

---

## 3. Detaljerad komponent- och filgenomgång

### 3.1 Startpunkter & Presentation (JavaFX & CLI)

1. **`Main.java` (JavaFX GUI):**
   - Startar det moderna skrivbordsgränssnittet via `AutoCoreApp`.
   - Drar igång `ThemeManager` (standard: `emerald`) och `I18n` (standard: svenska, snabbt att växla).
   - Ger dig responsiv sidonavigation (`SidebarView`), granulär sökning till höger i sidhuvudet — i samma rad som sidtiteln (`SearchDropdown` under fältet, `SearchResultsView` för hela vyn) — och modala transaktionsdialoger (`ActionDialogs`). Sökfunktionen slås av och på med växeln `globalSearch` i `config/features.properties`.
2. **`ConsoleApp.java` (Textbaserat CLI):**
   - Ger terminalanvändare och automatiserad drift full funktionalitet.
   - Har en 17-vals meny som täcker hela verkstadens flöde.
   - Använder bara `ConsolePrinter` för formatering och skickar all logik vidare till fasaden `GarageSystem`.
3. **`ConsolePrinter.java`:**
   - Sköter all utskriftsformatering (ANSI-boxar, tabeller, statuskoder) så att affärslogiken slipper den. Servicelagret innehåller inte en enda `System.out.println`.

### 3.2 Fasad & Servicelager (Facade Pattern)

Systemet använder **Facade Pattern** genom klassen `com.wac.autocore.service.GarageSystem`:
- **`GarageSystem`** visar ett enhetligt och förenklat API för både GUI och CLI.
- Bakom fasaden skickas alla anrop vidare till specialiserade domäntjänster som var och en har ett enskilt ansvar (**Single Responsibility Principle**):
  - **`CustomerService`:** Registrerar, söker och validerar kunduppgifter och VIP-status.
  - **`VehicleService`:** Kopplar registreringsnummer, bilmodell och ägare, och kollar att kunden finns.
  - **`BookingService`:** Skapar tidsbokningar, sköter statusövergångar och validerar fordon.
  - **`WorkOrderService`:** Håller arbetsorderns tillståndsmaskin (`CREATED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`). Låser mekanikerns tillgänglighet när ordern påbörjas och frigör mekanikern när den avslutas.
  - **`BillingService`:** Genererar fakturor utifrån utförda tjänster, räknar ut 10% VIP-rabatt och hanterar kampanjkoder (`WELCOME10` ger 10%, `SERVICE200` drar av 200 kr).
  - **`PaymentService`:** Tar hand om betalningstransaktioner, validerar betalmetoden (`CARD`, `SWISH`, `CASH`), markerar fakturan som betald och sparar transaktionen.
  - **`MechanicSchedule`:** En schemaläggningsmotor som räknar ut varje mekanikers arbetsbelastning timme för timme, stoppar dubbelbokningar och tar fram färgprogressionen till Kanban-korten.

### 3.3 Internationell Flerspråksmotor (I18n)

- **Klass:** `com.wac.autocore.ui.i18n.I18n`
- **Resursfiler:** `resources/com/wac/autocore/i18n/sv.json` och `en.json`.
- **Egenskaper:**
   - **Samma nycklar:** Svenska och engelska språkfilerna hålls synkade.
  - **Realtidsväxling utan omstart:** När du klickar på språkknappen i sidomenyn uppdateras gränssnittet direkt, via lyssnare (`I18n.addListener()`).
  - **Parameterstöd:** Metoden `I18n.t("key", arg1, arg2)` stoppar in dynamiska parametrar (`{0}`, `{1}`) på plats.
  - **Robust felhantering:** Saknas en nyckel returneras nyckeln själv som fallback, utan att appen kraschar.

### 3.4 Datalager & Databasintegration (SQLite)

   - SQLite används för lokal lagring via JDBC.
   - `Db` skapar tabeller och seedar databasen när appen startar.
   - Repositoryklasserna sköter läsning och skrivning mot databasen.

### 3.5 Domänmodeller

Modellerna är rena, välkapslade klasser under `com.wac.autocore.model`:
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

När projektet startade hade koden flera typiska "Code Smells" som vi nu har refaktorerat bort:

| Ursprunglig brist (Sprint 1) | Genomförd åtgärd & Ny arkitektur |
| :--- | :--- |
| **Monolitisk "God Class"** (`GarageSystem` hade 400+ rader och skötte allt från utskrifter till rabattregler). | **Fasadmönstret implementerat:** `GarageSystem` delegerar nu till 7 specialiserade tjänster (`BillingService`, `CustomerService`, etc.). |
| **UI sammanflätat med affärslogik** (`System.out.println` spridda i domänmetoder). | **Skiktseparation:** All utskriftslogik flyttad till `ConsolePrinter`. Alla servicemetoder returnerar rena domänobjekt eller felkoder. |
| **Hårdkodad och duplicerad sökning** i linjära for-loopar. | **Granulär sökmotor:** `GlobalSearch` och `EntityLookup` indexerar och söker över alla entiteter med prefixstöd och skiftlägesokänslighet. |
| **Frånvaro av tester:** Startpaketet innehöll inga automatiska tester. | **Manuell kontroll:** Funktionerna körs genom applikationens GUI och konsolflöde. |
| **Hårdkodat språk och texter:** Svenska texter hårdkodade i Java-strängar. | **Full I18n-motor:** Extern ordbok i JSON med 396 nycklar och momentan språkväxling mellan svenska och engelska. |
| **Enkel standardlayout:** Startpaketet saknade en samlad visuell riktning. | **Eget tema:** Emerald-temat använder tydliga färger, fokusstilar och läsbara textstorlekar. |

---

## 5. Implementerade Design Patterns (GoF)

1. **Facade Pattern (`GarageSystem`):**
   - Ger en enkel och samlad ingång till delsystemen: bokning, arbete, fakturering och betalning. Både GUI och CLI anropar fasaden utan att känna till delsystemens inbördes beroenden.
2. **Layered Architecture (Skiktad arkitektur):**
   - Skikten är tydligt åtskilda: Presentationslager $\rightarrow$ Fasad $\rightarrow$ Servicelager $\rightarrow$ Domänmodeller $\rightarrow$ Datalager.
3. **Observer Pattern (Händelselyssnare):**
   - `I18n.addListener()`: Registrerar vyer och komponenter, som sedan uppdaterar sina texter automatiskt när språket växlas i realtid.
   - `PageRouter`: Meddelar navigationsmenyn när sidan byts, så att rätt sida ritas ut och aktiveras.
4. **Factory Pattern (`TableFactory`):**
   - Samlar skapandet av JavaFX `TableView` på ett ställe: inbyggd sökfiltrering, sortering, zebramönstrade rader (`.table-row-cell:odd / :even`) och anpassade cellformatters.
5. **Strategy / Specialiserade Beräkningstjänster:**
   - Rabattlogik och betalningsvalidering ligger isolerade i `BillingService` respektive `PaymentService`. Det gör det enkelt att lägga till nya kampanjregler eller betalsätt utan att röra fasad eller GUI.

---

## 6. Kvalitet och tillgänglighet

Projektet använder Java 8, SQLite och JavaFX. Koden är uppdelad i modeller,
repository, service och UI. Det gör det lättare att följa ett flöde från en vy
via fasaden och servicen till databasen.

Emerald-temat är anpassat för god läsbarhet. Text, bakgrunder och knappar har
tydliga kontraster, interaktiva komponenter har fokusstilar och textstorleken
hålls på en nivå som fungerar i programmets vyer. Detta är projektets praktiska
WCAG-anpassning, inte en formell certifiering.

---

## 7. Färdplan, Arbetsfördelning & Git-rutiner

### Status & Ansvarsområden i Grupp C:

| Gruppmedlem | Huvudfokus & Arbetsområde | Aktuell status |
| :--- | :--- | :--- |
| **Alex** | Systemarkitektur, fasad och flerspråksmotor | **Klart & Integrerat i develop** |
| **Daniel** | Databasintegration (SQLite-persistens via `lib/sqlite-jdbc-...`) | **Pågående arbete** |
| **Lucas** | Domänmodeller, affärsregler för ordrar och bokningsflöden | **Klart & Integrerat i develop** |
| **Alex** | JavaFX GUI-vyer, layout, styling, WCAG-anpassning & teman | **Klart & Integrerat i develop** |

### Git-rutiner & Branch-strategi:

> [!IMPORTANT]
> **Strikt regel för brancher:**  
> Allt aktivt arbete, all integration och alla feature-merger ska gå mot **`develop`**, inget annat.  
> **`main` är reserverad för slutlig produktionsrelease och får INTE pushas till under en pågående sprint.**

#### Så synkar du ditt arbete:
```bash
# Se till att stå på develop
git checkout develop

# Hämta in det senaste från teamet
git pull origin develop

# Kompilera och kör relevanta delar från IntelliJ innan du pushar nya ändringar
```

När alla 50 kontroller är gröna är det säkert att pusha koden till `origin/develop`.
