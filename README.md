# syfomotebehov

Syfomotebehov lagrer data om behovet for et dialogmøte. Den sykmeldte og dens arbeidsgiver rapporter dette behovet.

syfomotebehov er en springboot-applikasjon basert. Den er satt opp til å kjøre på nais.

## Lokal utvikling 


### Oppstart

Start opp appen fra [LocalApplication.kt](./src/test/kotlin/no/nav/syfo/LocalApplication.kt).

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

Se [application.yaml](./src/test/resources/application.yaml)

### Testing

Enhetstester er satt opp med in-memory db og kan kjøres på vanlig vis: **./gradlew test**.

### Bygging

Applikasjonen pakkes til en stor jar vha. plugin Gradle Shadow og bygges med docker. Applikasjonen kan kjøres opp 
lokalt på docker hvis jdbc-url legges på path.

### Pipeline

Pipeline er på Github Action.
Commits til main-branch deployes automatisk til dev-gcp og prod-gcp.
Commits til ikke-main-branch bygges automatisk deploy til dev-gcp.

## Database
Appen kjører med en lokal in-memory database. Den spinnes opp som en del av applikasjonen og er 
også tilgjengelig i tester. 

## Alerterator
Syfomotebehov er satt opp med alerterator, slik når appen er nede vil det sendes en varsling til Slack kanalene #veden-alerts.
Spec'en for alerts ligger i filen alerts.yaml. Hvis man ønsker å forandre på hvilke varsler som skal sendes må man forandre
på alerts.yaml og så deploye alerts i NAIS av pipeline. Evt. kan man kjøre:
`kubectl apply -f alerts.yaml`.
For å se status på syfomotebehov sine alerts kan man kjøre:
`kubectl describe alert syfomotebehov-alerts`.
Dokumentasjon for Alerterator ligger her: https://doc.nais.io/observability/alerts

## Kontakt
Team eSYFO - #esyfo
