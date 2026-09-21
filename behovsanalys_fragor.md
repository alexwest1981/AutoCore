# Behovsanalys & UI-Kravfångst: Wigell AutoCore
**Dokumenttyp:** Krav- och behovsanalys inför gränssnitts- och systemdesign  
**Målgrupp:** Utvecklingsteamet (Grupp C: Alex, Lucas, Daniel, Vivianne), Kursansvarig / Lärare (Produktägare)  
**Utgångspunkt:** Originalkodbasen (`src/Main.java`, `GarageSystem.java`, `Database.java` och domänmodellerna)

---

## 1. Bakgrund & Syfte

När vi analyserar den ursprungliga koden i **Wigell AutoCore** (den textbaserade CLI-menyn med 17 val, `GarageSystem` som god-class samt `System.out.println`-baserade mockar) blir det uppenbart att koden representerar en ren **funktionsprototyp** (Proof of Concept). Den visar *vilka* datastrukturer som finns, men lämnar stora glapp kring *hur* en verklig verkstad faktiskt arbetar.

I modern systemarkitektur och agil metodik är användargränssnittet inte bara ett "skal", utan den primära drivkraften för:
1. **Domänlogikens utformning:** Vilka händelser triggas, vilka tillstånd är tillåtna och vilken validering krävs.
2. **Arkitektoniska mönster:** Hur separation of concerns (SoC), händelsehantering (Observer), tillståndshantering (State) och behörigheter (RBAC) motiveras.
3. **Användbarhet & Säkerhet:** Att minimera mänskliga misstag, skydda känslig data och effektivisera det dagliga arbetet.

Detta dokument samlar **alla tänkbara frågor, verksamhetskrav och gränssnittsbehov** som originalkoden väcker. Syftet är att använda dessa frågor som diskussionsunderlag med läraren/produktägaren samt som grund för gruppens arkitekturval och UI-design.

---

## 2. Användarroller & Verksamhetsmiljö (Personas)

Originalkoden har en enda gemensam meny (`Main.java`) där vem som helst kan utföra alla 17 moment. I en verklig verksamhet inom *Wigell Group* finns flera distinkta roller med helt olika behov:

### Frågor kring Roller & Behörigheter (RBAC)
1. **Vilka är de primära användarna av systemet?**
   - *Kundmottagare / Servicerådgivare:* Behöver snabbt kunna söka, boka in kunder över disk/telefon, registrera fordon och ta betalt.
   - *Verkstadstekniker / Mekaniker:* Behöver en ren arbetsöversikt, instruktioner, tidsrapportering och möjlighet att flagga för tilläggsarbete.
   - *Verkstadschef / Verkmästare:* Behöver resursplanering, beläggningsgrad, mekanikernas tillgänglighet och ekonomiska nyckeltal (KPI).
   - *Ekonom / Fakturaadministratör:* Behöver kontroll över obetalda fakturor, påminnelser, rabattgodkännanden och bokföringsunderlag.
   - *Slutkund (Bilägaren):* Ska kunden ha någon form av självbetjäning (webb/mobil för bokning och betalning), eller är systemet 100% internt?
2. **Vilken hårdvara och fysisk miljö används?**
   - Körs systemet på en stationär PC i en ren kundmottagning, eller på en smuts- och stöttålig surfplatta/touchskärm ute i verkstadshallen med handskar?
   - Behöver UI ha stora touch-ytor, hög kontrast eller ett dedikerat "verkstadsläge" (Dark Mode/High Contrast)?
3. **Behörighetsstyrning i UI:**
   - Ska mekanikern kunna se fakturabelopp, marginaler och andra kunders personuppgifter?
   - Ska en kundmottagare få radera eller modifiera en påbörjad arbetsorder utan chefsattest?
   - Ska alla kunna dela ut 10% rabattkoder, eller krävs administratörsrättighet?

---

## 3. Domän- och Flödesspecifika Behovsfrågor

Nedan bryts originalkodens 16 menyval och logik ned i konkreta behovs- och gränssnittsfrågor.

---

### 3.1 Kunder & Fordon (CLI Meny 1–4)
*Originalkod: `createCustomer(name, phone, email)`, `createVehicle(reg, brand, model, year, customerId)`.*

#### Gränssnitts- och Interaktionskrav
* **Hur söker personalen upp en kund?**
  - I CLI var man tvungen att lista *alla* kunder (`showCustomers()`) och läsa ett numeriskt `customerId`. I verkligheten ringer kunden och säger sitt efternamn, regnummer eller telefonnummer.
  - *Fråga:* Behöver UI ha ett globalt snabbsökfält som söker direkt på delsträngar av namn, telefonnummer, e-post eller registreringsnummer?
* **Hur hanteras registrering av fordon i förhållande till kund?**
  - Kan en kund äga flera fordon (t.ex. familjebilar eller företagsflotta)?
  - Ska man kunna registrera ett fordon direkt i samma vy som kunden skapas (guidad wizard), eller måste de göras i två separata steg?
  - Vad händer om ett fordon säljs eller byter ägare? Ska fordonet kunna kopplas om till en ny kund med bevarad reparationshistorik?

#### Validering & Dataintegritet
* **Formatvalidering i gränssnittet:**
  - Hur ska registreringsnummer valideras? Stöd för både klassiska svenska format (`ABC 123`) och nyare format (`ABC 12D`), samt utländska fordon?
  - Ska telefonnummer och e-postadresser valideras i realtid i inmatningsfältet med tydlig felindikering (inline error messaging)?
  - Hur hanteras dubbletter? Ska systemet varna om en kund med samma e-post/telefon eller ett fordon med samma registreringsnummer redan finns?

#### Affärsregler & GDPR
* **VIP-status:**
  - I `Customer.java` finns attributet `vip` (boolean). Vem sätter denna status? Sätts den manuellt med en knapp i kundkortet, eller baseras den automatiskt på hur mycket kunden handlat för under året?
* **GDPR & Radering:**
  - Vad händer om en kund begär att bli borttagen ur systemet? Får kunden raderas om det finns historiska fakturor och bokföringsunderlag kopplade till personen (vilket bryter mot Bokföringslagen)? Ska kunden anonymiseras istället?

---

### 3.2 Tidsbokning & Mottagning (CLI Meny 5–6)
*Originalkod: `createBooking(vehicleId, date, description)`.*

#### Planering & Tidsdimension
* **Klockslag och tidsluckor:**
  - I originalkoden anges bara ett `LocalDate` (t.ex. `2026-09-20`). Ingen tid på dygnet registreras!
  - *Fråga:* Hur bokar en verkstad i verkligheten? Lämnas bilen kl 07:00 och hämtas 16:30, eller finns det specifika bokningsbara tidsluckor (t.ex. kl 10:00–11:30)?
* **Kalendervy vs Tabellvy:**
  - Behöver användaren en grafisk kalender (dag/vecka/månad) i stil med Outlook/Google Calendar för att se verkstadens lediga kapacitet, eller räcker en tabellista?
  - Hur ser personalen vilka dagar som är fullbokade kontra lediga innan de försöker registrera en ny bokning?

#### Bokningsflöde & Ändringar
* **Koppling till felbeskrivning och önskad tjänst:**
  - Ska kunden/rådgivaren kunna välja preliminära tjänster redan vid bokningstillfället (t.ex. "Årlig service" + "Byte av torkarblad") så att beräknad tidsåtgång kan reserveras i kalendern?
* **Livscykel & Avbokning:**
  - Kan en bokning redigeras (ändra datum, fordon eller beskrivning)?
  - Hur hanteras avbokningar eller "No-shows" (kunden dök inte upp)? Vilka statusar behövs förutom `BOOKED`?

---

### 3.3 Mekaniker & Resursfördelning (CLI Meny 7–8)
*Originalkod: `showMechanics()`, `available` flagga, `specialization`.*

#### Resurshantering & Schemaläggning
* **Vad innebär egentligen `available`?**
  - I originalkoden sätts `mechanic.setAvailable(false)` så fort en arbetsorder startas, och `true` när den avslutas.
  - *Fråga:* I verkligheten kan en mekaniker ha ett arbete som tar 3 dagar. Betyder det att mekanikern inte kan tilldelas några andra jobb alls under dessa 3 dagar?
  - Hur hanteras arbetstider, luncher, raster, sjukfrånvaro och semester i UI?
* **Kompetens och specialiteter:**
  - I `Mechanic.java` finns `specialization` (t.ex. "Brakes", "Diagnostics").
  - *Fråga:* Ska UI förhindra att en mekaniker tilldelas en elbil eller en automatväxellåda om hen saknar certifiering/specialitet för detta? Ska lämpligaste mekaniker föreslås automatiskt i en dropdown?
* **Kan flera mekaniker samarbeta?**
  - Kan en arbetsorder tilldelas ett team eller två mekaniker (t.ex. motorlyft), eller är relationen strikt 1 order $\rightarrow$ 1 mekaniker?

---

### 3.4 Tjänster & Priskatalog (CLI Meny 7)
*Originalkod: `ServiceItem(name, description, price, estimatedMinutes)`.*

#### Prissättning & Faktisk tidsåtgång
* **Fastpris kontra Löpande räkning:**
  - I koden har varje tjänst ett fast pris och en estimerad tidsåtgång. Vad händer om ett arbete beräknat till 60 minuter tar 180 minuter på grund av fastrostade bultar?
  - Ska mekanikern kunna rapportera *faktisk tid* i UI, och ska systemet kunna debitera timtaxa utöver eller istället för fastpris?
* **Reservdelar & Förbrukningsmaterial:**
  - Originalkoden har endast `ServiceItem` (arbetskostnad/tjänst). Var läggs reservdelar in (t.ex. 4 liter syntetolja, bromsklossar, oljefilter)?
  - Behöver systemet ha en artikelkatalog / lagerhantering för fysiska reservdelar med artikelnummer, inköpspris, utpris och lagersaldo?
* **Dynamiska tillägg:**
  - Ska servicerådgivaren eller mekanikern kunna lägga till en ad-hoc-tjänst med valfritt namn och pris direkt i en order, utan att den måste finnas i den fasta katalogen?

---

### 3.5 Arbetsordrar & Verkstadsprocess (CLI Meny 9–12)
*Originalkod: `createWorkOrder(bookingId, mechanicId, serviceItemIds)`, `startWorkOrder()`, `completeWorkOrder()`.*

#### Inmatning & Interaktion
* **Tjänsteurval i UI:**
  - I CLI angavs tjänster som text: `1,3,4`. Detta är extremt felkänsligt och omodernt.
  - *Fråga:* Hur ska tjänster väljas i UI? En multiselect-dropdown, en sökbar checklista med priser och summor i realtid, eller "drag-and-drop"?
* **Tydlig orderstatus och Kanban-översikt:**
  - Skulle verkstaden ha nytta av en **Kanban-vy** (kolumner: *Mottagna / Skapade* $\rightarrow$ *Pågående* $\rightarrow$ *Väntar på delar* $\rightarrow$ *Klara*) där mekanikern eller verkmästaren kan dra kort mellan kolumner?

#### Avvikelser & Tilläggsarbete
* **Oförutsedda fel (Verkstadens vanligaste scenario):**
  - Bilen togs in för bromsbyte, men mekanikern upptäcker att hjullagret är helt slut och måste bytas.
  - *Fråga:* Hur hanteras detta i gränssnittet?
    1. Kan mekanikern pausa ordern och flagga den som `PAUSED / WAITING_APPROVAL`?
    2. Kan servicerådgivaren generera en tilläggsoffert direkt från ordern och skicka till kunden via SMS för godkännande med BankID/klick?
* **Mekanikerns arbetsanteckningar:**
  - Var dokumenterar mekanikern vad som faktiskt gjorts och vad som upptäckts (viktigt för bilens servicebok och eventuella framtida reklamationer)?

---

### 3.6 Fakturering & Rabattsystem (CLI Meny 13–14)
*Originalkod: `createInvoice(workOrderId, discountCode)`, 10% VIP, `WELCOME10`, `SERVICE200`.*

#### Regler kring Rabatter
* **Hårdkodade kampanjkoder vs Administrativt gränssnitt:**
  - I originalkoden ligger `WELCOME10` (10%) och `SERVICE200` (200 kr) hårdkodade i en `if/else`-sats i `GarageSystem`.
  - *Fråga:* Ska verkstadschefen eller ekonomiavdelningen kunna skapa, redigera, tidsbegränsa och radera kampanjkoder via ett eget administrativt formulär i UI?
* **Kombinerade rabatter:**
  - Kan en VIP-kund (10%) *dessutom* använda en kampanjkod (`WELCOME10` eller `SERVICE200`), eller är rabatter icke-kumulativa? Hur ska UI tydligt visa för kunden och personalen hur slutbeloppet räknats fram?
* **Manuell prisjustering & Goodwill:**
  - Kan en servicerådgivare ge en manuell rabatt ("avrundat pris" eller "goodwill-reparation")? Krävs det en skriftlig motivering i systemet för detta?

#### Moms, Skatter & Fakturaunderlag
* **Momsspecifikation (25%):**
  - Originalkoden räknar bara ett rent totalbelopp i flyttal (`double`).
  - *Fråga:* En godkänd svensk faktura måste särredovisa moms (25%), belopp exkl. moms och belopp inkl. moms. Ska detta synas i fakturavyn och på utskriften?
* **Fakturalayout & Export:**
  - Ska fakturan kunna förhandsgranskas som PDF, skrivas ut på papper, eller exporteras till ett externt bokföringssystem (t.ex. Fortnox eller Visma via SIE4-fil)?
* **Kreditering:**
  - Vad händer om en faktura skapas med fel belopp eller tjänster? Kan den makuleras/krediteras i UI, eller skapas en separat kreditfaktura?

---

### 3.7 Betalningar & Kassaflöde (CLI Meny 15–16)
*Originalkod: `processPayment(invoiceId, paymentType)`, mock för `CARD`, `SWISH`, `CASH`.*

#### Betalningsmetoder & Integrationer
* **Verkliga integrationer:**
  - Hur ska betalningen initieras i UI?
    - *Kort:* Ska UI skicka beloppet till en ansluten betalterminal (Nets, Zettle, Bambora) och invänta "Godkänd" via webhook/callback?
    - *Swish:* Ska systemet visa en dynamisk QR-kod på skärmen som kunden scannar med sin Swish-app, alternativt skicka en Swish-begäran till kundens mobilnummer?
    - *Faktura (Kredittid / 30 dagar):* Ska kunden kunna ta med sig bilen och betala fakturan via bankgiro inom 30 dagar? Hur görs kreditkontroll i systemet?
* **Kontanthantering & Skatteverkets krav:**
  - Originalet tillåter kontantbetalning (`CASH`). Enligt svensk lag krävs en certifierad kontrollenhet (svarta lådan) och ett godkänt kassaregister vid kontant betalning. Är systemet ett komplett kassasystem (POS) eller ett administrativt affärssystem?
* **Delbetalning:**
  - Kan en kund betala en del med Swish (t.ex. 1 000 kr) och resten med kort eller kontant? Hur ska UI hantera restbelopp på en faktura?
* **Kvitto:**
  - Ska kunden få kvitto utskrivet, skickat via e-post eller på SMS?

---

### 3.8 Notifieringar & Kommunikation med Kunden
*Originalkod: `System.out.println("Sending invoice notification to customer...")` & `("Sending payment confirmation...")`.*

#### Kommunikationskanaler & Automatik
* **Vilka kanaler ska användas?**
  - Ska notifieringar gå via SMS, E-post eller båda?
  - Ska systemet använda en extern leverantör (t.ex. Twilio för SMS, SendGrid för e-post)?
* **Vilka händelser ska trigga notifieringar?**
  1. *Bokningsbekräftelse:* När bokning skapas.
  2. *Påminnelse:* 24 timmar innan inlämning.
  3. *Arbete påbörjat:* "Nu rullar din Volvo in i verkstaden."
  4. *Godkännandebehov:* "Vi upptäckte slitna bromsklossar, kostnad 1 200 kr. Klicka här för att godkänna."
  5. *Bilen klar för hämtning:* "Din bil är klar! Fakturabelopp: 3 450 kr."
  6. *Betalningsbekräftelse / Kvitto:* Direkt efter genomförd betalning.
* **Historik & Status i UI:**
  - Kan personalen se i kundkortet vilka SMS och mail som har skickats till kunden och om de kommit fram?

---

## 4. UI/UX, Informationsarkitektur & Ergonomi

### 4.1 Dashboard & Förstasida (Översiktsvyn)
* **Vad är det allra första en användare behöver se när systemet öppnas på morgonen?**
  - KPI-kort: Hur många ordrar är pågående? Vad är dagens beräknade fakturering?
  - Dagens ankommande bilar (vilka kunder ska lämna in nyckeln under morgonen?).
  - Dagens bilar som ska lämnas ut (vad måste vara klart till kl 16:00?).
  - Varningar/Flaskhalsar: Finns det försenade ordrar eller mekaniker som saknar tilldelade jobb?

### 4.2 Sökbarhet & Tabellinteraktion
* **Sök- och filterfunktionalitet:**
  - Ska sökfältet filtrera tabellerna i realtid allt eftersom användaren skriver?
  - Behövs snabbfilter (t.ex. knappar för *"Endast aktiva"*, *"Mina tilldelade ordrar"*, *"Obetalda fakturor"*)?
* **Sortering & Paginering:**
  - Kan tabeller sorteras på datum, regnummer, pris och mekaniker genom att klicka på kolumnrubrikerna?
  - Vad händer om systemet innehåller 5 000 historiska ordrar? Krävs paginering (t.ex. 25 rader per sida) eller "infinite scroll"?

### 4.3 Dialoger, Modaler & Felhantering
* **Modaler kontra separata sidor:**
  - Bör "Skapa kund", "Ny bokning" och "Ta betalt" ligga i popup-fönster (modaler) så användaren inte tappar kontexten, eller på separata undersidor?
* **Förebyggande av fel (Defensive UI):**
  - Knappar som "Starta order" eller "Ta betalt" bör vara inaktiva (`disabled`) om inte en giltig rad i tabellen är markerad.
  - Varningsdialoger: Krävs bekräftelse innan en order slutförs, raderas eller en faktura skapas ("Är du säker på att du vill slutföra order #4?")?

---

## 5. Icke-funktionella krav (NFR) & Arkitektoniska konsekvenser

Frågorna kring UI och verksamhet är direkt avgörande för hur systemets arkitektur måste konstrueras bakom kulisserna:

| Verksamhetsfråga / UI-behov | Arkitektonisk konsekvens i kodbasen | Lämpligt GoF-mönster / Arkitekturprincip |
| :--- | :--- | :--- |
| **Olika betalsätt (Kort, Swish, Faktura)** | Betalningslogiken kan inte ligga i en `if/else`-sats i `GarageSystem`. Varje betalsätt måste vara en självständig klass. | **Strategy Pattern** (eller **Adapter Pattern** mot externa API:er). |
| **Olika rabattregler (VIP, Kampanjer, Säsong)** | Rabattberäkningen måste kunna utökas utan att ändra befintlig faktureringskod (Open/Closed Principle). | **Strategy Pattern** för rabattberäkning. |
| **Automatiserade notifieringar vid statusbyte** | När en arbetsorder ändrar status ska SMS/E-post skickas automatiskt utan hård koppling i orderkoden. | **Observer Pattern** (Publish/Subscribe händelsesystem). |
| **Arbetsorderns faser (Skapad, Pågående, Pausad, Klar)** | Tillåtna operationer beror på orderns nuvarande tillstånd. Förhindra ogiltiga anrop. | **State Pattern** eller strikta **Enums** med tillståndsövergångar. |
| **Flera användare & Permanent datalagring** | Statiska listor i `Database.java` försvinner vid omstart och förhindrar testning. Kräver abstraktion mot databas/fil. | **Repository Pattern** med interface (`CustomerRepository` etc.). |
| **Slankt & frikopplat UI mot affärslogik** | UI ska aldrig prata direkt med databaser eller göra beräkningar. All interaktion ska gå via en enhetlig ingång. | **Facade Pattern** (`GarageFacade`) och **MVC / MVVM**. |
| **Komplex skapandeprocess av arbetsordrar** | Skapa en order med bokning, kund, flera tjänster och mekaniker kräver stegvis validering. | **Builder Pattern** eller **Factory Pattern**. |

---

## 6. Sammanfattande Checklista: Topp 10 frågor till läraren

Följande koncentrerade frågebatteri kan teamet ställa direkt till kursansvarig/lärare vid nästa avstämning för att bekräfta omfattning och förväntningar:

1. **Användarroller & Omfattning:**  
   *Är systemet tänkt att användas av en enda generell administratör (som i original-CLI:t), eller vill du att vi modellerar och bygger med separata roller (receptionist, mekaniker, ekonom/chef)?*
2. **Bokningens granularitet:**  
   *I originalkoden finns bara ett datum utan klockslag för bokningar. Förväntas vi införa tidsluckor/klockslag och kalenderöverblick, eller räcker datumhantering?*
3. **Mekanikernas tillgänglighet:**  
   *I koden blir en mekaniker helt låst (`available = false`) vid start av order. Ska mekanikern kunna schemaläggas, hantera flera jobb, eller matcha specialiteter mot tjänster?*
4. **Artiklar och Reservdelar:**  
   *Systemet hanterar just nu endast tjänster (`ServiceItem`). Förväntas vi även hantera fysiska reservdelar och förbrukningsmaterial på arbetsordrar och fakturor?*
5. **Avvikelser under arbete:**  
   *Ska en arbetsorder kunna pausas, ändras eller utökas med nya moment om mekanikern upptäcker oväntade fel under reparationen?*
6. **Rabatter och Kampanjhantering:**  
   *Skall rabattregler (VIP, kampanjkoder) vara dynamiskt konfigurerbara via gränssnittet, eller räcker det att arkitekturen stödjer det via Strategy Pattern?*
7. **Moms och Svenska Fakturakrav:**  
   *Ska faktureringsmodulen räkna med och specificera svensk moms (25%), samt hantera delbetalningar och krediteringar?*
8. **Notifieringar (SMS/E-post):**  
   *Räcker det att notifieringar sköts via ett arkitektoniskt händelsemönster (Observer Pattern) med mockade loggutskrifter, eller förväntas riktiga API-anrop?*
9. **Kassalagstiftning och Kontanter:**  
   *Ska `CASH`-betalningar behandlas som en enkel simulering, eller behöver vi beakta krav på kvitto och kassaregister?*
10. **Persistens & Datalagring:**  
    *Är kravet in-memory repositories (lämpligt för enhetstester), eller förväntas vi spara till fil (JSON/XML) eller en SQL-databas?*
