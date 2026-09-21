# AutoCore

AutoCore (Wigell AutoCore) är ett affärs- och verkstadssystem (Core ERP / Garage Management System) utvecklat för koncernen Wigell Group.

## Översikt
Systemet hanterar den dagliga operativa verksamheten på en bilverkstad:
- **Kunder & Fordon:** Registrering och koppling av ägare till fordon.
- **Bokningar:** Tidsbokning för service och felsökning.
- **Mekaniker & Tjänster:** Register över mekaniker, specialiteter och priskatalog för verkstadstjänster.
- **Arbetsordrar:** Hantering av arbetsordrar, tilldelning av mekaniker och statusflöde.
- **Fakturering & Rabattsystem:** Fakturering med stöd för VIP-rabatt och kampanjkoder.
- **Betalningar:** Betalningshantering via kort, Swish och kontant.

## Skärmbilder
JavaFX-gränssnittet (AutoCore, tema *Emerald*):

![AutoCore JavaFX-gränssnitt](WigellAutoCore/docs/autocore-javafx-gui.png)

## Teknisk stack
- Java (JDK 8, BellSoft Liberica Full med JavaFX)
- JavaFX-gränssnitt (GUI)
- Konsolgränssnitt (CLI)

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

* **Huvudapplikationen (AutoCore GUI):** Kör `Main.java` i IntelliJ (eller via terminal: `./run.sh`)
* **Automatiserade enhetstester:** Kör `./test.sh` (eller `com.wac.autocore.test.TestRunner`)
* **Konsolversionen (CLI):** Kör `ConsoleApp.java` (eller via terminal: `./run.sh ConsoleApp`)
* **Modulvisa testappar:**
  - **Kunder:** `com.wac.autocore.gui.customers.TestCustomer`
  - **Fordon:** `com.wac.autocore.gui.vehicle.VehicleTest`
  - **Bokningar:** `com.wac.autocore.gui.booking.TestBookingApp`
  - **Arbetsordrar:** `com.wac.autocore.gui.workorder.TestWorkOrderApp`
  - **Fakturor:** `com.wac.autocore.gui.invoice.TestInvoiceApp`
  - **Betalningar:** `com.wac.autocore.gui.payment.TestPaymentApp`
  - **Mekaniker:** `com.wac.autocore.gui.mechanic.TestMechanicApp`
  - **Tjänster:** `com.wac.autocore.gui.serviceItem.TestServiceItemApp`

## JavaFX UI Arkitektur & Refaktorisering

JavaFX-gränssnittet i `com.wac.autocore.ui` är uppbyggt enligt Single Responsibility Principle för hög modularitet, enkel utbyggnad och hög testbarhet:

```
com.wac.autocore.ui/
├── AutoCoreApp.java               # Huvudapplikation – Stage, Scene, Shell samt inbyggd TopBar
├── ActionDialogs.java             # Modaler för Ny bokning, Ny order, Skapa faktura, Betalning m.fl.
├── components/
│   ├── UiComponents.java          # Återanvändbara knappar, paneler, KPI-kort och sidhuvuden
│   └── TableFactory.java          # Fabrik för typade, sökbara TableView med FilteredList och badge-chips
├── navigation/
│   ├── SidebarView.java           # Kollapsbara menysektioner och brand-information
│   └── PageRouter.java            # Sidrouter med sökbevarande och smart navigation
├── util/
│   ├── UiFormatters.java          # Ren formateringslogik (valuta, datum, statusord, badge-klasser)
│   └── EntityLookup.java          # Snabb uppslagning av relaterade entitetsnamn via ID
└── views/
    ├── OverviewView.java          # Dashboard med 4 KPI-kort, snabbknappar, statusfördelning och sökbar orderlista
    └── EntityPages.java           # Dedikerade vyer för Kunder, Fordon, Bokningar, Arbetsordrar, Tjänster, etc.
```

### TopBar & Granulär Global Sökning
* **Inbyggd i `AutoCoreApp.java`:** Toppmenyn ligger direkt i applikationskoden för enkel hantering och kan stängas av med en enda rad kommentar (`// mainCol.setTop(buildTopBar(router));`).
* **Sektionsindelad global sökvy (`SearchResultsView`):** När användaren söker i toppbarens sökfält söks hela systemet igenom (Kunder, Fordon, Arbetsordrar, Bokningar, Mekaniker, Fakturor och Tjänster).
* **Granulära sektioner:** Träffarna delas in i tydliga sektionspaneler (t.ex. *Customers (2)*, *Vehicles (1)*, *Work Orders (3)*). Endast sektioner med aktiva träffar visas.
* **Snabblänkar:** Varje sektion har en "Open in [Sektion] →"-knapp för att direkt öppna den relevanta entitetssidan.
* **Sömlöst flöde:** Söker du t.ex. "Anna" visas kunder/ordrar för Anna; ändrar du direkt till "Volvo" visas fordon och bokningar för Volvo utan att du behöver gå tillbaka till Overview. Tömmer du sökfältet återgår vyn automatiskt till din tidigare sida.
* **Robust sökalgoritm (`GlobalSearch`):** Isolerad ren söklogik i `com.wac.autocore.ui.util.GlobalSearch` som testas till 100% utan GUI.

### Automatiserade tester (`com.wac.autocore.test`)
All beräknings-, formaterings-, sök- och uppslagslogik har isolerats och täcks av automatiserade enhetstester:
* **`GlobalSearchTest`**: Verifierar granulär sökning över kunder, fordon, mekaniker, ordrar, skiftlägesokänslighet och tomma sökningar.
* **`TableFactoryTest`**: Verifierar flerkolumnssökning och regressionsskyddar mot indexbuggar vid filtrering.
* **`UiFormattersTest`**: Valuta (long/double), trunkering, statusöversättning, datum och badge-CSS-klasser.
* **`EntityLookupTest`**: Uppslagning mot `GarageSystem` för kundnamn, fordonsreg, mekaniker och tjänster.
* **`OverviewMetricsTest`**: Verifiering av KPI-mätetal (aktiva ordrar, omsättning, tillgänglighet).
* **Kör tester:** Kör `./test.sh` i terminalen. Alla 24 enhetstester körs på under en sekund.

### Tema- och stilhantering
* **Stöd för Light/Dark-mode & färgteman:** `dark`, `night`, `light`, `azure`, `classic`, `emerald`, `volt` samt `default` (Plain JavaFX).
* **Ren CSS-styrning:** Temaväljaren och popup-listan styrs via CSS (`components.css`, `dark.css`, `night.css`) utan hackiga inline-stilar på återanvända celler.
* **Inga CSS-varningar:** Popup-scenen synkroniserar stilklassen `.root` med huvudscenen så att Modenas tokens (`-fx-accent`, `-fx-box-border`) alltid finns tillgängliga.
* **Tydliga rubriker i default-temat:** Plain JavaFX har tydliga sidrubriker (24px fetstil) och markerad aktiv vy i sidomenyn.
* **Modaler & Alerts:** Ärver automatiskt det aktiva temat via `setOnShowing`-lyssnare.

## Design & Styleguide
Projektets visuella riktlinjer, komponentbibliotek och färgteman finns sammanställda i den interaktiva styleguiden:
- [STYLEGUIDE.html](STYLEGUIDE.html) (öppnas i valfri webbläsare för live-förhandsgranskning och tematester)

## Utvecklingsteam
Grupp C: Alex, Lucas, Daniel, Vivianne
