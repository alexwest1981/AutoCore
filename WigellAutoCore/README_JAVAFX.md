# AutoCore: JavaFX-gränssnittet

Hur paketen under `com.wac.autocore.ui` hänger ihop. Startinstruktioner finns i [README.md](../README.md).

## Huvudklassen

`AutoCoreApp.java` sätter upp `Stage` och `Scene`, temat, sidonavigeringen och sidroutern.

## Navigering (`navigation/`)

- `SidebarView.java` bygger vänstermenyn med varumärkesbadge och grupper: Customers, Vehicles, Bookings, Workshop, Finance. Håller reda på vilken post som är markerad.
- `TopNavView.java` är samma navigering i toppläge, med direktknappar och sektioner.
- `PageRouter.java` byter sida och markerar rätt post i menyn.

## Komponenter (`components/`)

- `BookingFormPane.java` är formuläret för tidsbokning. Validering medan man skriver, tjänster och mekaniker, och beräkning av tidsåtgång.
- `TimeSlotCell.java` visar i tidslistan om mekanikern är ledig eller upptagen.
- `MechanicKanbanCard.java` är Kanban-kortet för mekanikerschemat, med dag, vecka och månad, direktbokning på lediga timmar, en utfällbar detaljlåda och en meny för att redigera eller ta bort mekanikern.
- `TableFactory.java` bygger tabeller med hjälpare för textkolumner och statusbadges.
- `UiComponents.java` har knappar, KPI-kort, informationspaneler och de återkommande sidhuvudena.

Toppbaren ligger med flit i `AutoCoreApp.java`, så att den är lätt att ändra. Den växlar mellan sidomeny och toppmeny utan att tappa aktiv sida.

## Vyer (`views/`)

- `OverviewView.java` är dashboarden: fyra KPI-kort, snabbknappar, mekaniker-Kanban, kommande bokningar och de senaste arbetsordrarna.
- `EntityPages.java` har en byggare per entitet: kunder, fordon, bokningar, arbetsordrar, tjänster, mekaniker, fakturor och betalningar. Varje sida får sina knappar, som Start order och Complete order på arbetsordrar.

## Ren logik (`util/`)

- `UiFormatters.java` formaterar valuta och datum, kortar långa texter och mappar statusord till badgeklasser.
- `BookingAvailability.java` räknar fram lediga tider och hittar krockar mot schemat.
- `EntityLookup.java` slår upp namn på kund, fordon, mekaniker och tjänst via id.

Ingen av dem rör fönstret, så de går att testa utan att starta JavaFX.

## Tema och CSS

Appen kör ett tema: `emerald`, låst i `ThemeManager`. CSS ligger i `resources/com/wac/autocore/theme/`.

Fyra saker har varit värda att hålla fast vid:

1. Temat sätts på alla dialoger och alert-fönster via en `setOnShowing`-lyssnare i `ActionDialogs.java`, annars faller de tillbaka på JavaFX standard.
2. ComboBox-popupar synkar sina stylesheets och behåller `.root`-klassen, så standardtokens hittas utan CSS-varningar.
3. Ingen `setStyle(...)` sätts på celler. Stylingen sker i CSS, annars fastnar färger när temat byts.
4. Tabellrader har egen text- och bakgrundsfärg både vanliga och markerade, så ingen text blir osynlig.
