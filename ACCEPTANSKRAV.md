# Acceptanskrav för Wigell AutoCore

Dokumentet sammanfattar vad systemet ska kunna göra. Funktionerna kontrolleras genom att starta applikationen och gå igenom de viktigaste flödena i GUI:t eller konsolversionen.

## Bokningar

- En bokning ska kunna innehålla flera verkstadstjänster.
- Tjänsternas uppskattade tid och pris ska summeras.
- Tjänster ska kunna ändras innan arbetet har startat.
- Bokningar ska inte kunna krocka med andra bokningar eller ligga utanför verkstadens öppettider.
- Mekaniker ska bara kunna väljas när deras kompetens och tillgänglighet passar bokningen.

## Arbetsordrar

- En arbetsorder ska kunna skapas från en bokning.
- Bokningens tjänster ska följa med till arbetsordern.
- Arbetsordern ska ha ett tydligt statusflöde från skapad till påbörjad och slutförd.
- Mekanikern ska kunna markera vilka tjänster som faktiskt är utförda.
- Priset för ett utfört arbete ska sparas så att en senare katalogändring inte ändrar historiken.

## Fakturering och betalning

- En faktura ska ha en rad per utförd tjänst.
- Rabattregler och rabattkoder ska räknas på fakturans rader.
- Fakturans totalsumma ska aldrig bli negativ.
- Betalningar ska sparas med vald betalmetod och kopplas till rätt faktura.

## Persistens

- Kunder, fordon, bokningar, arbetsordrar, fakturor och betalningar ska sparas i SQLite.
- Kopplingen mellan bokningar och tjänster ska finnas kvar när applikationen startas om.
- Databasen ska kunna skapa sina tabeller och lägga in grunddata när applikationen startar.

## Språk och gränssnitt

- Gränssnittet ska kunna växla mellan svenska och engelska utan omstart.
- Svenska och engelska språkfiler ska ha samma nycklar.
- Emerald-temat ska ha tydliga kontraster mellan text, bakgrunder och knappar.
- Interaktiva kontroller ska ha synliga fokusstilar.
- Text ska vara läsbar i programmets vanliga vyer.

Detta är projektets praktiska kravlista. Den är inte en formell WCAG-certifiering och innehåller inga påståenden om automatiska testresultat.
