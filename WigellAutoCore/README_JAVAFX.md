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
* **`TopBarView.java`**: Toppmeny med sökfält och tema-ComboBox. Inkluderar fast, kontrastrik styling (vit bakgrund / mörk text) och popup-scensynkronisering via Java-reflektion så att popup-fönstret alltid ärver appens tema.
* **`TableFactory.java`**: Typad fabrik för att skapa `TableView` kopplad till `FilteredList`. Innehåller hjälpare för standardtextkolumner (`col`), statusbadge-kolumner (`badgeCol`) och flerkolumnssökning (`applySearch`).
* **`UiComponents.java`**: Återanvändbara UI-element: primära och sekundära knappar, KPI-kort, informationspaneler och standardiserade sidhuvuden (`pageHead`, `buildEntityPage`).

### Vyer (`views/`)
* **`OverviewView.java`**: Huvuddashboard med 4 KPI-kort (Aktiva arbetsordrar, Total omsättning, Bokningar, Mekaniker i tjänst), snabbknappar för modaler, statusöversikt, kommande bokningar och senaste ordrar.
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

## 3. Automatiserade Enhetstester (`com.wac.autocore.test`)

Eftersom all presentations-, beräknings- och uppslagslogik är isolerad i rena hjälpklasser kan den enhetstestas till 100% utan att öppna ett grafiskt fönster.

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
  - Egenutvecklad, fristående test-runner med färgkodad utskrift och tydliga felrapporter.

Kör testerna när som helst med:
```bash
./test.sh
```

---

## 4. Teman & CSS-arkitektur

Applikationen stöder 7 färgteman som kan växlas direkt under körning i toppmenyn:
* `dark` (Mörkt modernt)
* `night` (Djupt natt-tema)
* `light` (Ljust och rent)
* `azure` (Blå accent)
* `classic` (Klassisk industristil)
* `emerald` (Grön accent)
* `volt` (Högkontrast neon)

### Viktiga CSS-egenskaper & fixar
1. **Full kontrast i tabeller:** Både vanliga och markerade rader har explicit textfärg och bakgrundsfärg för att undvika osynlig text i mörka teman.
2. **Modaler & dialoger:** [ActionDialogs.java](file:///home/alex/Documents/Skolgrejer/Systemarkitektur/WigellAutoCore/autocore/src/com/wac/autocore/ui/ActionDialogs.java) applicerar det aktiva temat på alla `Dialog` och `Alert`-fönster via en `setOnShowing`-lyssnare.
3. **Temaväljaren:** Har fast inline-styling för att garantera perfekt läsbarhet (vit bakgrund med mörk text) i samtliga teman.
4. **Popup-scener:** ComboBox-popups synkroniserar sina stylesheets med huvudscenen via `TopBarView.syncComboPopup`.
