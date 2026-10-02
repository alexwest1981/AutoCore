# AutoCore - JavaFX GUI & Arkitektur

Här beskrivs JavaFX-gränssnittet i **Wigell AutoCore** (`com.wac.autocore.ui`): hur paketen hänger
ihop, hur data tar sig fram, och hur enhetstesterna körs.

---

## 1. Starta och köra

* **Starta applikationen:**
  ```bash
  ./run.sh Main
  ```
  eller öppna `Main.java` i IntelliJ och kör därifrån.

* **Kör enhetstesterna:**
  ```bash
  ./test.sh
  ```
  Testerna går genom `com.wac.autocore.test.TestRunner`, som kör direkt mot JDK 8 utan byggverktyg
  eller externa beroenden.

---

## 2. Arkitektur och moduluppdelning

Varje paket under `com.wac.autocore.ui` har en uppgift, i linje med Single Responsibility Principle.

### Huvudklass
* **`AutoCoreApp.java`**: tunn koordinator, omkring 80 rader. Sätter upp `Stage` och `Scene`
  (1280x800), root-`BorderPane` ("shell"), lägger på standtemat och knyter ihop sidonavigering,
  toppbar och sidrouter.

### Navigering (`navigation/`)
* **`SidebarView.java`**: bygger vänstermenyn med varumärkesbadge ("AC", "AutoCore Workshop System")
  och kollapsbara grupper (*Customers*, *Vehicles*, *Bookings*, *Workshop*, *Finance*). Håller reda
  på vilken knapp som är markerad (`.selected`).
* **`TopNavView.java`**: horisontell toppnavigering för "Top bar"-läget, med direktknappar och
  logiska sektioner. Markerar aktiv sida med samma WAC-tokens som övriga teman.
* **`PageRouter.java`**: byter sida och markerar rätt post i både `SidebarView` och `TopNavView`.
  Kopplar också den aktiva sidans tabell till toppbarens sökfält, så att filtreringen hamnar i rätt vy.

### Komponenter (`components/`)
* **`TopBar` (ligger i `AutoCoreApp.java`)**: toppmeny med varumärke (i toppläge), sökfält,
  layoutväxlare (`Sidebar ◧` / `Top bar ⎕`) och tema-ComboBox. Den ligger i applikationskoden för att
  vara lätt att ändra. Växlingen mellan fullbreddstoppmeny och sidomeny tappar inte aktiv sida.
  Styling för mörka och ljusa teman sköts dynamiskt, och popup-fönster ärver appens tema och
  `.root`-klass via reflektion, utan CSS-varningar.
* **`BookingFormPane.java`**: sammansatt formulärpanel för tidsbokning, med validering medan man
  skriver, koppling mot verkstadens tjänster, mekanikertilldelning och beräkning av tidsåtgång.
* **`TimeSlotCell.java`**: egen ComboBox-cell för tidsval som visar direkt om mekanikern är ledig
  (🟢) eller upptagen (🔴).
* **`MechanicKanbanCard.java`**: interaktivt Kanban-kort för mekanikerschemat. Dagsvy (07:00–16:00)
  med klickbara SVG-plusknappar för direktbokning, utfällbar *inline drawer* med bokningsinfo,
  veckovy med färgprogression i fyra steg (grön, gul, orange, röd), månadsvy för tillgänglighet,
  snabbnavigering till nästa bokningsdag (`📅 Nästa bokning: ... →`) och en kebabmeny (`⋮`) för att
  redigera eller ta bort en mekaniker.
* **`TableFactory.java`**: typad fabrik som bygger en `TableView` kopplad till `FilteredList`, med
  hjälpare för textkolumner (`col`), statusbadges (`badgeCol`) och sökning över flera kolumner
  (`applySearch`). Sökningen hämtar celldata direkt från radobjektet (`col.getCellData(row)`), vilket
  hindrar `IndexOutOfBoundsException` när man söker i en filtrerad vy.
* **`UiComponents.java`**: återanvändbara element som primära och sekundära knappar, KPI-kort,
  informationspaneler och standardiserade sidhuvuden (`pageHead`, `buildEntityPage`).

### Vyer (`views/`)
* **`OverviewView.java`**: huvuddashboarden med fyra KPI-kort (aktiva arbetsordrar, total omsättning,
  bokningar, mekaniker i tjänst), snabbknappar till modalerna, mekaniker-kanbantavlan med
  specialiseringsfilter och vågrät rullning, kommande bokningar och en sökbar tabell över de senaste
  arbetsordrarna.
* **`EntityPages.java`**: en byggare per domänentitet:
  - Kunder (med "+ New customer")
  - Fordon (med "+ Register vehicle")
  - Bokningar (med "+ New booking")
  - Arbetsordrar (med "+ New work order", samt "Start order" och "Complete order" som styrs av
    radurvalet)
  - Tjänster (priskatalog och beräknad tidsåtgång)
  - Mekaniker (specialisering och tillgänglighet)
  - Fakturor (med "+ Create invoice" och "Pay selected invoice")
  - Betalningar (med "+ Register payment")

### Ren logik och presentationshjälpare (`util/`)
* **`UiFormatters.java`**: formateringslogik som inte rör JavaFX-fönstret. Formaterar valuta
  (`formatMoney`) och datum (`todayFormatted`, `formatDate`), kortar långa texter och mappar statusord
  till CSS-badgeklasser.
* **`BookingAvailability.java`**: räknar ut lediga tidsslottar och upptäcker krockar mot mekanikerns
  schema.
* **`EntityLookup.java`**: slår upp namn på kund, fordon, mekaniker och tjänster via id mot
  `GarageSystem`.

---

## 3. Sökning i hela systemet

`SearchResultsView` är en egen vy för global sökning:
1. **Hela systemet på en gång:** när du skriver i toppbarens sökfält söks kunder, fordon,
   arbetsordrar, bokningar, mekaniker, fakturor och tjänster igenom via `GlobalSearch`.
2. **Egna paneler per sektion:** resultaten hamnar i separata paneler med antal träffar och egna
   tabeller. Paneler utan träffar visas inte.
3. **Vidare till rätt sida:** varje panel har en "Open in [Sektion] →"-knapp som går direkt till
   motsvarande entitetssida.
4. **Utan att fastna:** du kan söka på "Anna", se kunder och deras ordrar, och sedan byta till
   "Volvo" för att se fordon och bokningar, utan att gå via Overview emellan.
5. **Tillbaka automatiskt:** tömmer du sökfältet går vyn tillbaka till sidan du var på innan du
   började söka.

---

## 4. Enhetstester (`com.wac.autocore.test`)

Presentations-, beräknings-, sök- och uppslagslogiken ligger i rena hjälpklasser, så den går att
testa utan att öppna ett grafiskt fönster.

* **`GlobalSearchTest.java`**:
  - söker över kunder, fordon, mekaniker och arbetsordrar.
  - kontrollerar träffar över flera domänentiteter samtidigt.
  - kontrollerar skiftlägesokänslighet (uppercase/lowercase/mixed case).
  - kontrollerar tomma och ogiltiga söksträngar.
* **`TableFactoryTest.java`**:
  - söker över flera kolumner med `FilteredList`.
  - kontrollerar delsträngar, skiftlägesokänslighet och att blanksteg trimmas.
  - kontrollerar att sökning i en redan filtrerad tabell inte kraschar med indexfel.
* **`UiFormattersTest.java`**:
  - kontrollerar valutformatering (`long` och `double`).
  - kontrollerar texttrunkering med ellips (`…`).
  - kontrollerar översättning av statusord (`BOOKED` → `Booked`, och så vidare).
  - kontrollerar badge- och punkt-CSS-klasser (`success`, `danger`, `warn`, `info`).
  - kontrollerar engelsk datumgenerering.
* **`EntityLookupTest.java`**:
  - kontrollerar id-uppslag mot `GarageSystem` för kunder, fordon, mekaniker och tjänster.
  - kontrollerar fallback när id:t är okänt.
  - kontrollerar mekanikers specialisering och kvalificering för tjänster.
* **`OverviewMetricsTest.java`**:
  - kontrollerar KPI-beräkningarna: aktiva arbetsordrar, omsättning från lyckade betalningar och
    mekanikertillgänglighet.
* **`MechanicScheduleTest.java`**:
  - kontrollerar de nio dagslotterna (07:00–16:00), skyddet mot dubbelbokning, lediga slotter,
    avbokning, veckoöversikt, månadstillgänglighet, färgprogressionen (grön → gul → orange → röd)
    och `getNextBookingDate`.
* **`I18nTest.java`**:
  - kontrollerar språkväxling i realtid, parameteriserade meddelanden, fallback och full paritet
    mellan `sv.json` och `en.json`.
* **`CodeQualityTest.java`, `DocumentationTest.java`, `SecurityAuditTest.java`,
  `DataFlowAuditTest.java`, `WcagAccessibilityTest.java`**:
  - kontrollerar frikoppling av servicelagret, att dokumentsiffrorna stämmer med sviten,
    SQL-injektionsskydd (både mönster och dataflöde), att inga hemligheter ligger hårdkodade, och
    WCAG 2.1 AAA för kontrast och fokusringar.
* **`TestRunner.java`**:
  - egen testkörare med färgkodad utskrift och tydliga felrapporter. Den hittar testklasserna själv:
    allt som slutar på `Test.java` i paketet körs.

Kör testerna när som helst med:
```bash
./test.sh
```
eller en full kvalitets- och säkerhetsaudit med:
```bash
./check.sh
```

---

## 5. Teman och CSS

Applikationen har 7 färgteman som växlas direkt i toppmenyn medan programmet kör:
* `dark` (mörkt modernt, standard)
* `night` (djupt natt-tema)
* `light` (ljust och rent)
* `azure` (blå accent)
* `classic` (klassisk industristil)
* `emerald` (grön accent)
* `volt` (högkontrast neon)
* `default` (vanlig JavaFX Modena, för jämförelse och tillgänglighet)

### CSS-egenskaper som varit viktiga
1. **Ingen färgstagnation vid temabyte:** cellerna i ComboBox (`.theme-pick`) styrs helt via CSS i
   `components.css`, `dark.css` och `night.css`. Ingen `setStyle(...)` körs på `ListCell`, vilket
   tog bort buggen där celler behöll vit text efter ett byte mellan ljust och mörkt tema.
2. **Kontrast i tabeller:** både vanliga och markerade rader har egen text- och bakgrundsfärg, så
   ingen text blir osynlig i något tema.
3. **Modaler och dialoger:** `ActionDialogs.java` lägger det aktiva temat på alla `Dialog`- och
   `Alert`-fönster via en `setOnShowing`-lyssnare.
4. **Popup-scener:** ComboBox-popups synkroniserar sina stylesheets och behåller `.root`-klassen, så
   att Modenas standardtokens (`-fx-box-border`, `-fx-base`) alltid hittas utan CSS-varningar.
5. **Standtemat (vanlig JavaFX):** har egna regler för `.page-title` (24px fetstil) och aktiv meny
   (`.nav-item.selected`), så hierarkin syns även utan anpassat tema.
