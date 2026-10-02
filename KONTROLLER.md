# Kontroller och säkerhetsåtgärder

Här står vad koden skyddar, vilka kontroller som körs på den, och vad körningen gav. En post per
tillfälle, nyaste överst. Siffrorna är mätta, inte uppskattade.

---

## 2026-10-02 · PR #70 och #72, mergade till develop (`d3230ef`)

Kört samma dag på JDK 8 (`jdk8u504-full`) med `./test.sh`. Tabellen är körningen efter PR #72. Före
den var summan 110/110 och säkerhetsområdet 4/4.

| Område | Resultat |
|---|---|
| [1/6] Smoketest — schema, klassladdning, resurser, uppstart | GODKÄND 4/4 |
| [2/6] Enhetstester — affärslogik, bokningar, scheman, mätetal, sök | GODKÄND 85/85 |
| [3/6] Jira beviskort (SCRUM-159, 165, 168–173) | GODKÄND 8/8 |
| [4/6] Kvalitetskontroll — språkparitet, temaintegritet, arkitektur | GODKÄND 4/4 |
| [5/6] Säkerhetskontroll — SQL-injektion, hemligheter, exekveringsskydd | GODKÄND 7/7 |
| [6/6] WCAG 2.1 AAA — kontrast, fokus, textstorlek | GODKÄND 5/5 |
| **Totalt** | **GODKÄND 113/113**, exit 0 |

Efter den körningen har sviten vuxit till 115 tester, när kontrollen som jämför dokumentsiffran med
sviten tillkom. Den posten ligger längst ner.

CI (GitHub Actions, *Build, Test & Quality Audits*) grön, 41 s för #70 och 32 s för #72.

### Vad som ändrades i koden

Skydden låg tidigare bara i gränssnittet, som `canDelete*`-predikat i `ui/`, medan tjänstens
delete-metoder raderade utan att fråga dem. Regeln gällde alltså bara när anropet råkade komma från
formuläret. Nu nekar tjänsten, och svaret blir detsamma varifrån anropet än kommer:

* `GarageSystem.deleteMechanic`, `deleteCustomer`, `deleteVehicle`, `deleteBooking` och
  `deleteServiceItem` använder `refuseUnless(...)` med samma villkor som gränssnittet
  (`GarageSystem.java:335, 363, 407, 454, 496`).
* `BillingService.createInvoice` tillåter bara en faktura per arbetsorder (`hasInvoiceFor`,
  `BillingService.java:72`). Förut gick det att fakturera samma utförda arbete två gånger.
* `WorkOrderService.markServicesAsCompleted` kräver att arbetet har startat (`IN_PROGRESS`,
  `WorkOrderService.java:150`). Förut gick det att markera arbete som aldrig påbörjats som utfört.
* `PaymentService.processPayment` skapade förut en betalningsrad för en faktura på noll kronor, och
  för en betaltyp systemet inte känner igen. Ingen av dem kunde någonsin bli lyckad, så fakturan
  stod kvar som obetald medan raderna samlades (`PaymentService.java:59`).

Registreringsnumret får samma skepnad i hela registret: versaler och ett mellanslag mellan
bokstäverna och siffrorna, så `abc 123` blir `ABC 123` medan testplåten `G2V001` lämnas orörd.
Regeln ligger i `Vehicle`s konstruktor och setter, inte i formuläret. Formuläret gjorde
`.trim().toUpperCase()` medan tjänsten sparade rakt av, så nästa väg in kringgick regeln.
`VehicleRepository.save` nekar dessutom ett nummer som redan tillhör ett annat fordon, och ett tomt
nummer. `Db.normalizeRegistrationNumbers` rättar rader som sparades innan regeln fanns; den är
idempotent och kör på egen anslutning efter att tabellerna skapats.

Priset fryses när arbetet utförs. `work_order_service_items.price` finns både när tabellen skapas
och som migrering (SCRUM-160), och fakturan läser det frysta priset först:
`CASE WHEN w.price IS NOT NULL THEN w.price ELSE s.price END`. En prisändring i katalogen kan
alltså inte ändra en genomförd arbetsorder eller en redan skickad faktura.

`OverviewMetricsTest` innehöll fem kontroller som inte kunde bli röda: `activeCount >= 0`,
`activeCount <= orders.size()`, `revenue >= 0.0`, `available >= 0` och
`available <= mechanics.size()`. Påståenden som är sanna för varje tänkbar indata, också för en tom
databas, så kontrollen såg ut att bevaka ekonomin utan att göra det. Nu jämför den antalet pågående
arbetsordrar mot en fråga i databasen, intäkten mot de betalda fakturorna, och mekanikerns
tillgänglighet genom hela arbetsflödet. `DataIntegrityTest` fick samma behandling: den bevakar fyra
felklasser som regler (referensintegritet, pengalogik, tillståndsregler, i18n-paritet) i stället för
de enskilda fall som en gång inträffade.

### Säkerhetsgranskningen (svitens område 5)

`SecurityAuditTest` skannar källkoden efter fyra saker:

* **Hårdkodade hemligheter:**
  `(?i)(api[_-]?key|secret[_-]?key|aws[_-]?secret|private[_-]?key|client[_-]?secret)\s*=\s*"[^"]{8,}"`
* **SQL-injektion:** `(?i)(select|insert|update|delete)\s+.*\+\s*[a-zA-Z0-9_]+` på en rad som
  samtidigt kör `executeQuery`, `executeUpdate` eller `prepareStatement`
* **Farlig processkörning:** `Runtime.exec`
* **Känsliga uppgifter i loggen:**
  `System.(out|err).print…(password|secret|creditcard|cvv|personnummer)`

Läget 2026-10-02: 7/7 godkända, alltså inga hårdkodade hemligheter, inga SQL-injektionsmönster,
ingen farlig processkörning och ingen känslig loggning. Granskningen fällde en riktig rad samma dag:
en `DELETE` som byggdes med strängkonkatenering i testkoden
(`"DELETE FROM " + table + " WHERE id = " + id`), och som byttes mot en `PreparedStatement` med
parametrar.

### Dataflödesanalysen (PR #72)

Mönstren ovan tittar på en rad i taget. `DataFlowAuditTest` följer i stället värdet. Den läser
kompilatorns eget träd, och kan därför skilja på `"SELECT * FROM " + TABELL` (konstant, ofarlig) och
`"SELECT * FROM " + namn` (data, farlig). En fråga som byggs på en rad och körs på en annan, vilket
mönstren inte ser, fångas. Analysen följer `String`-konkatenering, `String.format`, `valueOf`, `join`
och `StringBuilder.append`, fram till `executeQuery`, `executeUpdate`, `execute`,
`prepareStatement`, `prepareCall`, `addBatch`, `Runtime.exec` och `ProcessBuilder`.

Analysen har ett eget prov, `testTheAnalysisCatchesWhatThePatternMisses`. Det matar in en farlig och
en ofarlig kodsnutt i minnet och kräver att den ena fälls och den andra lämnas i fred. Utan provet
vet vi inte om analysen kan larma alls, och en granskning som aldrig larmar ser lika grön ut som en
ren kodbas.

Analysen har två gränser: ingen typanalys, för en sträng är en sträng, och ingen analys över
metodgränser, så ett värde som byggs i en annan metod och skickas in följs inte. Regeln är därför att
ett värde är smittat när det har byggts av data, inte för att det kommer in i en metod. Annars hade
varje hjälpmetod som tar en fråga som parameter larmat, och en granskning som larmar fel blir
avstängd.

Reglerna och analysen tillsammans täcker den vanliga formen av misstaget, inte varje variant. De
ersätter inte en genomläsning av den som skriver koden.

### Så kör du kontrollerna

```bash
./test.sh                 # hela sviten, sex områden
./test.sh --smoke         # databasschema, klassladdning, resurser, uppstart
./test.sh --unit          # enhetstesterna
./test.sh --bevis         # Jira-beviskorten
./test.sh --quality       # språkparitet, temaintegritet, arkitektur
./test.sh --security      # säkerhetsgranskningen
./test.sh --wcag          # WCAG 2.1 AAA
```

Samma skript finns för Windows (`test.ps1`, `test.bat`). Filen `test.ps1` sparas med UTF-8 BOM, för
utan den läser PowerShell 5.1 å/ä/ö fel. Windows-versionen kör samma kontroller; testklasserna hittas
automatiskt i paketet.

Dataflödesanalysen läser kompilatorns eget träd, och de klasserna ligger i JDK:ns `lib/tools.jar`.
Skripten lägger filen på klassökvägen när den finns. Får du `package com.sun.source does not exist`
kör du ett JRE i stället för ett JDK. Felsökningsavsnittet i `Audit_Readme.md` tar upp det.

## Var resultatet hamnar

En granskning som bara syns för den som körde den finns inte. Resultatet hamnar på sex ställen:

* **I terminalen**, direkt. `./test.sh` skriver `Status: GODKÄND (7/7 tester)` för säkerhetsområdet,
  en rad per test, och vid ett fall vad analysen hittade. Fil och radnummer står på samma rad, så
  raden syns i svitens utdrag i stället för att klippas bort.
* **`rapport.md`** i projektroten, vid en full körning. Filen är gitignorerad, alltså en lokal
  rapport per maskin, och följer inte med i repot.
* **CI-loggen** på GitHub Actions, vid varje push och pull request. Det är där en granskning som
  faller syns för någon annan än den som körde den.
* **`KONTROLLER.md`** (den här filen) och **`Audit_Readme.md`**: vad som kontrolleras, varför, och
  vad körningen gav.
* **`ACCEPTANSKRAV.md`**: AK-15:s rad namnger båda granskningarna och antalet tester, och rubriken
  överst säger hur många tester hela sviten ska ha.
* **Projektloggen i Obsidian-valvet**: en post per arbetstillfälle med vad som gjordes och mättes,
  vilka commits som gick in, och vad som är kvar.

Jira-tavlan får ingen post av sig själv. Analysen är ingen egen uppgift i sprinten utan hör till
AK-15 och till säkerhetsbeviset, så ska den synas där måste den skrivas in. Det är ett medvetet val:
tavlan beskriver arbetet, inte varje kontroll som bevakar det.
