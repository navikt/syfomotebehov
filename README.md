# syfo-dialogmotebehov

syfo-dialogmøtebehov er en mikrotjeneste i POC-en Robot-Berit. syfo-dialogmotebehov lagrer data om behovet for et
dialogmøte, dette rapportertes inn av den sykemeldtes nærmeste leder.

## Lokal utvikling 

### Oppstart
Start opp appen fra [LocalApplication.java](../syfo-dialogmotebehov/src/test/java/no/nav/syfo/LocalApplication.java).

Har du IntelliJ satt opp med Spring-støtte, må du i Run/Debug configuration endre Spring boot modulen til å bruke
testversjonen og ikke prodversjonen av applikasjonen:

```
Run > Edit Configurations > Spring Boot > Local Application > Configuration
Endre på følgende felter:
Name --> LocalApplication
Main --> no.nav.syfo.LocalApplication
```

Appen kjører da på localhost:8080/

### Properties

Se [ApplicationCofig](../syfo-dialogmotebehov/src/test/java/no/nav/syfo/config/ApplicationConfigTest.java)

### Testing

### Bygging

### Deploy


## Database
Appen kjører med en lokal H2 in-memory database. Den spinnes opp som en del av applikasjonen og er 
også tilgjengelig i tester. Du kan logge inn og kjøre spørringer på:
`localhost/h2` med jdbc_url: `jdbc:h2:mem:testdb`