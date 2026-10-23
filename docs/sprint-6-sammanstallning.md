# Sprint 6 — vad som hände och vad som lämnas in

AutoCore 4.0 byggdes klart under sprint 6. Tavlan delar den i 6.0 till 6.5, från 19 oktober till
stängningen fredag 23 oktober 2026. Den här filen sammanställer vad som levererades, vilka kort
som är kvar och var bevisen finns.

Den som ska läsa in sig inför redovisningen kan ta det i den här ordningen: den här filen för
helheten, [riskanalysen](riskanalys.md) för vad som gick fel och varför, och
[arkitekturbesluten](arkitekturbeslut.md) för varför systemet ser ut som det gör. Planen med de
åtta punkterna och regressionsjämförelsen är underlaget bakom.

## Siffrorna vid stängning

Kontrollerade mot kod och tavla 23 oktober 2026, klockan 12.29. Backloggen flyttades in i sprint
6.5 under eftermiddagen, så kortantalet är en ögonblicksbild och inte samma som vid sprintens start.

| Mått | Värde | Var det kommer ifrån |
|---|---|---|
| Kort i sprint 6 | 65 | Tavlan, sprintarna 6.0–6.5 |
| Kort klara | 58 | Tavlan |
| Kort i In Review | 1, SCRUM-330 | Tavlan |
| Kort i In Progress | 2, SCRUM-283 och SCRUM-348 | Tavlan 23 oktober |
| Kort i To Do | 4, SCRUM-282, SCRUM-322, SCRUM-351 och SCRUM-352 | Tavlan 23 oktober |
| Story points | 84 | Tavlan |
| Commit | 105, varav 95 utan merge | `git log --since=2026-10-19` |
| Fördelning | Alex 58, Daniel 25, Lucas 12 commit utan merge | `git log` efter författare |
| JUnit-prov | 132, alla gröna | `mvn -o test` 23 oktober |
| Bevisprogram | 15 | `src/test/java/com/wac/autocore/proof/` |
| Bevis med Java 8-baslinje | 11 | [`bevis/`](bevis/README.md) |
| Migreringar | 4 | `src/main/resources/com/wac/autocore/db/migrations/` |
| Operationer i rollmatrisen | 30 | `Operation.java` |
| Roller | 3 | `Role.java` |

## Vad som levererades, per epik

### E1 Plattform och bygge (SCRUM-287)

| Kort | Vad det gav |
|---|---|
| SCRUM-294 | `pom.xml` med JDK 25 och JavaFX 25. Maven hämtar allt som förut låg i `lib/`. |
| SCRUM-295 | Resurserna flyttade till `src/main/resources`, så Maven tar med dem i jar-filen. |
| SCRUM-296 | Spring Boot startar appen, och `start.sh` bygger med Maven i stället för `javac`. |
| SCRUM-297 | `DatePickerSkin` från det publika API:et i stället för `com.sun.javafx`, och veckokolumnen syns. |
| SCRUM-328 | Prov som visar att Spring-kontexten startar med rätt databassökväg. |
| SCRUM-334 | `--enable-native-access` gäller alla startvägar, och `BuildFlagsTest` ser till att den finns kvar. |
| SCRUM-336 | Död kod och en klass utanför paketet bort ur trädet. |
| SCRUM-340 | JDK-golvet förklarat i `pom.xml` och README. |
| SCRUM-330 | Listan över inkompatibiliteter. Ligger i In Review vid stängning. |

Bevis: [`pom.xml`](../WigellAutoCore/autocore/pom.xml), [`.github/workflows/ci.yml`](../.github/workflows/ci.yml),
[`start.sh`](../start.sh), `SpringContextTest`, `BuildFlagsTest`, [README](../README.md).

### E2 Kärnan och det fristående läget (SCRUM-288)

| Kort | Vad det gav |
|---|---|
| SCRUM-298 | `Db` är en böna med sökvägen i konfigurationen, och `RepositoryFactory` fördelar den. |
| SCRUM-299 | `IdentityHolder` i kärnan, utan Spring. |
| SCRUM-300 | `StandaloneWorkOrders` bokar och driver en arbetsorder utan Spring och utan fönster. |
| SCRUM-313 | Det fristående programmet skiljer utloggad från obehörig. |
| SCRUM-338, SCRUM-339 | Programmet väljer en öppen dag i stället för att anta dagens datum plus trettio. |

Bevis: [`DatabaseConfig.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/config/DatabaseConfig.java),
[`IdentityHolder.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/security/IdentityHolder.java),
[`StandaloneWorkOrders.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/standalone/StandaloneWorkOrders.java),
`StandaloneWorkOrdersTest`.

### E3 Autentisering och behörighet (SCRUM-289)

| Kort | Vad det gav |
|---|---|
| SCRUM-301 | Migrering 004 med `users`, roll och PBKDF2-hash. |
| SCRUM-302, SCRUM-319 | Inloggning i appen, som ett kort i huvudfönstret. |
| SCRUM-303 | Rollmatrisen som data: 30 operationer, tre roller. |
| SCRUM-304 | Behörighetsdekoratören framför fasaden. |
| SCRUM-305 | Tre konton med tillåten och nekad åtkomst. |
| SCRUM-312 | Inga hemligheter i källkoden; lösenord kan sättas i miljön. |
| SCRUM-320 | Läsningarna går genom behörighetskontrollen. |
| SCRUM-324, SCRUM-325 | Språkvalet sparas och är en egen operation för alla roller. |
| SCRUM-332 | Bevisklasserna ut ur produktionsträdet. |
| SCRUM-337 | De två metoder som gick förbi spärren. |
| SCRUM-361 | Det rollen inte får använda syns inte i fönstret. |
| SCRUM-362 | Ekonomin stängs för läsbehörig, och ett klick utan knapp når inte fram. |
| SCRUM-357 | Konsolversionen loggar in och går genom samma spärr. |

Bevis: [`RoleMatrix.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/security/RoleMatrix.java),
[`GuardedGarageSystem.java`](../WigellAutoCore/autocore/src/main/java/com/wac/autocore/service/GuardedGarageSystem.java),
`RoleMatrixTest`, `AuthorizationTest`, `NoUnguardedConstructionTest`, `SecretScanTest`.

### E4 Loggning och spårbarhet (SCRUM-290)

| Kort | Vad det gav |
|---|---|
| SCRUM-306 | Logback med en loggfil och nivå per miljö. |
| SCRUM-307 | Händelser för verksamhet och säkerhet. |
| SCRUM-308 | Trace-id och operation på varje rad, och inga personuppgifter i loggen. |
| SCRUM-314 | Tekniska fel visas som en mening; detaljerna stannar i loggen. |
| SCRUM-329, SCRUM-331 | Loggningen gäller även utanför fönstret, och inloggningen syns i den. |
| SCRUM-341 | Loggprovet skriver sin egen loggfil i stället för att hoppas att en finns. |
| SCRUM-327 | Prov för verksamhetshändelser och användartext. |
| SCRUM-358 | Ett tekniskt nej får en mening, inte "NEKAD:". |

Bevis: [`logback-spring.xml`](../WigellAutoCore/autocore/src/main/resources/logback-spring.xml),
[`logback-standalone.xml`](../WigellAutoCore/autocore/src/main/resources/logback-standalone.xml),
`LogContentTest`, `EventLoggingTest`, [`demo-loggning.md`](demo-loggning.md).

### E5 Bevis och dokumentation (SCRUM-291)

| Kort | Vad det gav |
|---|---|
| SCRUM-292 | Regressionsbaslinje på Java 8, elva prov. |
| SCRUM-309 | Jämförelsen mot baslinjen: elva av elva svarar lika eller bara olika där indata gör det. |
| SCRUM-315 | Appen körd mot en kopia av den riktiga databasen. |
| SCRUM-316 | Planen delas och uppdateras när en risk slår till. |
| SCRUM-321 | Planen för migreringen som dokument med de åtta punkterna. |
| SCRUM-326 | `StatusRoutesProof` tar dag och timme i stället för att läsa klockan. |
| SCRUM-333 | Prov för arbetsorderns affärsregler. |
| SCRUM-286 | En arbetsorder som avbröts av misstag går att ta bort, med spärr och logg. |
| SCRUM-342 | Regressionsbeviset flyttat in i repot. |
| SCRUM-323 | Demons tre loggrader med kommandona som ger dem. |
| SCRUM-322 | De arkitektoniska besluten. Den här sammanställningen och [`arkitekturbeslut.md`](arkitekturbeslut.md) är underlaget. |
| SCRUM-335 | README stämmer med koden och tar upp loggningen. Uppdaterad 23 oktober. |

Bevis: [`bevis/regressionsjamforelse.md`](bevis/regressionsjamforelse.md),
[`bevis/baslinje-java8/`](bevis/baslinje-java8/), [`bevis/utskrifter-4.0/`](bevis/utskrifter-4.0/),
`OldDatabaseProof`, `WorkOrderRulesTest`, [README](../README.md).

### Korten utan epik

Nio kort i sprint 6 hörde inte till någon av epikerna. De flesta kom ur genomgångarna 22 oktober;
SCRUM-319 och SCRUM-327 hörde till inloggningen respektive loggningen men saknade epik. De är lika
mycket en del av leveransen:

| Kort | Vad det gav |
|---|---|
| SCRUM-355 | Gränssnittet står still när man klickar och när sidor laddas. |
| SCRUM-356 | Skalan är 1 när appen startar, oavsett ärvd `GDK_SCALE`. |
| SCRUM-357 | Konsolen går genom behörighetsspärren. |
| SCRUM-358 | Konsolen visar en mening i stället för råa fel. |
| SCRUM-360 | Ett kort stängs bara med ett bevis som går att köra om. |
| SCRUM-361 | Funktioner rollen inte får använda syns inte i fönstret. |
| SCRUM-362 | Ekonomin är administratörens, och ett klick utan knapp når inte fram. |
| SCRUM-319 | Inloggningskortet i huvudfönstret. |
| SCRUM-327 | Prov för verksamhetshändelser och användartext. |

Två kort löstes också under sprinten men ligger utanför sprinten på tavlan: SCRUM-343 (en avbruten
reklamation avbokade hela bokningen) och SCRUM-344 (en återställd bokning syntes inte i "Skapa
arbetsorder"). Båda är Done, och bevisen finns i commit `5a16c05` respektive `ebc6e64` samt i
`WorkOrderRulesTest`.

## Det som ändrade planen

Planen skrevs före arbetet och uppdaterades när en risk slog till. Det här är de tolv ändringar som
betydde mest, med risknumret i [riskanalysen](riskanalys.md):

1. `start.sh` föll när Maven tog över beroendena (R1). Den byggdes om till att köra Maven.
2. Resurserna lästes från filsystemet och `DatePickerSkin` fanns bara i en intern klass (R2).
3. Den statiska `Db` blev en böna med sökvägen i konfigurationen (R3).
4. Bevisen flyttade ut ur produktionsträdet (R4), vilket också tog bort tio genvägar förbi spärren.
5. Läsningarna gick fritt ända till E3-7, och konsolen gick förbi spärren ända till SCRUM-357 (R6, R12).
6. Ärvd `GDK_SCALE` dubblerade hela gränssnittet (R7).
7. Det fristående programmet kunde boka en stängd dag (R8).
8. Ett bevis var beroende av klockan, och baslinjen låg i `/tmp` (R9, R10).
9. Ett loggprov hoppades över, och konsolen visade råa fel (R11, R13).
10. Gränssnittet erbjöd funktioner rollen inte får, ett klick utan knapp nådde fram, och ekonomin
    låg öppen för läsbehörig (R14, R15, R16).
11. En avbruten reklamation påverkade en bokning som fortfarande användes, och en återställd
    bokning syntes inte i arbetsorderskaparen (R17, R18). Korten låg utanför sprinten men löstes
    under den.
12. Bygget krävde JDK 25 trots kravet på 17 eller senare (R20). Det förklarades i stället för att
    döljas.

Flera saker hittades utanför sviten och var därför värda egna kort: den brutna `start.sh`, som föll
först när någon startade den, skalan som ärvdes från sessionen, och gränssnittets rörelse vid hover
och fokus, som bara syntes när någon tittade på fönstret.

## Öppet när sprinten stänger

| Kort | Status | Vad som återstår |
|---|---|---|
| SCRUM-330, E1-7 Listan över inkompatibiliteter | In Review | Granska och merga. Listan hör ihop med [arkitekturbesluten](arkitekturbeslut.md) och README. |
| SCRUM-348, R3 Ändra tid | In Progress | Knappen i arbetsorderns detaljvy. Lucas. |
| SCRUM-283, L5 Arkitekturbeskrivning | In Progress | Underlaget är [`arkitekturbeslut.md`](arkitekturbeslut.md). Daniel. |
| SCRUM-322, E5-7 Arkitektoniska beslut | To Do | Underlaget är [`arkitekturbeslut.md`](arkitekturbeslut.md) och den här filen. Kortet kan stängas när filerna är mergade. |
| SCRUM-282, L4 Demon | To Do | De sex punkterna inför beställaren. |
| SCRUM-351, SCRUM-352 | To Do | Två beslut som väntar: siffror i mekanikernas namn, och om en arbetsorder får slutföras utan utförda arbeten. Se nästa avsnitt. |
| SCRUM-335, E5-11 README | Done | Mergad i #261. README tar upp arkitekturträdet, inloggning och roller, loggningen och hur sviten körs. |
| SCRUM-345, R2-2 Badgefärgen | Done | Mergad i #262. Färgen följer statusvärdet, och `StatusBadgeTest` håller den kvar. |
| SCRUM-353, R2-1 Typer som enum | Done | Mergad i #263. `PaymentType` och `WorkOrderType`, med `StoredTypeTest` som bevis. |
| SCRUM-350, R7 Passerade timmar | Done | Mergad i #266. Regeln ligger i `OpeningHours.hasPassed`, med `OpeningHoursTest` som bevis. |
| SCRUM-346, SCRUM-349 | Done | Mergade i #264 och #265 av Lucas. |
| SCRUM-354, utloggning | Done på tavlan | Saknar bevis enligt genomgången i SCRUM-360. Öppna igen eller redovisa som manuellt prövat. |
| Grenar | Öppna | Merga i beroendeordning före 15.00 och radera de grenar som är inmergade. |

De kvarstående riskerna O1–O9 i [riskanalysen](riskanalys.md) påverkar inte demon men ska nämnas i
redovisningen.

## Det som flyttades in i sprinten under eftermiddagen

De här korten låg i backloggen på morgonen och flyttades in i sprint 6.5. Fyra av dem är mergade
under eftermiddagen: 346, 349, 350 och 353. Kvar i den gruppen är två beslut och två leveranskort.

| Kort | Vad det gäller |
|---|---|
| SCRUM-351 | R8: beslut om mekaniker får ha siffror i namnet. |
| SCRUM-352 | R9: beslut om en arbetsorder får slutföras utan utförda arbeten. |
| SCRUM-282 | L4: demon med de sex punkterna inför beställaren. |
| SCRUM-283 | L5: kort arkitekturbeskrivning för beställaren. [`arkitekturbeslut.md`](arkitekturbeslut.md) är underlaget. |

SCRUM-364, databasen som säger att den är redo innan den är det, är stängd. Commit `b95626a` gjorde
samma sak, vilket var det som kortet och koden skulle jämföras om.

## Vad som inte ingår i 4.0

- Ingen extern identitetsleverantör. Beslutet och skälen står i [ADR-001](arkitekturbeslut.md#adr-001-egen-inloggning-mot-users-ingen-extern-identitetsleverantör).
- Ingen webbserver och ingen webbklient. Appen är en desktop-app med en lokal SQLite-fil ([ADR-002](arkitekturbeslut.md#adr-002-spring-boot-startar-appen-gränssnittet-är-kvar-i-javafx)).
- Inga JavaFX-körningsprov i CI. Gränssnittets beteenden prövas manuellt (O1).
- Ingen automatisk säkerhetskopia före migrering. Kör mot en kopia (O7).
- Ingen prövning på Windows eller macOS i CI (O8).
- Ingen automatisk kontroll att dokumentens siffror stämmer. De räknas om för hand (O5).

## Så kontrolleras siffrorna

Kommandona är körda 23 oktober 2026 från `WigellAutoCore/autocore` om inget annat sägs.
`mvn -o test` kräver att beroendena redan är hämtade; annars kör `mvn test`.

```
mvn -o test                                  # 132 prov, 0 fel

grep -rho '@Test' src/test/java | wc -l      # 132
ls src/test/java/com/wac/autocore/proof/*.java | wc -l   # 15
ls src/main/resources/com/wac/autocore/db/migrations/*.sql | wc -l   # 4

sed -n '/public enum Operation/,/^}/p' \
  src/main/java/com/wac/autocore/security/Operation.java \
  | grep -cE '^    [A-Z][A-Z_0-9]*(,|$)'     # 30

sed -n '/public enum Role/,/^}/p' \
  src/main/java/com/wac/autocore/security/Role.java \
  | grep -cE '^    [A-Z][A-Z_0-9]*(,|;|$)'   # 3

# Från repots rot:
git log --since=2026-10-19 --no-merges --oneline | wc -l   # 95
```

Antalet prov i surefire-rapporterna kan summeras separat:

```
grep -h 'tests=' target/surefire-reports/*.xml \
  | sed -E 's/.*tests="([0-9]+)".*/\1/' \
  | awk '{s+=$1} END {print s}'              # 132
```

Korten och story points kommer från Jira-tavlan och går inte att räkna fram ur repot.

## Dokumentkartan

| Dokument | Svarar på | Läsekrets |
|---|---|---|
| [README](../README.md) | Vad systemet gör, hur det byggs och körs, inloggning och roller, loggningen, och vad som räknas som bevis. | Den som ska köra eller granska koden. |
| [plan-4.0.md](plan-4.0.md) | Planen före arbetet: plattform, risker, migrering, säkerhet, loggning, fristående läge och kontroll. | Den som vill se vad som var tänkt innan. |
| [riskanalys.md](riskanalys.md) | Vad som faktiskt hände med varje risk, och vad som är kvar. | Den som ska bedöma genomförandet. |
| [arkitekturbeslut.md](arkitekturbeslut.md) | Varför systemet ser ut som det gör, beslut för beslut. | Den som ska granska arkitekturen. |
| [demo-loggning.md](demo-loggning.md) | De tre loggraderna och kommandona som ger dem. | Den som ska hålla demon. |
| [bevis/README.md](bevis/README.md) | Hur bevisen togs och hur de körs om. | Den som vill kontrollera påståendena. |
| [bevis/regressionsjamforelse.md](bevis/regressionsjamforelse.md) | Varje rad som skiljer Java 8-baslinjen från 4.0. | Den som ska se att inget tappades. |
| [sprint-6-sammanstallning.md](sprint-6-sammanstallning.md) | Den här filen: helheten, siffrorna och det som är öppet. | Alla, först. |

Alla delade dokument ligger i `docs/`. En kortare översikt finns i [`docs/README.md`](README.md).
