# Arkitekturbeslut — AutoCore 4.0

Ett beslut här är ett som är svårt att ändra utan att röra flera lager: var identiteten kommer ifrån, var spärren sitter, hur lagringen skapas, hur loggen ser ut. Mindre val, som namnet på en metod, hör inte hit.

Från ADR-002 och framåt har varje beslut samma form: **Beslut**, **Skäl**, **Följder**, **Bevis **och **När det ska omprövas**. ADR-001 behåller sina egna underrubriker, eftersom den flyttas in oförändrad. Beviset ska gå att kontrollera från en färsk klon, antingen som en fil i repot eller som ett prov.

ADR-001 skrevs av Alex 21 oktober och låg i gruppens gemensamma mapp utanför repot. Den flyttas hit i sak oförändrad, så att alla beslut ligger på samma ställe.

**ADR**

**Beslut**

**Berör**

*001*

Egen inloggning mot users, ingen extern identitetsleverantör

E3

*002*

Spring Boot startar appen, gränssnittet är kvar i JavaFX

E1

*003*

JDK 25 är bygggolvet, trots att kravet säger 17 eller senare

E1

*004*

Lagringen är en böna med sökvägen i konfigurationen

E2

*005*

Identiteten bor i kärnan

E2, E3

*006*

Behörigheten sitter i en dekoratör framför fasaden

E3

*007*

Rollmatrisen är data

E3

*008*

Ekonomin tillhör administratören

E3

*009*

Fönstret frågar spärren innan det ritar

E3

*010*

Loggning med profil per miljö och trace-id i MDC

E4

*011*

Användaren får en mening, loggen får detaljerna

E4

*012*

Migreringarna är en kedja, databasen skapas aldrig om

E5

*013*

Det fristående programmet är en egen startväg med samma spärr

E2

*014*

Bevisen ligger utanför produktionsträdet

E3, E5

*015*

Skalan är 1 när appen startar

E1

*016*

Native access deklareras i manifestet

E1

*017*

Språkvalet är en egen operation för alla roller

E3

## ADR-001: Egen inloggning mot users, ingen extern identitetsleverantör

**Datum:** 2026-10-21 · **Status:** gäller till 4.0 är levererad · **Berör:** E3

### Beslut

AutoCore 4.0 autentiserar mot den egna users-tabellen med PBKDF2-hashar. Keycloak, Entra ID eller annan extern leverantör införs inte i den här etappen.

### Skäl

WAC4_0, avsnitt 4: "Koncernen ställer i denna etapp inget krav på integration med externa identitetsleverantörer." Kravet är i stället att användare kan identifieras med individuella konton, att lösenord inte lagras i klartext, och att tre konton kan demonstreras. Migrering 004 och inloggningen uppfyller det.

Praktiskt väger tre saker tyngre än vad en extern leverantör ger nu: appen är en desktop-app, så rätt flöde vore PKCE med systemwebbläsare och loopback-redirect; det fristående programmet skulle behöva en server för att kunna logga in; och demon ska gå att köra utan att en container är uppe.

### Vad beslutet inte ändrar

Behörighetskontrollen. En token säger vem någon är, inte vad rollen får göra. Spärren i kärnan (ADR-006) behövs lika mycket med en extern leverantör.

### Vad som gör ett byte billigt

Identitet och behörighet är redan åtskilda: Identity och Role bär vem, IdentityHolder håller den under sessionen, RoleMatrix svarar på vad rollen får göra. Ett byte rör bara den första delen, alltså varifrån identiteten kommer.

### Bevis

Migrering 004 i *004_users.sql*, *LoginService.java*, LoginServiceTest och PasswordHashTest.

### När beslutet ska omprövas

Flera applikationer som ska dela inloggning, kontohantering med återställning och MFA, eller krav på revision av inloggningar.

## ADR-002: Spring Boot startar appen, gränssnittet är kvar i JavaFX

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E1

### Beslut

Appen startar i Spring Boot 4.1.1. Gränssnittet är kvar i JavaFX, och appen har ingen webbserver och inget HTTP-gränssnitt. Spring ger kontexten, konfigurationen, loggningen och provstödet.

### Skäl

WAC4_0 §2 pekar på Spring Boot 4.x. Gränssnittet är produktens arbetsyta och byggdes i sprint 1; att byta till en webbklient hade varit ett annat projekt. En desktop-app med en lokal SQLite-fil behöver ingen server för att vara körbar, och kravet på en fristående väg (E2-3) blir enklare att uppfylla när appen inte är en webbserver.

### Följder

JavaFX skapar själv sin Application, så kontexten öppnas i AutoCoreApp.init() och stängs i stop(). Det finns därför två startklasser med olika uppgifter: AutoCoreApplication är Springs ingång och Main är den som sätter skalan och startar fönstret. spring-boot-starter används utan webbmodul. SpringContextTest prövar att kontexten startar med rätt databassökväg.

### Bevis

*pom.xml*, *AutoCoreApplication.java*, *AutoCoreApp.java*, SpringContextTest.

### När beslutet ska omprövas

Om systemet ska användas från flera maskiner samtidigt, eller om kravet ändras till en webbklient. Då behövs en server och en annan ägare av databasen.

## ADR-003: JDK 25 är bygggolvet, trots att kravet säger 17 eller senare

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E1

### Beslut

pom.xml kräver exakt JDK 25, [25,26). Bygget stannar med en läsbar rad om någon kör 17, 21 eller 26.

### Skäl

Golvet sätts av JavaFX, inte av Spring. javafx-base-25.0.4-linux.jar är byggt för Java 23 (class file 67), medan Spring Boot 4 är byggt för Java 17. Under 23 går bygget inte alls. JDK 25 är den version projektet byggs och prövas på, både lokalt och i CI.

WAC4_0 §2 säger "Java 17 eller senare". Kravet är alltså uppfyllt med marginal, men bygget begär mer än kravet gör. Det står uttryckligen i README och i felmeddelandet, så ingen behöver gissa.

### Följder

En granskare med JDK 21 får ett stopp med förklaring i stället för ett class file-fel. CI använder Temurin 25. mise.toml pekar på samma version. Den som bygger på en annan plattform får rätt JavaFX via Maven.

### Bevis

maven-enforcer-plugin i *pom.xml*, *README* under "Innan du bygger", *.github/workflows/ci.yml*.

### När beslutet ska omprövas

Om JavaFX släpper ett bygge för en äldre JDK, eller om kravet skärps till att bygget måste gå på 17 utan undantag.

## ADR-004: Lagringen är en böna med sökvägen i konfigurationen

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E2, E5

### Beslut

Db är en Spring-böna. Sökvägen kommer från autocore.db.path, som kan sättas med AUTOCORE_DB_PATH eller -Dautocore.db.path=.... Varje repository får sin Db via RepositoryFactory, aldrig via en statisk klass.

### Skäl

Den statiska Db var risken R3 i *riskanalysen*. Varje repository läste en sökväg som sattes någon annanstans, och proven kunde bara köras för att klassen råkade vara statisk. En böna ägs av kontexten, och en körning mot en kopia av den riktiga databasen blir en inställning i stället för en kodändring.

### Följder

Db.ensureReady() sätter sin ready-flagga först efter att schemat och demodata är på plats. En start som faller får inte kommas ihåg som klar, för då hade nästa anrop svarat "no such table" i stället för felet som faktiskt hände. Ett prov som körs utan Spring skapar sin egen Db med samma sökväg.

### Bevis

*DatabaseConfig.java*, *Db.java*, SpringContextTest, DbReadyTest, commit b95626a.

### När beslutet ska omprövas

Om databasen flyttar till en server, eller om två processer ska skriva i samma fil. SQLite klarar det senare bara med en tydlig låsstrategi.

## ADR-005: Identiteten bor i kärnan

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E2, E3

### Beslut

IdentityHolder ligger i security i kärnan och känner inte till Spring. Appen fyller den vid inloggning och tömmer den vid utloggning. Konsolen och det fristående programmet skapar egna hållare.

### Skäl

Appen, konsolen och det fristående programmet ska svara lika på frågan "vem är inloggad". Om hållaren låg i gränssnittet skulle de tre vägarna behöva varsin kopia, och spärren skulle inte kunna användas utanför fönstret. Att klassen är vanlig Java gör den också lätt att pröva utan JavaFX.

### Följder

Hållaren använder volatile, inte ThreadLocal: en person är inloggad för hela sessionen, och JavaFX:s bakgrundstrådar måste se samma identitet som fönstret. set(null) är förbjudet; utloggning görs med clear(), så att "ingen inloggad" är ett uttryckligt tillstånd.

### Bevis

*IdentityHolder.java*, IdentityHolderTest, commit 66b56b4.

### När beslutet ska omprövas

Om systemet får flera samtidiga användare i samma process, till exempel en server. Då räcker inte en enda hållare, och identiteten måste följa anropet i stället för processen.

## ADR-006: Behörigheten sitter i en dekoratör framför fasaden

**Datum:** 2026-10-21 · **Status:** gäller · **Berör:** E3

### Beslut

Kontrollen ligger i GuardedGarageSystem, som ligger framför GarageSystem och kräver en operation för varje anrop som läser eller skriver. Gränssnittet anropar spärren, men är inte spärren. Samma klass används av fönstret, konsolen och det fristående programmet.

### Skäl

Om kontrollen satt i gränssnittet skulle varje ny ingång behöva komma ihåg den. Det var precis vad som hände i R6 och R12: läsningarna saknade kontroll, och ConsoleApp byggde tjänsten direkt. En dekoratör framför fasaden ger en plats att läsa regeln på, och en plats att pröva den.

### Följder

Spärren skiljer på "ingen är inloggad" och "rollen får inte", eftersom kravet skiljer på dem: NotAuthenticatedException respektive AccessDeniedException. Läsning och skrivning går genom samma väg. Varje nekad åtkomst skriver en loggrad med användare, roll och operation, men aldrig med det användaren skrev. NoUnguardedConstructionTest fäller en klass som skapar new GarageSystem( direkt.

### Bevis

*GuardedGarageSystem.java*, AuthorizationTest, NoUnguardedConstructionTest, commit 135dd0e, commit 6575338.

### När beslutet ska omprövas

Om behörigheten flyttar till en server eller en gateway. Då ska regeln ägas där, och klienten får bara visa vad servern redan har svarat om.

## ADR-007: Rollmatrisen är data

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E3

### Beslut

RoleMatrix är en oföränderlig tabell med 30 operationer och tre roller. Administratören har allt, verkstadspersonalen har allt utom de åtta administratörsuppgifterna, och en läsbehörig har READ och CHANGE_LANGUAGE.

### Skäl

Regeln ska gå att läsa på ett ställe i stället för som en villkorad sats i varje metod. Att administratören definieras som "alla operationer" gör dessutom att en ny operation är administratörens tills någon uttryckligen tar bort den från personalen. Ett fel i matrisen får inte öppna dörren av misstag.

### Följder

En roll eller operation som saknas ger nej, inte ja. Matrisen kan skrivas ut som text, vilket RoleMatrixProof gör, så regeln går att läsa utan att läsa koden. RoleMatrixTest prövar både tillåtna och nekade anrop.

### Bevis

*RoleMatrix.java*, *Operation.java*, RoleMatrixTest, RoleMatrixProof.

### När beslutet ska omprövas

Om rollerna blir fler än tre, eller om behörigheter ska sättas per användare i stället för per roll. Då behövs en tabell i databasen, inte en konstant i koden.

## ADR-008: Ekonomin tillhör administratören

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E3

### Beslut

Fakturor, betalningar och det som går att fakturera kräver READ_FINANCE, CREATE_INVOICE eller TAKE_PAYMENT. Alla tre är administratörens. Verkstadspersonalen arbetar med kunder, fordon, bokningar, arbetsorder, tjänster och mekaniker, och en läsbehörig ser dagens arbete men inte ekonomin.

### Skäl

Kravet beskriver personalens dag som kunder, fordon och arbetsorder och lägger faktureringen utanför den. Ett konto som får skapa en faktura men inte se den skulle dessutom erbjudas en dörr utan något bakom.

Beslutet ändrades under sprinten. Först hade personalen READ_FINANCE, sedan stängdes ekonomin för både personal och läsbehörig, eftersom en läsbehörig inte ska se vad verkstaden fakturerat. Det är den senare raden i RoleMatrix som gäller.

### Följder

PageRouter frågar bara efter faktura- och betalningsantal när READ_FINANCE är tillåtet, och EntityLookup släpper inte ut en faktura till den som inte får se den. Utan de följdändringarna nekades anrop i bakgrunden och användaren fick "Åtkomst nekad" utan att ha klickat.

### Bevis

ADMIN_ONLY i *RoleMatrix.java*, commit 0ba8ab9, commit 12577e4.

### När beslutet ska omprövas

Om verkstaden vill att personalen ska kunna ta emot betalning vid disken. Då är TAKE_PAYMENT det som flyttas, inte hela ekonomin.

## ADR-009: Fönstret frågar spärren innan det ritar

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E3

### Beslut

Gränssnittet frågar GarageSystem.may(Operation) innan det visar en kontroll. Actions gömmer kontrollen och tar bort dess plats i layouten. Kontrollen får också ett filter som konsumerar händelsen om någon ändå når den. Under båda lagren ligger spärren kvar.

### Skäl

Teamet beslutade att stänga av funktioner för obehöriga i stället för att låta dem klicka och få ett nej. WAC4_0 §5 säger att det inte räcker att enbart dölja, så spärren är orörd: att dölja är artigheten, spärren är regeln. Ett filter före knappens handler behövs också, för ett event kan fyras från kod utan att någon klickar.

### Följder

Tre lager, och alla behövs. GarageSystem.may() svarar ja, medan GuardedGarageSystem.may() svarar med rollmatrisen och nej när ingen är inloggad. Actions loggar ett försök som når fram. Ett direkt anrop till tjänsten nekas av dekoratören, oavsett vad fönstret visar. LayoutStillnessTest ser till att gömda kontroller inte lämnar ett hål som flyttar resten av layouten.

### Bevis

*Actions.java*, commit e4716eb, commit 12577e4.

### När beslutet ska omprövas

Om gränssnittet byts mot ett webbgränssnitt. Då är det serverns svar som avgör vad som ritas, och klienten får inte vara den som tolkar rollen.

## ADR-010: Loggning med profil per miljö och trace-id i MDC

**Datum:** 2026-10-21 · **Status:** gäller · **Berör:** E4

### Beslut

Logback konfigureras i logback-spring.xml med två profiler. Utan profil, eller i dev, loggas vår egen kod på DEBUG till både konsol och fil. I prod loggas vår kod på INFO och allt annat på WARN, och bara till fil. Varje rad bär tidpunkt, nivå, tråd, trace-id och operation.

### Skäl

Kravet skiljer på vad som ska synas i utveckling och i drift, och en loggrad utan trace-id går inte att knyta till anropet som skapade den. Att lägga trace-id och operation i MDC gör att varje rad som skrivs under ett anrop får dem utan att varje loggning behöver skicka med dem.

### Följder

Filen rullar vid 10 MB, sparas i fjorton dagar och har ett tak på 200 MB. Katalogen kan flyttas med AUTOCORE_LOG_DIR. Läsningar ansluter till pågående trace-id i stället för att starta ett nytt, så en läsning som görs inuti en ändring hör till samma anrop. Det fristående programmet kan inte läsa logback-spring.xml, eftersom det inte har Spring, och använder därför logback-standalone.xml med samma mönster.

### Bevis

*logback-spring.xml*, *logback-standalone.xml*, LogContentTest, *demo-loggning.md*.

### När beslutet ska omprövas

Om loggen ska samlas in centralt. Då behövs ett appender för det, men mönstret och trace-id:t kan vara kvar.

## ADR-011: Användaren får en mening, loggen får detaljerna

**Datum:** 2026-10-21 · **Status:** gäller · **Berör:** E4

### Beslut

Ett fel som når användaren är en textnyckel med en mening på användarens språk. Undantaget, orsaken och stacken stannar i loggen. Nekad åtkomst skriver sin rad där den kastas, inte där den visas.

### Skäl

Ett tekniskt nej ska gå att felsöka utan att användaren får klassnamn eller SQL. Att raden skrivs där undantaget kastas gör dessutom att en anropare som glömmer logga inte kan få ett nej att försvinna ur loggen.

### Följder

AppException bär textnyckeln, och DatabaseInitException svarar view.error.startup i stället för att visa SQL-felet. AccessDeniedException och NotAuthenticatedException är tysta i konstruktorn, eftersom spärren redan har skrivit vem som försökte vad. Det fristående programmet skriver bara ut meningen, inte den dubbla raden.

### Bevis

*AppException.java*, *demo-loggning.md*, commit d74a169, EventLoggingTest.

### När beslutet ska omprövas

Om fel ska visas i ett API med felkoder för en klient. Då behövs en stabil kod utöver meningen, men detaljerna hör fortfarande till loggen.

## ADR-012: Migreringarna är en kedja, databasen skapas aldrig om

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E5

### Beslut

Schemat byggs av fyra filer som körs i ordning: 001_baslinjen.sql, 002_nycklar.sql, 003_ore.sql och 004_users.sql. Vilka som har körts står i schema_version. En ny ändring får nästa nummer och läggs sist. Att börja om med en tom databas är uteslutet.

### Skäl

Kravet är att befintlig information bevaras. En omskapad databas hade tappat kunder, fordon och arbetsorder, och OldDatabaseProof hade inte kunnat köras. Migreringen måste därför vara additiv och gå att köra om utan att göra något andra gången.

### Följder

Varje fil körs i en transaktion, och schema_version skrivs i samma transaktion som ändringen. Främmande nycklar stängs av under en ombyggnad, eftersom SQLite inte kan växla dem inuti en transaktion, och kontrolleras efteråt. En äldre databas som saknar kolumner får dem tillagda innan filerna körs, och bara de som saknas. Db.ensureReady() markerar inte databasen som klar förrän allt har gått igenom.

### Bevis

*Migrations.java*, *bevis/regressionsjamforelse.md*, OldDatabaseProof, DbReadyTest.

### När beslutet ska omprövas

Om databasen byts mot ett system med egna migreringsverktyg. Kedjan och registret är då fortfarande principen, men filerna ägs av verktyget.

## ADR-013: Det fristående programmet är en egen startväg med samma spärr

**Datum:** 2026-10-20 · **Status:** gäller · **Berör:** E2

### Beslut

StandaloneWorkOrders är ett vanligt Java-program utan Spring, utan gränssnitt och utan webbserver. Det skapar sin egen Db, fyller en IdentityHolder och använder samma GuardedGarageSystem som appen.

### Skäl

Kravet på fristående verksamhetsfunktionalitet ska visas utan att appen eller en server är igång. Om programmet byggde tjänsten direkt skulle det samtidigt vara en väg förbi spärren, vilket R12 visade när konsolen gjorde precis det.

### Följder

Programmet väljer en öppen dag ur OpeningHours i stället för att anta dagens datum plus trettio, så det inte faller på en lördag. Valet ligger i en egen metod som tar startdagen som argument, så det går att pröva med ett fast datum. Loggningen laddas från logback-standalone.xml. Argumentet utan lämnar hållaren tom och visar det nekade svaret.

### Bevis

*StandaloneWorkOrders.java*, StandaloneWorkOrdersTest, commit 18fcb52, commit c50c88d.

### När beslutet ska omprövas

Om det fristående programmet ska bli en tjänst som anropas av något annat. Då är det inte längre fristående i den mening kravet menar.

## ADR-014: Bevisen ligger utanför produktionsträdet

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E3, E5

### Beslut

Bevisprogrammen ligger i src/test/java/com/wac/autocore/proof/, inte i src/main. De följer alltså inte med i jar-filen. Surefire hoppar över paketet, eftersom SelfTest annars hade setts som ett JUnit-prov på namnet. Varje program körs för hand med target/test-classes först i classpath.

### Skäl

Tio av programmen byggde tjänsten förbi behörighetsspärren. Så länge de låg i produktionsträdet fanns en genväg i den levererade artefakten. Ett bevis ska visa att koden gör rätt, inte vara en dörr in i den.

### Följder

Femton bevisprogram finns, och elva av dem har en baslinje från Java 8. Båda sidornas utskrifter ligger i *bevis/*, så jämförelsen går att göra om från en färsk klon. Ett bevis räknas bara om det går att köra om; en commit som nämner ett kort räknas inte.

### Bevis

*pom.xml*, *bevis/README.md*, *README* under "Prov och bevis", commit 8ee9a3c.

### När beslutet ska omprövas

Om bevisen flyttas till ett eget modul eller repo. Kravet på att de ska gå att köra om från det som lämnas in är detsamma.

## ADR-015: Skalan är 1 när appen startar

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E1

### Beslut

Main sätter glass.gtk.uiScale till 1 innan JavaFX startar. start.sh sätter GDK_SCALE i stället för att ärva det. Den som har en skärm som behöver en annan skala kan sätta AUTOCORE_UI_SCALE.

### Skäl

JavaFX frågar GTK hur stort allt ska vara, och svaret kommer från GDK_SCALE. En session som startades innan skärmens skala sänktes behåller det gamla värdet, och då ritas varje knapp, varje typsnitt och hela fönstret dubbelt så stort. Ingen kod hade ändrats, vilket gjorde felet svårt att hitta. En varning i start.sh hjälper inte den som redan har problemet, och syns inte alls när appen startas från IntelliJ.

### Följder

Appen ser likadan ut oavsett vilken skalinställning terminalen råkade ha. MainScaleTest prövar egenskapen, och mätningen i commiten visar att inloggningskortets yta går från 714×810 till 354×405 med rätt värde.

### Bevis

*Main.java*, MainScaleTest, commit 71571f8, *README* under "Skalan gränssnittet ritas i".

### När beslutet ska omprövas

Om appen ska följa operativsystemets skalning automatiskt i stället för att börja på 1.

## ADR-016: Native access deklareras i manifestet

**Datum:** 2026-10-22 · **Status:** gäller · **Berör:** E1

### Beslut

Jar-filen bär Enable-Native-Access: ALL-UNNAMED i manifestet, satt av maven-jar-plugin. start.sh skickar --enable-native-access=ALL-UNNAMED till Maven-körningen, spring-boot-maven-plugin har den i sin konfiguration, och README använder den när det fristående programmet startas med java -cp.

### Skäl

SQLite-drivrutinen använder inbyggd kod. På JDK 25 ger en körning utan flaggan varningen "Restricted methods will be blocked in a future release". Att bara dokumentera flaggan för en av startvägarna räcker inte; den ska gälla alla.

### Följder

java -jar startar utan varning. Den som kör det fristående programmet med java -cp måste fortfarande ange flaggan, och det står i README. BuildFlagsTest läser pom.xml, start.sh och README och faller om flaggan försvinner från någon av dem.

### Bevis

*pom.xml*, BuildFlagsTest, commit 6ee586c.

### När beslutet ska omprövas

Om drivrutinen slutar använda inbyggd kod, eller om JDK:t kräver en annan flagga.

## ADR-017: Språkvalet är en egen operation för alla roller

**Datum:** 2026-10-21 · **Status:** gäller · **Berör:** E3

### Beslut

CHANGE_LANGUAGE är en egen operation, skild från CHANGE_SETTINGS. Alla tre rollerna har den.

### Skäl

Språkvalet säger något om personen som tittar på fönstret, inte om hur verkstaden drivs. Att lägga det under CHANGE_SETTINGS, som bara administratören har, gjorde att personal och läsbehörig tappade sitt språkval. Det upptäcktes i SCRUM-325: en inställning som hörde till personen låg under en behörighet som hörde till rollen.

### Följder

En läsbehörig får ändra språk men inget annat. Språkvalet sparas i inställningstabellen och överlever en omstart, och I18n kan byta språk utan omstart.

### Bevis

CHANGE_LANGUAGE i *Operation.java*, READING i *RoleMatrix.java*, och korten SCRUM-324 och SCRUM-325.

### När det ska omprövas

Om språkvalet flyttar till en användarprofil som ägs utanför appen. Då är det fortfarande en personlig inställning, men den behöver inte vara en operation i rollmatrisen.

## Arbetsregler som följer av besluten

Tre regler styr hur arbetet kontrolleras. De är inte arkitektur i sig, men utan dem går besluten inte att lita på.

**Ett kort stängs bara med ett bevis som går att köra om.** Ett bevis är ett prov som fäller när beteendet försvinner, ett dokument i docs/ med ett kommando som går att köra om, eller en fil i docs/bevis/ med utskriften och hur den togs. En commit som nämner nyckeln räknas inte, eftersom automationen stänger kortet på just det. Regeln står i *README* och kom till efter genomgången i SCRUM-360.

**Ett CI-flöde per gren.** .github/workflows/ci.yml har en concurrency-grupp per ref och avbryter den äldre körningen. Utan den startar varje push två körningar på en gren med öppen PR, och de slåss om samma runners.

**Siffrorna i dokumenten kontrolleras mot koden.** Provantal, operationer, bevis och migreringar räknas om med kommandona i *sprintsammanställningen* innan dokumenten delas. Under sprinten rättades provantalet flera gånger, och planens antal operationer rättades till 30 när READ_FINANCE tillkom.

## Så läggs ett beslut till

Nästa beslut får nästa lediga nummer och samma fem rubriker. Status är gäller, prövas eller ersatt av ADR-NNN. Ett beslut som ersätts tas inte bort; det får en rad om varför det inte längre gäller, så att historiken går att läsa.
