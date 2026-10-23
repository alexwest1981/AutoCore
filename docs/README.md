# Dokumenten i `docs/`

Fyra dokument hör till leveransen av AutoCore 4.0. Resten är underlag som gör påståendena i dem
kontrollerbara.

| Dokument | Svarar på | Läs först om du ska |
|---|---|---|
| [sprint-6-sammanstallning.md](sprint-6-sammanstallning.md) | Vad sprinten levererade, siffrorna, vad som är öppet och var bevisen finns. | Få helheten inför redovisningen. |
| [riskanalys.md](riskanalys.md) | Vilka risker som fanns, vilka som slog till, vad som gjordes och vad som är kvar. | Bedöma genomförandet. |
| [arkitekturbeslut.md](arkitekturbeslut.md) | Varför systemet ser ut som det gör, beslut för beslut (ADR-001 till ADR-017). | Granska arkitekturen. |
| [plan-4.0.md](plan-4.0.md) | Planen före arbetet: plattform, risker, migrering, säkerhet, loggning, fristående läge och kontroll. | Se vad som var tänkt innan. |
| [kravtackning.md](kravtackning.md) | WAC 2.5 till 4.0 rad för rad mot koden, dokumenten och tavlan, och de två punkter som återstår. | Kontrollera att inget krav tappats. |
| [inkompatibiliteter.md](inkompatibiliteter.md) | Vad som inte gick att flytta rakt av, var det syntes och hur det hanterades. | Se §2 och §11 H i kravet. |
| [genomgang.md](genomgang.md) | Ordningen de åtta demopunkterna tas i, med kommandon och en reservväg per punkt. | Hålla i redovisningen. |

Underlaget:

| Fil | Innehåll |
|---|---|
| [demo-loggning.md](demo-loggning.md) | De tre loggraderna demon kräver, med kommandona som ger dem. |
| [bevis/README.md](bevis/README.md) | Hur bevisen togs och hur de körs om. |
| [bevis/regressionsjamforelse.md](bevis/regressionsjamforelse.md) | Jämförelsen mellan Java 8-baslinjen och 4.0, rad för rad. |
| [bevis/baslinje-java8/](bevis/baslinje-java8/) | Utskrifterna från Java 8. |
| [bevis/utskrifter-4.0/](bevis/utskrifter-4.0/) | Samma prov körda på 4.0. |
| [index.html](index.html) | Landningssidan som publiceras via GitHub Pages. |

Repots [README](../README.md) är ingången för den som ska bygga och köra. Där står också vad som
räknas som bevis och hur ett kort stängs.

## Så hålls dokumenten sanna

- Siffrorna i sammanställningen räknas om mot koden innan dokumenten delas. Kommandona står i
  [sprint-6-sammanstallning.md](sprint-6-sammanstallning.md#så-kontrolleras-siffrorna).
- Ett nytt arkitekturbeslut får nästa lediga ADR-nummer och samma fem rubriker. Ett ersatt beslut
  tas inte bort.
- En ny risk får nästa lediga R-nummer i [riskanalysen](riskanalys.md). När den stängs flyttas
  beviset in i raden.
- Ett bevis som inte går att köra om räknas inte. Regeln står i [README](../README.md).
