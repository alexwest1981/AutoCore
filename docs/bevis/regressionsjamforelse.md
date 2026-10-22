# E5-2 Regressionsjämförelse mot baslinjen

SCRUM-309 och SCRUM-342. Körd 21 oktober 2026 mot `develop` i `c6a2872`. Utskriften för
`StatusRoutesProof` är omkörd 22 oktober efter SCRUM-326.

## Vad som jämförs

E5-1 (SCRUM-292) spelade in utskrifterna från elva prov på Java 8, på commit
[`b0ef2b9`](https://github.com/alexwest1981/AutoCore/commit/b0ef2b9), den sista innan
Maven-bygget mergades in. Baslinjens egen README skriver `ce1fde8`, vilket är samma commit
före historieomskrivningen samma kväll.

De elva proven är:

```
BookingChangeProof      NumberRulesProof
CancelledOrdersProof    OldDatabaseProof
DraftConfirmationProof  RuleViolationProof
ErrorBoundaryProof      StatusRoutesProof
FlowProof               WorkOrderProof
                        WorkOrderRowsProof
```

Baslinjens filer låg i `bevis/`, som togs ur git i `de88b90`. De finns kvar i historiken
och är hämtade därifrån:

```
git show bbe77eb:bevis/   → 12 filer
```

Utskrifterna från båda sidorna ligger nu i repot: [baslinje-java8/](baslinje-java8/) och
[utskrifter-4.0/](utskrifter-4.0/), med argumenten per prov i
[baslinje-java8/README.txt](baslinje-java8/README.txt).

## Så lästes skillnaderna

Utskrifterna skiljer sig åt redan innan innehållet granskas, av två skäl som inte har med
saken att göra:

- `Databas redo: <sökväg>` skrevs med `System.out` i baslinjen och är en loggrad nu.
- Loggrader från `WorkOrderService` (införda i E4-2, `813e90e`) blandas i utskriften, och
  JVM:en varnar för `System::load` när sqlite-drivrutinen laddas.

Jämförelsen är därför gjord på utskrifter där loggrader, JVM-varningar och tomma rader är
borttagna, och där sökvägen till databasen är utbytt mot `<db>`. Inget annat är filtrerat.

## Resultat

| Prov | Utfall | Skillnad |
|---|---|---|
| BookingChangeProof | samma | ingen |
| CancelledOrdersProof | samma | ingen |
| DraftConfirmationProof | samma | ingen |
| ErrorBoundaryProof | samma | ingen |
| FlowProof | samma | ingen |
| NumberRulesProof | samma | ingen |
| OldDatabaseProof | samma svar | `schema_version` 3 → 4, `users=3` tillkommer |
| RuleViolationProof | samma | ingen |
| WorkOrderProof | samma svar | utkastets nummer 20 → 7 |
| WorkOrderRowsProof | samma svar | utkast 26 och 27 → 7 och 8 |
| StatusRoutesProof | samma svar | ingen, efter åtgärden i SCRUM-326 |

Tio av elva är identiska eller skiljer sig bara i sådant som följer av en annan databas.
Den elfte avvek när jämförelsen gjordes, av ett skäl som inte hörde till plattformsbytet,
och är åtgärdad i SCRUM-326.

## Vad skillnaderna kommer av

### OldDatabaseProof: schema_version 3 → 4 och users=3

Migrering `004_users.sql` (E3-1, `SCRUM-301`) tillkom efter baslinjen. Allt annat i
sammanfattningen är oförändrat, inklusive verksamhetsdatan:

```
customers=6  vehicles=6  bookings=6  work_orders=6  mechanics=3
service_items=4  booking_service_items=7  work_order_service_items=7
```

Det är det närmaste man kommer ett bevis för WAC4_0 §9: den gamla databasen lyfts genom
4.0 och innehållet står kvar. Att `users` tillkommer är meningen, kontona behövs för E3.

### WorkOrderProof och WorkOrderRowsProof: numren

Baslinjen kördes mot `/tmp/bas.db`, en kopia av dåtidens `data/autocore.db`, som hade
minst 19 arbetsordrar. Kopian låg i `/tmp` och finns inte kvar, och dagens
`data/autocore.db` är en färsk seedning med 6 arbetsordrar. Proven har därför körts mot
en kopia av den färska databasen, och nästa arbetsorder får nummer 7 i stället för 20.

Allt annat i utskrifterna är ordagrant detsamma: samma statusbyten, samma mekaniker,
samma radinnehåll i `WorkOrderRowsProof`, inklusive de fyra `true`-raderna på slutet. Att
numret skiljer sig är en följd av indata, inte av koden.

### StatusRoutesProof: drop-in-vägen prövas nu

```
baslinjen:
  drop-in: [IN_PROGRESS nej, CONFIRMED ja, COMPLETED nej, ...]
4.0, den 21 oktober:
  drop-in: ingen ledig tid kvar i dag, vägen prövas inte i den här körningen
```

Provet skapade en drop-in med `createDropInBooking`, som tog den aktuella timmen samma
dag. Körningen gjordes 20:13, efter stängning, och då svarar tjänsten med ett regelbrott
och provet hoppade över vägen. Baslinjen spelades in på dagen och fick med alla fyra
vägarna.

De tre vägar som prövades svarade exakt som i baslinjen, och `alla vägar svarar lika: true`
gällde fortfarande. Avvikelsen var alltså inte en regression i koden, utan att provet var
beroende av när på dygnet och vilken veckodag det körs.

SCRUM-326 tar bort beroendet. `createDropInBooking` har fått en överlagring som tar emot
dag och timme, så provet kan be om en tid som ligger inom öppettiderna i stället för att
hoppas på att klockan råkar vara rätt. Överlagringen går genom samma spärr som den gamla
signaturen. Provet använder onsdagen den 3 mars 2027 och klockan 08:00, räknar de vägar som
prövats och avslutar med felkod om det inte är fyra.

Omkörd utskrift, `/tmp/bas.db` utbytt mot en kopia av dagens databas:

```
Samma statusbyten på 4 vägar: [bokning, drop-in, reklamation, utkast]
bokning: [IN_PROGRESS nej, CONFIRMED ja, COMPLETED nej, IN_PROGRESS ja, CANCELLED ja, IN_PROGRESS ja, COMPLETED ja, CANCELLED nej, CONFIRMED nej]
drop-in: [IN_PROGRESS nej, CONFIRMED ja, COMPLETED nej, IN_PROGRESS ja, CANCELLED ja, IN_PROGRESS ja, COMPLETED ja, CANCELLED nej, CONFIRMED nej]
reklamation: [IN_PROGRESS nej, CONFIRMED ja, COMPLETED nej, IN_PROGRESS ja, CANCELLED ja, IN_PROGRESS ja, COMPLETED ja, CANCELLED nej, CONFIRMED nej]
utkast: [IN_PROGRESS nej, CONFIRMED ja, COMPLETED nej, IN_PROGRESS ja, CANCELLED ja, IN_PROGRESS ja, COMPLETED ja, CANCELLED nej, CONFIRMED nej]

alla vägar svarar lika: true
alla fyra vägarna prövades: true
```

Raden för drop-in är ordagrant baslinjens, och rubriken namnger de vägar som prövas i
stället för att säga "de fyra" oavsett utfall. Två körningar i rad ger samma svar; endast
tidsstämplar och databasens filnamn skiljer.

## Vad det här säger om kraven

WAC4_0 §9 kräver att befintlig information bevaras, att tidigare registrerade kunder,
fordon och arbetsorder fortfarande kan användas, att affärsreglerna gäller och att
valideringsreglerna fungerar. §11 B kräver att tidigare funktioner fungerar och att
befintlig information finns kvar.

Elva av elva prov svarar lika, eller skiljer sig bara där indata gör det. Ingenting i
jämförelsen visar att funktionalitet försvunnit i plattformsbytet.
