# Bevisen i den här mappen

Här ligger underlaget för jämförelsen i [regressionsjamforelse.md](regressionsjamforelse.md).
Mappen finns för att påståendet i [planen](../plan-4.0.md) ska gå att kontrollera med bara
repot framför sig.

## Vad som ligger här

| Mapp | Innehåll |
|---|---|
| `baslinje-java8/` | Elva utskrifter från Java 8, inspelade 20 oktober 2026 på commit `b0ef2b9`. `README.txt` i mappen säger hur de kördes. |
| `utskrifter-4.0/` | Samma elva prov körda på 4.0 med samma argument. `StatusRoutesProof.txt` är omkörd 22 oktober efter SCRUM-326. |

Filerna är provens eget svar, ordagrant som de skrevs. Inget är redigerat i efterhand.

## Hur jämförelsen görs om

Proven ligger i `WigellAutoCore/autocore/src/test/java/com/wac/autocore/proof/` och körs
efter `mvn test-compile`:

```
cd WigellAutoCore/autocore
mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt
mvn -o test-compile
java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.proof.StatusRoutesProof
```

Varje prov skriver sitt svar på stdout. Argumenten per prov står i
`baslinje-java8/README.txt`, och de är samma på båda sidorna av jämförelsen.

Skillnaden mellan en utskrift från 2026 och en från i dag är tre saker som inte har med
saken att göra: loggrader, JVM-varningar och sökvägen till databasen. Jämförelsen är gjord
på utskrifter där de är borttagna och sökvägen utbytt mot `<db>`. Inget annat är filtrerat,
och det står i [regressionsjamforelse.md](regressionsjamforelse.md) vilka rader som skiljer
sig och varför.

## Originalen

Originalen ligger kvar utanför repot i `~/Documents/Skolgrejer/AutoCore-bevis/E5-2/`, hos
Alex. De är kopierade hit oförändrade, så repo-kopian räcker för att läsa och jämföra. Att
 originalen ligger kvar där de skrevs är ett önskemål från gruppen, inte ett krav från
kravdokumentet.
