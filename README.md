# syfomotebehov

Syfomotebehov lagrer data om behovet for et dialogmøte. Den sykmeldte og dens arbeidsgiver rapporter dette behovet.

syfomotebehov er en springboot-applikasjon basert på https://github.com/navikt/syfospringboot-kickstarter. Den er
satt opp til å kjøre på nais.

## Lokal utvikling 

### Oppstart

Start opp appen fra [LocalApplication.java](../syfomotebehov/src/test/java/no/nav/syfo/LocalApplication.java).

Har du IntelliJ satt opp med Spring-støtte, må du i Run/Debug configuration endre Spring boot modulen til å bruke
testversjonen og ikke prodversjonen av applikasjonen:

```
Run > Edit Configurations > Spring Boot > Local Application > Configuration
Endre på følgende felter:
Name --> LocalApplication
Main --> no.nav.syfo.LocalApplication
```

Appen kjører da på localhost:8811/

### Properties

Se [ApplicationConfig](../syfomotebehov/src/test/java/no/nav/syfo/config/ApplicationConfigTest.java)

### Testing

Enhetstester er satt opp med in-memory db og kan kjøres på vanlig vis: **mvn test**.

### Bygging

Applikasjonen pakkes til en stor jar vha spring-boot-maven-plugin, og bygges med docker. Applikasjonen kan kjøres opp 
lokalt på docker hvis jdbc-url legges på path.

### Pipeline

Pipeline er på Github Action.
Commits til Master-branch deployes automatisk til dev-fss og prod-fss.
Commits til ikke-master-branch bygges uten automatisk deploy.


## Database
Appen kjører med en lokal H2 in-memory database. Den spinnes opp som en del av applikasjonen og er 
også tilgjengelig i tester. Du kan logge inn og kjøre spørringer på:
`localhost/h2` med jdbc_url: `jdbc:h2:mem:testdb`
