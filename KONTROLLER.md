# Kontroller och säkerhetsåtgärder

Vad som skyddas i koden, vilka kontroller som körs på det, och vad körningen gav.
En post per tillfälle, nyaste överst. Siffrorna är mätta, inte uppskattade.

---

## 2026-10-02 · PR #70 och #72, mergade till develop (`d3230ef`)

Körd samma dag på JDK 8 (`jdk8u504-full`), `./test.sh`. Siffrorna är från körningen efter PR #72;
före den var summan 110/110 och säkerhetsområdet 4/4.

| Område | Resultat |
|---|---|
| [1/6] Smoketest — schema, klassladdning, resurser, uppstart | GODKÄND 4/4 |
| [2/6] Enhetstester — affärslogik, bokningar, scheman, mätetal, sök | GODKÄND 85/85 |
| [3/6] Jira beviskort (SCRUM-159, 165, 168–173) | GODKÄND 8/8 |
| [4/6] Kvalitetskontroll — språkparitet, temaintegritet, arkitektur | GODKÄND 4/4 |
| [5/6] Säkerhetskontroll — SQL-injektion, hemligheter, exekveringsskydd | GODKÄND 7/7 |
| [6/6] WCAG 2.1 AAA — kontrast, fokus, textstorlek | GODKÄND 5/5 |
| **Totalt** | **GODKÄND 113/113**, exit 0 |

CI (GitHub Actions, *Build, Test & Quality Audits*) grön (41 s för #70, 32 s för #72), merge-läge
CLEAN.

### Åtgärder i koden

**Regler nekar i tjänsten, inte i gränssnittet.** Skydden fanns tidigare bara som
`canDelete*`-predikat i `ui/`, medan tjänstens delete-metoder raderade utan att fråga dem — regeln
gällde alltså bara om anropet råkade komma från formuläret. Nu nekar tjänsten, så samma svar gäller
varifrån anropet än kommer:

* `GarageSystem.deleteMechanic`, `deleteCustomer`, `deleteVehicle`, `deleteBooking` och
  `deleteServiceItem` — `refuseUnless(...)` med samma villkor som gränssnittet använder
  (`GarageSystem.java:335, 363, 407, 454, 496`).
* `BillingService.createInvoice` — en faktura per arbetsorder (`hasInvoiceFor`,
  `BillingService.java:72`). Förut gick det att fakturera samma utförda arbete två gånger.
* `WorkOrderService.markServicesAsCompleted` — kräver att arbetet har startat (`IN_PROGRESS`,
  `WorkOrderService.java:150`). Förut gick det att markera arbete som aldrig påbörjats som utfört.
* `PaymentService.processPayment` — en faktura på noll kronor, eller en betaltyp systemet inte
  känner igen, skapade förut en betalningsrad som aldrig kunde bli lyckad: fakturan stod kvar som
  obetald medan raderna samlades (`PaymentService.java:59`).

**Indata normaliseras där varje väg in passerar.** Registreringsnumret får samma skepnad i hela
registret — versaler och ett mellanslag mellan bokstäverna och siffrorna (`abc 123` → `ABC 123`,
medan testplåten `G2V001` lämnas orörd). Regeln ligger i `Vehicle`s konstruktor och setter, inte i
formuläret: formuläret gjorde `.trim().toUpperCase()` medan tjänsten sparade rakt av, så nästa väg
in kringgick regeln. `VehicleRepository.save` nekar dessutom ett nummer som redan tillhör ett annat
fordon och ett tomt nummer, och `Db.normalizeRegistrationNumbers` rättar rader som sparades innan
regeln fanns (idempotent, körs på egen anslutning efter att tabellerna skapats).

**Frysta priser.** `work_order_service_items.price` finns både när tabellen skapas och som migrering
(SCRUM-160), och fakturan läser det frysta priset först:
`CASE WHEN w.price IS NOT NULL THEN w.price ELSE s.price END`. En prisändring i katalogen kan
alltså inte ändra en genomförd arbetsorder eller en redan skickad faktura.

**Kontroller som kan falla.** `OverviewMetricsTest` innehöll fem kontroller som inte kunde bli röda:
`activeCount >= 0`, `activeCount <= orders.size()`, `revenue >= 0.0`, `available >= 0` och
`available <= mechanics.size()` — påståenden som är sanna för varje tänkbar indata, också för en
tom databas. Kontrollen såg ut att bevaka ekonomin utan att göra det. Den jämför nu antalet
pågående arbetsordrar mot en fråga i databasen, intäkten mot de betalda fakturorna och mekanikerns
tillgänglighet genom hela arbetsflödet. Samma sak gäller
`DataIntegrityTest`, som bevakar fyra felklasser som regler (referensintegritet, pengalogik,
tillståndsregler, i18n-paritet) i stället för de enskilda fall som en gång inträffade.

### Säkerhetsgranskningen (svitens område 5)

`SecurityAuditTest` skannar källkoden efter fyra saker:

* **Hårdkodade hemligheter:**
  `(?i)(api[_-]?key|secret[_-]?key|aws[_-]?secret|private[_-]?key|client[_-]?secret)\s*=\s*"[^"]{8,}"`
* **SQL-injektion:** `(?i)(select|insert|update|delete)\s+.*\+\s*[a-zA-Z0-9_]+` på en rad som
  samtidigt kör `executeQuery`, `executeUpdate` eller `prepareStatement`
* **Farlig processkörning:** `Runtime.exec`
* **Känsliga uppgifter i loggen:**
  `System.(out|err).print…(password|secret|creditcard|cvv|personnummer)`

Läget 2026-10-02: 7/7 godkända — inga hårdkodade hemligheter, inga SQL-injektionsmönster, ingen
farlig processkörning, ingen känslig loggning. Granskningen fällde en riktig rad samma dag: en
`DELETE` som byggdes med strängkonkatenering i testkoden
(`"DELETE FROM " + table + " WHERE id = " + id`) och som byttes mot en `PreparedStatement` med
parametrar.

### Dataflödesanalysen (PR #72)

Mönstren ovan tittar på en rad i taget. `DataFlowAuditTest` följer i stället **värdet**: den läser
kompilatorns eget träd och skiljer därför på `"SELECT * FROM " + TABELL` (konstant, ofarlig) och
`"SELECT * FROM " + namn` (data, farlig). En fråga som byggs på en rad och körs på en annan — som
mönstren inte ser — fångas. Både `String`-konkatenering, `String.format`, `valueOf`, `join` och
`StringBuilder.append` följs, fram till `executeQuery`, `executeUpdate`, `execute`,
`prepareStatement`, `prepareCall`, `addBatch`, `Runtime.exec` och `ProcessBuilder`.

Analysen har ett eget prov, `testTheAnalysisCatchesWhatThePatternMisses`: den matar in en farlig och
en ofarlig kodsnutt i minnet och kräver att den ena fälls och den andra lämnas i fred. Utan det
provet vet vi inte om analysen kan larma alls — och en granskning som aldrig larmar ser lika grön ut
som en ren kodbas.

*Gränser:* ingen typanalys (en sträng är en sträng) och ingen analys över metodgränser — ett värde
som byggs i en annan metod och skickas in följs inte. Regeln är därför att ett värde är smittat när
det **byggts** av data, inte för att det kommer in i en metod; annars hade varje hjälpmetod som tar
en fråga som parameter larmat, och en granskning som larmar fel blir avstängd.

**Känd begränsning, sammanfattad:** reglerna är mönsterbaserade och analysen saknar typanalys och
interproceduranalys. Tillsammans täcker de den vanliga formen av misstaget, inte varje variant — de
ersätter inte en genomläsning av den som skriver koden.

### Så körs kontrollerna

```bash
./test.sh                 # hela sviten, sex områden
./test.sh --smoke         # databasschema, klassladdning, resurser, uppstart
./test.sh --unit          # enhetstesterna
./test.sh --bevis         # Jira-beviskorten
./test.sh --quality       # språkparitet, temaintegritet, arkitektur
./test.sh --security      # säkerhetsgranskningen
./test.sh --wcag          # WCAG 2.1 AAA
```

Samma skript finns för Windows (`test.ps1`, `test.bat`). Filen `test.ps1` sparas med UTF-8 BOM —
utan den läser PowerShell 5.1 å/ä/ö fel. Windows-versionen kör samma kontroller; testklasserna
hittas automatiskt i paketet.

Dataflödesanalysen läser kompilatorns eget träd, och de klasserna ligger i JDK:ns `lib/tools.jar`.
Skripten lägger filen på klassökvägen när den finns. Får du `package com.sun.source does not exist`
kör du ett JRE i stället för ett JDK — se felsökningsavsnittet i `Audit_Readme.md`.
