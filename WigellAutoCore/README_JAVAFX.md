# AutoCore - JavaFX GUI & Arkitektur

Detta dokument beskriver JavaFX-gränssnittet i **Wigell AutoCore** (`com.wac.autocore.ui`), dess modulära arkitektur, hur data flödar samt hur de automatiserade enhetstesterna fungerar.

---

## 1. Starta och köra

* **Starta huvudapplikationen:**
  ```bash
  ./run.sh Main
  ```
  eller kör `Main.java` direkt via IDE (IntelliJ).

* **Kör automatiserade enhetstester:**
  ```bash
  ./test.sh
  ```
  Tester körs via den fristående test-runnern `com.wac.autocore.test.TestRunner` direkt mot JDK 8 utan externa byggberoenden.

---

## 2. Arkitektur och Moduluppdelning

Arkitekturen följer Single Responsibility Principle och är indelad i följande paket under `com.wac.autocore.ui`:

### Huvudklass
* **`AutoCoreApp.java`**: Slank koordinator (~80 rader). Sätter upp JavaFX `Stage`, `Scene` (1280x800), root `BorderPane` ("shell"), applicerar default-tema och kopplar samman sidonavigering, toppbar och sidrouter.

### Navigering (`navigation/`)
* **`SidebarView.java`**: Bygger vänstermenyn med varumärkesbadge ("AC", "AutoCore Workshop System") och kollapsbara grupper (*Customers*, *Vehicles*, *Bookings*, *Workshop*, *Finance*). Hanterar aktiv knappmarkering (`.selected`).
* **`PageRouter.java`**: Ansvarar för sidbyten och kopplar automatiskt den aktiva sidans tabell till toppbarens sökfält så att filtrering sker mot rätt vy.

### Komponenter (`components/`)
* **`TopBar` (inbyggd i `AutoCoreApp.java`)**: Toppmeny med sökfält och tema-ComboBox. Ligger direkt i applikationskoden så att den enkelt kan avaktiveras (kommentera bort `mainCol.setTop(...)`). Inkluderar dynamisk styling för mörka och ljusa teman samt popup-scensynkronisering via Java-reflektion så att popup-fönstret ärver appens tema och `.root`-klass utan CSS-varningar.
* **`TableFactory.java`**: Typad fabrik för att skapa `TableView` kopplad till `FilteredList`. Innehåller hjälpare för standardtextkolumner (`col`), statusbadge-kolumner (`badgeCol`) och flerkolumnssökning (`applySearch`). Sökningen hämtar celldata direkt från radobjektet (`col.getCellData(row)`), vilket förhindrar `IndexOutOfBoundsException` när filtrerade vyer söks.
* **`UiComponents.java`**: Återanvändbara UI-element: primära och sekundära knappar, KPI-kort, informationspaneler och standardiserade sidhuvuden (`pageHead`, `buildEntityPage`).

### Vyer (`views/`)
* **`OverviewView.java`**: Huvuddashboard med 4 KPI-kort (Aktiva arbetsordrar, Total omsättning, Bokningar, Mekaniker i tjänst), snabbknappar för modaler, statusöversikt, kommande bokningar och sökbar tabell för senaste arbetsordrar.
* **`EntityPages.java`**: Dedikerade byggare för varje domänentitet:
  - Kunder (med "+ New customer")
  - Fordon (med "+ Register vehicle")
  - Bokningar (med "+ New booking")
  - Arbetsordrar (med "+ New work order", samt dynamiska "Start order" / "Complete order" knappar styrda av radurval)
  - Tjänster (priskatalog och beräknad tidsåtgång)
  - Mekaniker (specialisering och tillgänglighet)
  - Fakturor (med "+ Create invoice" och "Pay selected invoice")
  - Betalningar (med "+ Register payment")

### Ren logik & Presentationshjälpare (`util/`)
* **`UiFormatters.java`**: Ren formateringslogik frikopplad från JavaFX-fönsterkontexten. Formaterar valuta (`formatMoney`), datum (`todayFormatted`, `formatDate`), trunkerar långa texter och mappar statusord till CSS-badgeklasser.
* **`EntityLookup.java`**: Slår upp relaterade namn (kund, fordon, mekaniker, tjänster) baserat på ID mot `GarageSystem`.

---

## 3. Sökarkitektur & Flöde

Sökfunktionen i AutoCore är genomgående integrerad mellan toppmenyn och sidorna:
1. **Flerkolumnssökning:** `TableFactory.applySearch(...)` matchar användarens sökord mot alla relevanta kolumner på en entitet (t.ex. namn, telefon, e-post, adress för kunder; regnummer, märke, modell för fordon).
2. **Koppling mot tabell:** När en vy laddas registrerar den sin tabell hos `PageRouter.setActiveTable(...)`.
3. **Bevarad sökning vid sidbyten:** Om användaren har skrivit något i sökfältet och klickar till en annan sida i sidomenyn, appliceras sökordet automatiskt på den nya sidans tabell omedelbart vid sidbytet.
4. **Startsida (Overview):** Sökfältet är kopplat till tabellen för senaste arbetsordrar direkt vid applikationens start.
5. **Smart navigering vid Enter:** Om användaren trycker `Enter` i sökfältet från Overview analyseras söktermen (t.ex. registreringsnummer, mekanikernamn, kundnamn) och användaren navigeras automatiskt till lämplig vy med sökfiltret applicerat.

---

## 4. Automatiserade Enhetstester (`com.wac.autocore.test`)

Eftersom all presentations-, beräknings-, sök- och uppslagslogik är isolerad i rena hjälpklasser kan den enhetstestas till 100% utan att öppna ett grafiskt fönster.

* **`TableFactoryTest.java`**:
  - Testar flerkolumnssökning med `FilteredList`.
  - Verifierar delsträngsmatchning, skiftlägesokänslighet (case-insensitivity) och blankstegstrimning.
  - Verifierar att sökning på redan filtrerad tabell inte kraschar med indexfel.
* **`UiFormattersTest.java`**:
  - Testar valutaformatering (`long` och `double`).
  - Testar texttrunkering med ellipser (`…`).
  - Testar statusordsöversättning (`BOOKED` $\rightarrow$ `Booked`, etc.).
  - Testar badge- och punkt-CSS-klasser (`success`, `danger`, `warn`, `info`).
  - Testar engelsk datumgenerering.
* **`EntityLookupTest.java`**:
  - Testar ID-uppslag mot `GarageSystem` för kunder, fordon, mekaniker och tjänster.
  - Verifierar korrekta fallbacks för okända ID:n.
* **`OverviewMetricsTest.java`**:
  - Verifierar KPI-beräkningar för aktiva arbetsordrar, total omsättning från lyckade betalningar och mekanikertillgänglighet.
* **`TestRunner.java`**:
  - Egenutvecklad, fristående test-runner med färgkodad utskrift och tydliga felrapporter. Totalt 17 automatiserade tester.

Kör testerna när som helst med:
```bash
./test.sh
```

---

## 5. Teman & CSS-arkitektur

Applikationen stöder 7 färgteman som kan växlas direkt under körning i toppmenyn:
* `dark` (Mörkt modernt, standard)
* `night` (Djupt natt-tema)
* `light` (Ljust och rent)
* `azure` (Blå accent)
* `classic` (Klassisk industristil)
* `emerald` (Grön accent)
* `volt` (Högkontrast neon)
* `default` (Plain JavaFX Modena för jämförelse och tillgänglighet)

### Viktiga CSS-egenskaper & fixar
1. **Ingen färgstagnation vid temabyte:** Cellerna i ComboBox (`.theme-pick`) styrs uteslutande via CSS-regler i `components.css`, `dark.css` och `night.css`. Inga inline `setStyle(...)` används på `ListCell`, vilket eliminerar buggen där celler behöll vit text efter byte mellan ljusa och mörka teman.
2. **Full kontrast i tabeller:** Både vanliga och markerade rader har explicit textfärg och bakgrundsfärg för att undvika osynlig text i alla teman.
3. **Modaler & dialoger:** [ActionDialogs.java](file:///home/alex/Documents/Skolgrejer/Systemarkitektur/WigellAutoCore/autocore/src/com/wac/autocore/ui/ActionDialogs.java) applicerar det aktiva temat på alla `Dialog` och `Alert`-fönster via en `setOnShowing`-lyssnare.
4. **Popup-scener:** ComboBox-popups synkroniserar sina stylesheets och behåller `.root`-klassen så att Modenas standardtokens (`-fx-box-border`, `-fx-base`) alltid hittas utan CSS-varningar.
5. **Default-temat (Plain JavaFX):** Innehåller explicita regler för `.page-title` (24px fetstil) och aktiv meny (.nav-item.selected) för tydlig hierarki även utan anpassat designtema.

