# syfomotebehov

[![Build & Deploy](https://github.com/navikt/syfomotebehov/actions/workflows/build-and-deploy.yaml/badge.svg)](https://github.com/navikt/syfomotebehov/actions/workflows/build-and-deploy.yaml)

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=Kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=Spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Postgresql](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/g/)


Syfomotebehov lagrer data om behovet for et dialogmøte. Den sykmeldte og dens arbeidsgiver rapporter dette behovet.

syfomotebehov er en springboot-applikasjon basert. Den er satt opp til å kjøre på nais.

## Lokal utvikling
**Avhengigheter**
* For å kjøre med mockede eksterne tjenester må spring profilen "local" være aktivert.
* En docker engine må kjøre i bakgrunnen for at testcontainers skal kunne kjøre Kafka og Postgres.

Start opp appen fra [LocalApplication.kt](./src/test/kotlin/no/nav/syfo/LocalApplication.kt).

Appen kjører da på localhost:8811

#### IntelliJ
Har du IntelliJ satt opp med Spring-støtte, må du i Run/Debug configuration endre Spring boot modulen til å bruke
testversjonen og ikke prodversjonen av applikasjonen:

```
Run > Edit Configurations > Spring Boot > Local Application > Configuration
Endre på følgende felter:
Name --> LocalApplication
Main --> no.nav.syfo.LocalApplication
Active profiles --> local
```

#### Mise/Gradle
Mise:
```bash
mise start
```
Gradle:
```bash
./gradlew bootRunLocal
```

#### Persistent storage

For å beholde data mellom oppstarter av appen, må man legge til følgene config i ~/.testcontainers.properties:
`testcontainers.reuse.enable=true`

```bash
echo "testcontainers.reuse.enable=true" >> ~/.testcontainers.properties
```

#### Mock eksterne tjenester
Stubs for eksterne tjenester ligger under /test/.../stubs og benyttes i "local"-profilen. De ble innført etter at testene ble skrevet, og benyttes
ikke i disse.

#### Mock auth
Det er satt opp egne endepunkter som kjører med "local"-profilen for å mocke auth.

Kall mot beskyttede endepunkter må sende tokenet i `Authorization: Bearer <token>`. `ID_token`-cookie brukes ikke lenger for backend-auth.

### Bruno
Bruno collection ligger under `/bruno` og bruker MockOauth2Server for auth

Det er satt variabler på collection- og sub folder-nivå for å konfigurere fødselsnummer, hvilken type auth som benyttes
og andre felles verdier.

`arbeidsgiver` `arbeidstaker` og `veileder` er satt opp med pre request scripts som kaller token-endepunktene til MockOauth2Server og setter bearer-token i collection variabler som må sendes i `Authorization`-headeren.

Alle kall mot `/api/internad/v4/veileder/**` må bruke `Authorization: Bearer <token>`. Klienter som fortsatt baserer seg på `ID_token`-cookie må oppdateres.

Der hvor endepunkter er versjonert, er kun siste versjon lagt til i Bruno. p.t. møtebehov v4.
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
