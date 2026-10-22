# De tre loggraderna som demon kräver

WAC4_0 §11 F: "Teamet visar loggar från minst en lyckad verksamhetsoperation, en nekad åtkomst
och ett tekniskt fel."

Raderna nedan är kopierade ordagrant ur körningarna den 22 oktober 2026 på `develop`. Varje rad
bär tidpunkt, nivå, tråd, trace-id, operation och logger, alltså det §8 räknar upp. Inget av dem
innehåller lösenord, token eller personuppgifter.

Kör kommandona från `WigellAutoCore/autocore` efter `mvn test-compile`. Classpath är
`target/classes:$(cat target/cp.txt)`.

## 1. Lyckad verksamhetsoperation

Det fristående programmet skapar en bokning och driver arbetsordern till klar. Ingen inloggning
behövs för det här programmet, det sätter en identitet själv.

```
java -cp "target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/demo.db
```

```
2026-10-22T14:45:18.391+02:00 INFO  [main] [trace=87daa17e op=CREATE_WORK_ORDER] c.w.a.service.WorkOrderService - Arbetsorder 7 skapad för bokning 7
2026-10-22T14:45:18.411+02:00 INFO  [main] [trace=25cc3eea op=CHANGE_WORK_ORDER_STATUS] c.w.a.service.WorkOrderService - Arbetsorder 7 slutförd
```

Den andra raden kommer från samma körning. `op=` säger vilken operation raden hör till, och
`trace=` binder ihop raderna från samma anrop.

## 2. Nekad åtkomst

Samma program, men med argumentet `utan`, som lämnar identitetshållaren tom.

```
java -cp "target/classes:$(cat target/cp.txt)" \
     com.wac.autocore.standalone.StandaloneWorkOrders /tmp/demo.db utan
```

```
2026-10-22T14:45:18.991+02:00 WARN  [main] [trace=46a144c2 op=READ] c.w.a.service.GuardedGarageSystem - Åtkomst nekad: ingen inloggad försökte READ
NEKAD: You are not signed in. Sign in and try again.
```

Loggraden är WARN och namnger operationen men ingen användare, eftersom det inte fanns någon.

Nekas en inloggad användare i stället står namnet och rollen där. Här en läsbehörig användare som
försöker skapa en kund i konsolen, mot en databas där kontots lösenord satts vid start med
`AUTOCORE_READONLY_PASSWORD`:

```
printf 'readonly\ndemo12345\n2\nTestkund\n070-1234567\ntest@example.se\n0\n' \
  | java -cp "target/classes:$(cat target/cp.txt)" com.wac.autocore.ConsoleApp /tmp/demo2.db
```

```
2026-10-22T14:46:32.474+02:00 WARN  [main] [trace=c0fc4307 op=CREATE_CUSTOMER] c.w.a.service.GuardedGarageSystem - Åtkomst nekad: readonly (READ_ONLY) försökte CREATE_CUSTOMER
Customer was not saved: Role READ_ONLY may not CREATE_CUSTOMER.
```

Samma nej, två mottagare: loggen får namn, roll och operation, och användaren får en mening.

## 3. Tekniskt fel

Appen startad mot en databassökväg som inte går att öppna. Kräver en bildskärm, eftersom det är
fönstret som startar.

```
java -cp "target/classes:$(cat target/cp.txt)" \
     -Dautocore.db.path=/proc/autocore.db com.wac.autocore.Main
```

```
2026-10-22T14:45:23.965+02:00 ERROR [JavaFX-Launcher] [trace=- op=-] com.wac.autocore.ui.AutoCoreApp - Databasen är inte körbar. Åtgärda felet och starta om.
com.wac.autocore.exception.DatabaseInitException: Databasen kunde inte förberedas: [SQLITE_CANTOPEN] Unable to open the database file (unable to open database file)
	at com.wac.autocore.data.Migrations.apply(Migrations.java:72)
	at com.wac.autocore.data.Db.initTables(Db.java:54)
```

Användaren får meningen "Databasen är inte körbar. Åtgärda felet och starta om." och loggen får
undantaget med staplen. Det är skillnaden §8 kräver: tekniska uppgifter nog för felsökning, men
inte i användarens ruta.

## Vad raderna visar tillsammans

Raderna kommer från tre olika vägar in i systemet: det fristående programmet med identitet, samma
program utan, och appens start. Alla tre skriver till samma logg och med samma mönster, vilket är
det §7 menar med att loggningen ska gå att följa oavsett var felet uppstod.

Loggfilen hamnar i `logs/autocore.log`. Katalogen kan flyttas med `AUTOCORE_LOG_DIR`, och
`prod`-profilen skriver bara till fil och på WARN, enligt `logback-spring.xml`.
