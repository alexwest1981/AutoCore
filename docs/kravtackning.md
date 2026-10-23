# Kravtäckning — WAC 2.5 till 4.0

Fyra kravdokument har gåtts igenom mot koden, dokumenten och tavlan: `WAC2_5.pdf` (28 september),
`WAC3_0.pdf` (5 oktober), `WAC3_5.pdf` (13 oktober) och `WAC4_0.pdf` (19 oktober). 4.0 är det som
gäller; de tidigare är med för att deras funktionalitet ska finnas kvar, eftersom §9 i 4.0 kräver
att tidigare funktionalitet inte försvinner.

Kontrollerat 23 oktober 2026 mot `develop` i `cdb9571`, mot dokumenten i [`docs/`](README.md) och
mot tavlan, där alla 65 kort i sprint 6 står i Done.

Statusarna betyder: **uppfyllt**, **delvis** och **saknas**.

## WAC 4.0

### §2 Ny teknikplattform

| Krav | Status | Bevis |
|---|---|---|
| Spring Boot 4.x | uppfyllt | `pom.xml`: `spring-boot-starter-parent` 4.1.1 |
| Java 17 eller senare | uppfyllt | JDK 25, `maven-enforcer-plugin` kräver `[25,26)`. Varför golvet ligger över kravet står i [ADR-003](arkitekturbeslut.md#adr-003-jdk-25-är-bygggolvet-trots-att-kravet-säger-17-eller-senare) |
| Befintliga beroenden anpassade | uppfyllt | `sqlite-jdbc` och `json` hämtas av Maven i stället för att ligga i `lib/` |
| Tidigare funktionalitet bibehållen | uppfyllt | [`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md), elva prov mot Java 8-baslinjen |
| Byggas, startas och användas på nya plattformen | uppfyllt | [`start.sh`](../start.sh), `.github/workflows/ci.yml`, [README](../README.md) |
| Inkompatibiliteter identifierade och hanterade | uppfyllt | [inkompatibiliteter.md](inkompatibiliteter.md), nio rader med vad som hände, var det syntes och hur det hanterades |

### §3 Fristående verksamhetsfunktionalitet

| Krav | Status | Bevis |
|---|---|---|
| Minst två operationer från ett fristående Java-program | uppfyllt | `StandaloneWorkOrders`: en bokning och en arbetsorder hela vägen |
| Inte beroende av Spring | uppfyllt | Inget Spring på classpath i den körningen, `logback-standalone.xml` i stället för Spring-profilerna |
| Inget GUI och ingen webbserver | uppfyllt | `main` med `System.out`, ingen JavaFX, ingen server |
| Minst en operation om arbetsorderns livscykel | uppfyllt | Skapa, starta och slutföra en arbetsorder |
| Samma affärsregler som appen | uppfyllt | Samma tjänsteklasser och samma `GuardedGarageSystem`; [`ADR-013`](arkitekturbeslut.md#adr-013-det-fristående-programmet-är-en-egen-startväg-med-samma-spärr) |

### §4 Autentisering

| Krav | Status | Bevis |
|---|---|---|
| Fungerande mekanism | uppfyllt | `LoginService`, tabellen `users`, PBKDF2 med salt |
| Individuella konton | uppfyllt | `admin`, `staff`, `readonly` i migrering 004. Kravet tillåter fördefinierade testkonton |
| Skyddade funktioner stängda för oautentiserade | uppfyllt | `NotAuthenticatedException` från spärren, prövat i `AuthorizationTest` |
| Lösenord inte i klartext | uppfyllt | PBKDF2, 210 000 varv i migreringen, omhashat till 600 000 vid inloggning |
| Uppgifter och hemligheter inte i loggen | uppfyllt | `LogContentTest`, `SecretScanTest` |
| Säkerhetskonfiguration utan hemligheter i källkoden | uppfyllt | `AUTOCORE_*_PASSWORD`, `autocore.db.path`, [`ADR-001`](arkitekturbeslut.md#adr-001-egen-inloggning-mot-users-ingen-extern-identitetsleverantör) |

### §5 Behörighetsstyrning

| Krav | Status | Bevis |
|---|---|---|
| Kontroll innan skyddade operationer | uppfyllt | `GuardedGarageSystem` kräver en operation för varje anrop, läsning som skrivning |
| Neka utan behörighet | uppfyllt | `AccessDeniedException`, skild från `NotAuthenticatedException` |
| Ingen väg runt kontrollen | uppfyllt | Konsolen loggar in och går genom samma spärr (SCRUM-357), `Actions` fångar händelser som fyras från kod, och `NoUnguardedConstructionTest` fäller en direkt konstruktion |
| Skilja oautentiserad från obehörig | uppfyllt | Två undantag, två texter, prövat i `AuthorizationTest` |
| Minst tre konton med olika behörighet | uppfyllt | `RoleMatrixTest`, `RoleMatrixProof` |
| Läsbehörig kan inte avsluta eller ta bort en arbetsorder | uppfyllt | `READ_ONLY` har `READ` och `CHANGE_LANGUAGE`; `completeWorkOrder` kräver `CHANGE_WORK_ORDER_STATUS` och borttagning kräver `DELETE_WORK_ORDER` |
| Inte tillräckligt att bara dölja i gränssnittet | uppfyllt | [`ADR-009`](arkitekturbeslut.md#adr-009-fönstret-frågar-spärren-innan-det-ritar): tre lager, där spärren ligger underst |

### §6 Säkerhet i olika körlägen

| Krav | Status | Bevis |
|---|---|---|
| Verksamhetens regler skilda från identitetshantering | uppfyllt | `IdentityHolder` och `Role` i kärnan, reglerna i tjänsterna |
| Visa hur behörighet hanteras där operationerna görs tillgängliga | uppfyllt | Det fristående programmet sätter en identitet och går genom spärren; `utan`-argumentet visar det nekade svaret |
| Kravet på fristående funktionalitet kvarstår efter säkerhetslösningen | uppfyllt | Samma program, med spärren på plats |

### §7 Loggning

| Krav | Status | Bevis |
|---|---|---|
| Etablerad lösning | uppfyllt | SLF4J och Logback |
| Lämpliga loggnivåer | uppfyllt | DEBUG för vår kod i dev, INFO i prod, WARN för andras, ERROR för tekniska fel |
| Verksamhetshändelser följbara | uppfyllt | Arbetsorder skapad och slutförd, `WorkOrderService` |
| Tekniska fel med felsökningsinformation | uppfyllt | Undantag och stapl i loggen, meningen till användaren är en annan ([`ADR-011`](arkitekturbeslut.md#adr-011-användaren-får-en-mening-loggen-får-detaljerna)) |
| Säkerhetshändelser | uppfyllt | Nekad åtkomst med användare, roll och operation |
| Konfigurerbar per körmiljö | uppfyllt | `logback-spring.xml` med `dev` och `prod` |
| Operation som misslyckas på grund av felaktiga uppgifter | uppfyllt | `AppException.noteRaised` skriver `Åtgärden avvisades:` med klass och textnyckel |
| Databasfel | uppfyllt | `DataAccessException` och `DatabaseInitException` loggas på ERROR med orsaken |
| Oväntad teknisk exception | uppfyllt | Fångas i gränssnittet och i det fristående programmet, loggas med stapl |
| Felsökning inte beroende av tillfälliga konsolutskrifter | uppfyllt | Appen loggar till fil. `System.out` finns bara i konsolversionen och i det fristående programmet, som är program vars svar *är* utskriften |

### §8 Spårbarhet och skydd av känslig information

| Krav | Status | Bevis |
|---|---|---|
| Tidpunkt, nivå och rapporterande del | uppfyllt | Mönstret i `logback-spring.xml` |
| Vilken operation som utfördes | uppfyllt | `op=` ur `Operation` i MDC |
| Identifierare, till exempel en arbetsorder | uppfyllt | Raden namnger arbetsordern och bokningen |
| Användaridentitet vid säkerhetshändelser | uppfyllt | Namn och roll när någon är inloggad, ingen när ingen är det |
| Inga lösenord eller token i loggen | uppfyllt | `LogContentTest` |
| Inga känsliga personuppgifter i onödan | uppfyllt | Användartext loggas aldrig, `EventLoggingTest` |
| Tekniska felmeddelanden avslöjar inget internt | uppfyllt | Textnycklar i stället för klassnamn och SQL |
| Fullständiga feluppgifter för felsökning | uppfyllt | Undantaget med stapl i loggen |
| Följa ett fel från användarens operation till delen där det uppstod | uppfyllt | Trace-id per operation, [`demo-loggning.md`](demo-loggning.md) |

### §9 Bibehållen information och funktionalitet

| Krav | Status | Bevis |
|---|---|---|
| Befintlig information bevarad | uppfyllt | `OldDatabaseProof`: samma antal kunder, fordon, bokningar och ordrar före och efter |
| Tidigare kunder, fordon och arbetsorder användbara | uppfyllt | Samma bevis |
| Befintliga affärsregler gäller | uppfyllt | Elva bevis mot Java 8-baslinjen, `WorkOrderRulesTest` |
| Tidigare valideringsregler fungerar | uppfyllt | `NumberRulesProof`, `RuleViolationProof` |
| Verksamhetsfel och tekniska fel hanteras konsekvent | uppfyllt | `AppException` med textnyckel, `DataAccessException` för lagring |
| Inte löst genom att radera data och börja om | uppfyllt | Migreringarna 001–004 är additiva, [`ADR-012`](arkitekturbeslut.md#adr-012-migreringarna-är-en-kedja-databasen-skapas-aldrig-om) |

### §10 Migrations- och riskanalys

| Krav | Status | Bevis |
|---|---|---|
| Plan med de åtta punkterna | uppfyllt | [`plan-4.0.md`](plan-4.0.md), en rubrik per punkt |
| Planen uppdateras när nya risker hittas | uppfyllt | Planens avsnitt 2, [riskanalysen](riskanalys.md), SCRUM-316 |

### §11 Demonstration

| Punkt | Status | Bevis |
|---|---|---|
| A. Plattformsmigrering | uppfyllt | `./start.sh`, `mvn -f WigellAutoCore/autocore/pom.xml package`, CI |
| B. Bibehållen funktionalitet | uppfyllt | [`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md) |
| C. Autentisering | uppfyllt | Logga in och använda det kontot har tillgång till |
| D. Behörighetskontroll, tre konton, tillåtet och nekat | uppfyllt | [`demo-loggning.md`](demo-loggning.md) avsnitt 2 visar ett nekande; `RoleMatrixProof` visar alla tre raderna |
| E. Skyddad operation kan inte kringgås | uppfyllt | Försök fyra händelsen från kod ger inget (`Actions`), direkt anrop nekas av spärren, `NoUnguardedConstructionTest` |
| F. Loggar: lyckad operation, nekad åtkomst, tekniskt fel | uppfyllt | [`demo-loggning.md`](demo-loggning.md), tre avsnitt med kommandon |
| G. Fristående körläge, två operationer utan Spring | uppfyllt | `StandaloneWorkOrders` |
| H. Arkitektoniska beslut | **delvis** | Besluten vi fattat finns i [arkitekturbeslut.md](arkitekturbeslut.md). Kravet frågar också vilka *tidigare* beslut som underlättade respektive försvårade arbetet, och den genomgången finns inte. Se [Det som saknas](#det-som-saknas) |

## WAC 2.5 — acceptanskriterier

| Kriterium | Status | Var det lever |
|---|---|---|
| 1. En bokning kan innehålla flera tjänster | uppfyllt | `booking_service_items`, `BookingService` |
| 2. Tjänster kan läggas till och tas bort innan arbetet påbörjats | uppfyllt | `BookingFormPane`, `isServicesLocked` |
| 3. Total beräknad arbetstid | uppfyllt | `MechanicSchedule`, bokningsdialogen |
| 4. Total beräknad kostnad | uppfyllt | `bookingPrice`, kolumnen för beräknad kostnad |
| 5. Arbetsordern innehåller arbetena | uppfyllt | `work_order_service_items` |
| 6. Faktura med flera rader | uppfyllt | `invoice_lines`, `BillingService` |
| 7. Pris kan ändras | uppfyllt | `updateServiceItem` |
| 8. Prisändring påverkar inte tidigare arbete eller fakturor | uppfyllt | Priset fryses vid slutförandet, prövat i `CompleteWithoutMarkingTest` |
| 9. Rabattfunktionerna fungerar med den nya modellen | uppfyllt | `DiscountRules`, `NumberRulesProof` |
| 10–11. Informationen sparas och finns kvar efter omstart | uppfyllt | SQLite, migreringarna |
| 12. Befintlig funktionalitet fungerar | uppfyllt | Regressionsjämförelsen |

Kravet att systemet "även fortsättningsvis ska kunna köras med Java 8" gäller inte längre: 4.0 §2
ersätter det med Spring Boot 4 och Java 17 eller senare.

## WAC 3.0 — förväntad leverans

| Punkt | Status | Var det lever |
|---|---|---|
| 1. Servicepaket kan skapas och användas vid bokning | uppfyllt | `ServicePackage`, `BookingPackagePicker` |
| 2. En bokning från ett paket kan ändras utan att paketet påverkas | uppfyllt | Bokningen får paketets tjänster som sina egna |
| 3. Senare ändringar av ett paket ändrar inte gamla bokningar | uppfyllt | Bokningen bär tjänsterna och paketnamnet själv, `package_name` i migreringen |
| 4. Ny bokning utifrån en tidigare | uppfyllt | `BookingDialogs.showCopyBookingDialog`, `BookingFormPane` med `forCopy` |
| 5. Olika typer av arbetsorder | uppfyllt | `WorkOrderType`: `STANDARD`, `RECLAMATION`, `INTERNAL` |
| 6. Drop-in utan tidigare bokning | uppfyllt | `createDropInBooking` |
| 7. Reklamation kopplad till tidigare arbetsorder | uppfyllt | `original_work_order_id` |
| 8–9. Utkast, och komplettering efter hand | uppfyllt | `createDraft`, `DraftChanges` |
| 10. Orimliga övergångar förhindras | uppfyllt | `WorkOrderStatus.canChangeTo`, `StatusRoutesProof` |
| 11–12. Beständighet och befintlig funktionalitet | uppfyllt | Migreringarna, regressionsjämförelsen |

## WAC 3.5 — leveranskrav

| Punkt | Status | Bevis |
|---|---|---|
| Centrala funktioner oberoende av gränssnittet | uppfyllt | Fasaden `GarageSystem`, konsolen och det fristående programmet |
| Samma resultat oavsett varifrån | uppfyllt | En väg genom tjänsterna, [`ADR-006`](arkitekturbeslut.md#adr-006-behörigheten-sitter-i-en-dekoratör-framför-fasaden) |
| Verksamhetsreglerna oberoende av lagringen | uppfyllt | `RepositoryFactory`, [`ADR-004`](arkitekturbeslut.md#adr-004-lagringen-är-en-böna-med-sökvägen-i-konfigurationen) |
| Lagringsfel skilda från otillåtna operationer | uppfyllt | `DataAccessException` mot `RuleViolationException` och `ValidationException` |
| Samma regler för arbetsorderns tillstånd oavsett väg | uppfyllt | `canChangeTo` används av både vy och tjänst |
| Enhetlig validering och felrapportering | uppfyllt | Undantagen med textnyckel, reglerna i tjänsterna |
| Etablerade standarder för validering | **delvis** | Inget Jakarta Bean Validation. Reglerna ligger i tjänsterna i stället. Kravet är formulerat som ett önskemål där teamet får bedöma |
| Tidigare funktionalitet inte försämrad | uppfyllt | Regressionsjämförelsen, 135 prov |

## Det som saknas

Tre saker. Ingen av dem är ett hinder för demon, men den första är ett krav som uttryckligen
efterfrågas och som en granskare letar efter.

**1. §11 H:s andra halva saknas.** Kravet är inte bara vilka beslut vi fattat, utan också vilka
*tidigare* teknik- och arkitekturbeslut som underlättade respektive försvårade arbetet. Det finns
ingen sådan genomgång. Underlaget finns i [riskanalysen](riskanalys.md): den statiska `Db`,
bevisen i produktionsträdet och två byggvägar försvårade arbetet; fasaden `GarageSystem`,
`RepositoryFactory` och tjänstelagret gjorde det möjligt att flytta in Spring och spärren utan att
röra verksamhetslogiken.

Att göra: ett kort avsnitt i [arkitekturbeslut.md](arkitekturbeslut.md), "Vad som underlättade och
vad som försvårade", med fem rader och hänvisning till risknumren.

**2. Validering utan etablerad standard (WAC 3.5 §5).** Reglerna är handskrivna i tjänsterna i
stället för Jakarta Bean Validation. Kravet är ett önskemål och teamet får bedöma, men det bör stå
en rad om varför, så att det inte ser ut som en glömska.

**3. Prisfrysningen har ett indirekt prov.** WAC 2.5 kriterium 8 prövas genom
`CompleteWithoutMarkingTest`, men det finns inget prov som ändrar priset i katalogen efter en
faktura och visar att den gamla fakturan står still. Låg risk, eftersom priset fryses vid
slutförandet, men det är det prov en granskare frågar efter.

## Det som är kontrollerat och håller

Allt övrigt i de fyra dokumenten är täckt: plattformsbytet, den fristående vägen, autentisering,
behörighet inklusive den läsbehörigas gränser, loggning och spårbarhet, bevarad information,
planen med de åtta punkterna, alla åtta demopunkter utom halva H, och samtliga acceptanskriterier i
2.5, 3.0 och 3.5 utom den frivilliga valideringsstandarden.

135 prov går igenom, elva bevis svarar som Java 8-baslinjen eller bara olika där indata gör det, och
`OldDatabaseProof` visar att den gamla databasen lyfts utan att något tappas.

## Så kontrolleras det om

Från repots rot:

```
mvn -o test -f WigellAutoCore/autocore/pom.xml     # 135 prov
grep -c '^## ADR-' docs/arkitekturbeslut.md        # 18 beslut
grep -c '^| R[0-9]' docs/riskanalys.md             # 25 risker
```

Kraven själva ligger utanför repot, i `~/Documents/AutoCore/WAC4_0.pdf` och de tre tidigare
versionerna. Texten går att läsa med `pdftotext -layout WAC4_0.pdf -`.
