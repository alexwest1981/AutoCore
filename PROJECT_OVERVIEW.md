# Projektöversikt & Arkitekturansats: Wigell AutoCore

Detta dokument ger en fullständig genomgång av projektet **Wigell AutoCore**, alla dess filer och komponenter, vad projektet är avsett att uppnå ur både ett domänmässigt och systemarkitektoniskt perspektiv, samt en konkret guide för hur utvecklingsteamet (**Grupp C: Alex, Lucas, Daniel, Vivianne**) kan påbörja arbetet.

## Innehållsförteckning

* _Övergripande syfte & Domän_

* _Fil- och katalogstruktur_

* _Detaljerad filgenomgång_

* _Nulägesanalys & Arkitektoniska brister (Code Smells)_

* _Vad man tänkt uppnå ur arkitektursynpunkt_

* _Föreslagna Design Patterns_

* _Föreslagen Målarkitektur (Layered Architecture)_

* _Hur vi börjar utvecklingen – Steg för steg_

## 1. Övergripande syfte & Domän

**Wigell AutoCore** är ett kärnsystem (Core ERP/Garage Management System) utvecklat för koncernen *Wigell Group*. Systemet hanterar den dagliga operativa verksamheten på en bilverkstad:

* **Kunder & Fordon:** Registrering och koppling av ägare till fordon.

* **Bokningar (Bookings):** Kunder bokar in sitt fordon för service eller felsökning ett givet datum.

* **Mekaniker & Tjänster:** Register över tillgängliga mekaniker (med specialiteter) och en priskatalog över verkstadstjänster (oljebyte, bromsbyte, etc.).

* **Arbetsordrar (Work Orders):** En bokning omvandlas till en arbetsorder där en ledig mekaniker tilldelas och en eller flera tjänster kopplas på. Arbetsordern genomgår livscykeln: CREATED $\rightarrow$ IN_PROGRESS $\rightarrow$ COMPLETED.

* **Fakturering & Rabattsystem:** När en arbetsorder är slutförd skapas en faktura. Rabatter appliceras baserat på kundstatus (VIP ger 10%) samt eventuella kampanjkoder (WELCOME10, SERVICE200).

* **Betalning:** Betalningar genomförs mot fakturan via olika betalsätt (*CARD*, *SWISH*, *CASH*). Fakturan markeras som betald och kunden aviseras.

## 2. Fil- och katalogstruktur

Projektets källkod finns under WigellAutoCore/autocore/src/:

```
Systemarkitektur/
│
├── Gruppindelning agil systemutveckling.pdf   # Kursgrupper (Grupp C: Alex, Lucas, Daniel, Vivianne)
├── Systemarkitektur.iml                       # IntelliJ modulfil för root
├── WigellAutoCore.zip                         # Originalarkiv med startkoden
│
└── WigellAutoCore/
    └── autocore/
        ├── autocore.iml
        └── src/
            ├── Main.java                      # Startpunkt & textbaserat konsolgränssnitt (CLI)
            │
            └── com/wac/autocore/
                ├── data/
                │   └── Database.java          # Statisk in-memory databas med testdata
                │
                ├── model/
                │   ├── Booking.java           # Tidsbokning av fordon
                │   ├── Customer.java          # Kunddata och VIP-status
                │   ├── Invoice.java           # Fakturaunderlag och belopp
                │   ├── Mechanic.java          # Mekaniker, specialitet och tillgänglighet
                │   ├── Payment.java           # Betalningstransaktion
                │   ├── ServiceItem.java       # Priskatalog/arbetsmoment med tidsestimat
                │   ├── Vehicle.java           # Fordonsuppgifter kopplade till kund
                │   └── WorkOrder.java         # Arbetsorder med tilldelad mekaniker & tjänster
                │
                └── service/
                    └── GarageSystem.java      # Monolitisk serviceklass (all affärslogik)
```

## 3. Detaljerad filgenomgång

### 3.1 Startpunkt & UI: Main.java

* **Placering:** src/Main.java (default package)

* **Roll:** Applikationens ingångspunkt via konsolen.

* **Funktionalitet:**

* Innehåller en while(running)-loop med en 17-vals meny (0–16).

* Sköter inmatning från användaren med Scanner.

* Hjälpmetoder för felhantering vid inmatning: readInt(msg) och readDate(msg).

* Anropar instansen GarageSystem garageSystem för alla operationer.

* **Problem:**

* Meny, inmatning och flödesstyrning ligger blandat i samma fil.

* Viss affärsvalidering sker i Main (t.ex. parsning av kommaseparerade tjänste-ID:n input.split(",")), medan annan sker i GarageSystem.

### 3.2 Servicelager: com.wac.autocore.service.GarageSystem.java

* **Roll:** Fungerar just nu som en "God Class" eller ett pseudo-fasadlager.

* **Metoder för visning:**

* showCustomers(), showVehicles(), showBookings(), showServiceItems(), showMechanics(), showWorkOrders(), showInvoices(), showPayments()

* Alla dessa hämtar listor direkt från Database och skriver ut till konsolen med System.out.println.

* **CRUD & Affärsprocesser:**

* createCustomer(...): Skapar och sparar kund.

* createVehicle(...): Verifierar att kunden existerar innan fordonet skapas.

* createBooking(...): Verifierar fordon innan bokning skapas.

* createWorkOrder(...): Validerar bokning, mekaniker (kollar mechanic.isAvailable()) samt tjänste-ID:n. Uppdaterar bokningens status till "WORK_ORDER_CREATED".

* startWorkOrder(workOrderId): Sätter status "IN_PROGRESS", låser mekanikern (setAvailable(false)).

* completeWorkOrder(workOrderId): Sätter status "COMPLETED", frigör mekanikern (setAvailable(true)).

* createInvoice(workOrderId, discountCode): Beräknar totalpris från tjänster, applicerar VIP-rabatt (10%) och hårdkodade rabattkoder (WELCOME10, SERVICE200), mockar en notifiering till kunden.

* processPayment(invoiceId, paymentType): Hårdkodade if/else för CARD, SWISH, CASH. Mockar anslutning till externa betaltjänster, sätter fakturan som betald och skickar bekräftelse.

* **Hjälpmetoder:**

* findCustomer, findVehicle, findBooking, findMechanic, findServiceItem, findWorkOrder, findInvoice (linjära for-loopar över Database.get...()).

### 3.3 Datalager: com.wac.autocore.data.Database.java

* **Roll:** Simulerar en databas med statiska ArrayList-samlingar i minnet.

* **Funktionalitet:**

* Statiska listor: customers, vehicles, bookings, serviceItems, mechanics, workOrders, invoices, payments.

* static { loadSampleData(); }: Laddar in 3 kunder (varav 1 VIP), 3 fordon, 4 tjänster, 3 mekaniker och 2 bokningar vid uppstart.

* Statiska getters som returnerar referenser direkt till de interna listorna.

* **Problem:**

* Global statisk status försvårar isolerade enhetstester.

* Ingen inkapsling: extern kod kan modifiera eller rensa listorna direkt via Database.getCustomers().clear().

* Ingen abstraktion via Repository- eller DAO-interface.

### 3.4 Domänmodeller: com.wac.autocore.model.*

**Klass**

**Attribut**

**Anmärkningar & Utvecklingspotential**

**Customer**

id, name, phone, email, vip

Enkel POJO. Har isVip() / setVip().

**Vehicle**

id, registrationNumber, brand, model, year, customerId

Håller endast customerId som int (främmande nyckel).

**Booking**

id, vehicleId, date, description, status

status är en vanlig String ("BOOKED", "WORK_ORDER_CREATED", etc.). Borde vara en Enum!

**Mechanic**

id, name, phone, specialization, available

Håller reda på om mekanikern är upptagen i en arbetsorder.

**ServiceItem**

id, name, description, price, estimatedMinutes

Fast katalogpris i SEK och tidsåtgång i minuter.

**WorkOrder**

id, bookingId, mechanicId, serviceItemIds (List<Integer>), status

Håller lista av ID:n. Status är String ("CREATED", "IN_PROGRESS", "COMPLETED").

**Invoice**

id, workOrderId, invoiceDate, amount, discount, totalAmount, paid

Innehåller metod calculateTotalAmount() (amount - discount).

**Payment**

id, invoiceId, amount, paymentType, paymentDate, successful

paymentType är en sträng ("CARD", "SWISH", "CASH").

## 4. Nulägesanalys & Arkitektoniska brister (Code Smells)

Koden är medvetet skriven som ett "före-projekt" i kursen Systemarkitektur. Den fungerar rent funktionellt men bryter mot flera grundläggande SOLID-principer och designmönster:

* **Monolitisk "God Class" & SRP-brott i ****GarageSystem****:** - Klassen ansvarar för användargränssnitt (System.out.println), affärsvalidering, tillståndshantering, sökning i listor, prisberäkningar, rabattregler och mockade externa anrop.

* **Hårdkodad betalningslogik (OCP-brott):** - I processPayment() används hårdkodade if (paymentType.equalsIgnoreCase("CARD")) .... Om vi vill lägga till *Klarna* eller *Faktura 30 dagar* måste vi ändra i GarageSystem.

* **Hårdkodade rabatter (OCP-brott):** - VIP och kampanjkoder är sammanflätade med if/else i faktureringsmetoden.

* **Hårdkodade aviseringar (Mock notifications):** - Utskrifter som *"Sending invoice notification to customer..."* och *"Sending payment confirmation to customer..."* ligger inline. Det finns ingen mekanism för att byta mellan SMS, E-post eller Push, eller lyssna på händelser.

* **Globalt statiskt tillstånd (****Database****):** - Gör enhetstestning skör eftersom tester påverkar varandras data.

* **Magiska strängar för status och betalsätt:** - Statusar som "BOOKED", "COMPLETED" etc. och betaltyper "CARD" är vanliga strängar utan typ-säkerhet.

* **Beroendeinversion saknas (DIP-brott):** - Klasser beror på konkreta implementationer och statiska metoder istället för interface.

## 5. Vad man tänkt uppnå ur arkitektursynpunkt

Uppgiften i kursen syftar till att: 1. **Transformera en proceduriell/monolitisk kodbas** till en väldesignad, objektorienterad och underhållbar arkitektur. 2. **Applicera GoF-designmönster (Gang of Four):** - Kursmapparna i närmiljön visar tydligt fokusområdena: *Adapter, Bridge, Facade, Proxy, Singleton, Strategy/State/Observer*. 3. **Införa skiktad arkitektur (Separation of Concerns):** - Skilja UI (konsol/meny) från Affärslogik (Services) och Datalager (Repositories). 4. **Uppnå hög testbarhet och följa SOLID-principerna:** - Varje klass ska ha ett enda ansvar (Single Responsibility). - Nya betalsätt eller rabattregler ska kunna läggas till utan att röra befintlig kod (Open/Closed).

## 6. Föreslagna Design Patterns

Följande mönster passar domänen och löser de identifierade problemen:

### 1. Strategy Pattern (eller Adapter Pattern) för Betalningar

* **Varför:** Skilj på olika betalsätt (*Card*, *Swish*, *Cash*).

* **Struktur:** java public interface PaymentMethod { PaymentResult pay(Invoice invoice); } public class CardPayment implements PaymentMethod { ... } public class SwishPayment implements PaymentMethod { ... } public class CashPayment implements PaymentMethod { ... }

* Kan även kombineras med **Adapter Pattern** om man vill simulera anslutning mot externa SDK:er (t.ex. ett tredjeparts Swish-API eller Bank-API).

### 2. Strategy Pattern för Rabatter (Discounts)

* **Varför:** Rabatter kan kombineras eller variera över tid.

* **Struktur:** java public interface DiscountStrategy { double calculateDiscount(Customer customer, double subtotal, String code); } public class VipDiscountStrategy implements DiscountStrategy { ... } public class PromotionCodeDiscountStrategy implements DiscountStrategy { ... }

### 3. Repository Pattern för Datalagring

* **Varför:** Ersätt statiska Database.get...() med riktiga repositories som implementerar interface.

* **Struktur:**

* CustomerRepository, VehicleRepository, BookingRepository, WorkOrderRepository, InvoiceRepository.

* Gör det enkelt att mocka databasen i enhetstester eller byta till fil- eller SQL-lagring i framtiden.

### 4. Observer Pattern (eller Bridge) för Händelser/Notifieringar

* **Varför:** När en order ändrar status, en faktura skapas eller en betalning sker ska notifieringar skickas automatiskt utan att koppla ihop servicen med notifieringstjänsten.

* **Struktur:**

* EventListener / NotificationService med lyssnare som EmailNotifier, SmsNotifier.

### 5. Facade Pattern för GarageSystem

* **Varför:** GarageSystem bör fungera som en ren **Fasad (Facade)** mot de interna delsystemen (BookingService, WorkOrderService, BillingService, PaymentService), så att Main bara anropar en ren och enkel fasad utan att veta om alla interna mekanismer.

### 6. State Pattern eller Enums för Arbetsorderns livscykel

* **Varför:** Ersätt "CREATED", "IN_PROGRESS", "COMPLETED" med typade tillstånd som skyddar mot otillåtna statusövergångar (t.ex. att inte kunna starta en redan avslutad order).

## 7. Föreslagen Målarkitektur (Layered Architecture)

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│         ConsoleUI / Main / Menu / InputReader          │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    APPLICATION / FACADE                │
│                 GarageFacade / GarageSystem            │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐  ┌──────────▼─────────────┐
│      DOMAIN SERVICES       │  │   STRATEGIES & OBSERVERS│
│ - CustomerService          │  │ - PaymentStrategy      │
│ - BookingService           │  │ - DiscountStrategy     │
│ - WorkOrderService         │  │ - NotificationListener │
│ - BillingService           │  │                        │
└─────────────┬──────────────┘  └────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────┐
│                   DOMAIN ENTITIES & ENUMS              │
│ Customer, Vehicle, Booking, WorkOrder, Invoice, etc.   │
│ Enums: OrderStatus, PaymentType                        │
└─────────────────────────────┬──────────────────────────┘
                              │
┌─────────────────────────────▼──────────────────────────┐
│                 INFRASTRUCTURE / DATA                  │
│       Repository Interfaces & In-Memory Impl.          │
│ (CustomerRepository, WorkOrderRepository, etc.)        │
└────────────────────────────────────────────────────────┘
```

## 8. Hur vi börjar utvecklingen – Steg för steg

Här är en praktisk färdplan för gruppen (**Alex, Lucas, Daniel, Vivianne**):

### Steg 1: Etablera Git & Samarbetsrutiner

* Initiera ett Git-repository i projektet (om det inte redan är gjort): bash git init git add . git commit -m "feat: initial commit with original WigellAutoCore starter code"

* Skapa ett gemensamt GitHub/GitLab-repo och bjud in alla gruppmedlemmar.

* Bestäm en gemensam branch-strategi: - main: Stabil och körbar kod. - develop eller funktionsgrenar: t.ex. feature/refactor-models-enums, feature/payment-strategy, feature/repositories.

* Skapa en enkel Kanban-tavla (t.ex. GitHub Projects eller Trello) baserad på modulerna nedan.

### Steg 2: Skriv baslinjetester (Characterization Tests)

* **Innan ni börjar refaktorera:** Skriv enkla JUnit-tester mot befintliga GarageSystem och modellerna för att bekräfta:

* Att rabattberäkning blir rätt för VIP och koder.

* Att mekaniker blir upptagen när en arbetsorder startar och ledig när den slutförs.

* Att betalning markerar fakturan som paid = true.

* Testerna blir ert skyddsnät som garanterar att refaktoriseringen inte introducerar buggar!

### Steg 3: Typa upp systemet (Enums & Validering)

* Skapa enums:

* OrderStatus (CREATED, IN_PROGRESS, COMPLETED)

* BookingStatus (BOOKED, WORK_ORDER_CREATED, IN_PROGRESS, COMPLETED)

* PaymentType (CARD, SWISH, CASH)

* Uppdatera modellerna så att de använder enums istället för godtyckliga strängar.

### Steg 4: Separera Presentation (UI) från Affärslogik

* Rensa bort System.out.println() från GarageSystem.

* Låt affärsmetoderna returnera objekt, boolean eller kasta egna exceptions vid fel (t.ex. EntityNotFoundException, MechanicUnavailableException).

* Flytta all meny- och utskriftslogik till en dedikerad ConsoleUI- eller ConsolePrinter-klass.

### Steg 5: Dela upp ansvarsområden i gruppen (Parprogrammering / Modulvis)

För att alla i gruppen ska kunna arbeta parallellt utan merge-konflikter:

* **Person 1 (t.ex. Datalager & Repositories):**

* Skapa repository-interface (CustomerRepository, WorkOrderRepository, etc.).

* Flytta datahanteringen från statiska Database till instansierade repositories.

* **Person 2 (t.ex. Betalningsmodul & Strategy/Adapter):**

* Implementera PaymentStrategy-gränssnittet.

* Skapa CardPaymentStrategy, SwishPaymentStrategy, CashPaymentStrategy.

* Integrera med PaymentService.

* **Person 3 (t.ex. Fakturering & Rabattmodul):**

* Skapa DiscountStrategy och bryt ut VIP- och kod-rabatter.

* Hantera beräkningar i en dedikerad BillingService.

* **Person 4 (t.ex. Fasad, Händelser & UI):**

* Bygg GarageFacade som koordinerar tjänsterna mot Main.

* Bygg ett enkelt händelsesystem / Observer för notifieringar (orderstatus och betalningsbekräftelse).

### Steg 6: Slutgranskning och Dokumentation

* Kör alla tester och verifiera att konsolapplikationen fungerar smidigt och felfritt.

* Rita ett uppdaterat UML-klassdiagram över den nya arkitekturen och dokumentera vilka designmönster ni har valt och varför.
