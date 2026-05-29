# syfomotebehov

[![Build & Deploy](https://github.com/navikt/syfomotebehov/actions/workflows/build-and-deploy.yaml/badge.svg)](https://github.com/navikt/syfomotebehov/actions/workflows/build-and-deploy.yaml)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)

`syfomotebehov` er en Spring Boot-applikasjon for møtebehov i sykefraværsoppfølging. Tjenesten lagrer møtebehov fra arbeidstaker og arbeidsgiver, og gjør informasjonen tilgjengelig for veiledere som følger opp saken.

## Formål

Tjenesten støtter tre hovedflater:

- **Arbeidstaker** kan hente, sende inn og ferdigstille eget møtebehov.
- **Arbeidsgiver** kan hente og sende inn møtebehov for arbeidstaker.
- **Veileder** kan hente møtebehov og historikk, behandle behov og sende tilbakemelding.

Applikasjonen kjører på NAIS, lagrer data i PostgreSQL og bruker Kafka både til innlesing av hendelser og utsending av varsel.

## API

Alle endepunkter ligger under URL-prefikset `/syfomotebehov`.

### Arbeidstaker API (TokenX)

- **GET** `/api/v4/arbeidstaker/motebehov`: henter gjeldende møtebehov
- **POST** `/api/v4/arbeidstaker/motebehov`: lagrer møtebehov
- **POST** `/api/v4/arbeidstaker/motebehov/ferdigstill`: ferdigstiller møtebehov

### Arbeidsgiver API (TokenX)

- **GET** `/api/v4/motebehov`: henter møtebehov for arbeidstaker
- **POST** `/api/v4/motebehov`: lagrer møtebehov for arbeidstaker

### Veileder API (Azure AD)

- **GET** `/api/internad/v4/veileder/motebehov`: henter møtebehov for person
- **GET** `/api/internad/v4/veileder/historikk`: henter historikk for person
- **POST** `/api/internad/v4/veileder/motebehov/tilbakemelding`: sender tilbakemelding
- **POST** `/api/internad/v4/veileder/motebehov/behandle`: markerer møtebehov som behandlet

### Interne endepunkter

- **GET** `/internal/isAlive`
- **GET** `/internal/isReady`

## Integrasjoner

- **Innkommende trafikk:** `dialogmote-frontend`, `dialogmote-microfrontend`, `syfomodiaperson`, `isdialogmote` og `ditt-sykefravaer`
- **Utgående kall:** `syfobrukertilgang`, `istilgangskontroll`, `syfobehandlendeenhet`, `isnarmesteleder` og PDL
- **Kafka-consumere:**
  - `teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person`
  - `teamsykefravr.isdialogmotekandidat-dialogmotekandidat`
- **Kafka-produsent:** `team-esyfo.varselbus`
- **Database:** PostgreSQL 14 i GCP

## Utvikling

- Repoet bruker `mise` for lokale oppgaver og verktøyversjoner.
- Kjør `mise tasks` for å se tilgjengelige oppgaver for lokal oppstart, formattering, linting og tester.
- Docker må kjøre i bakgrunnen fordi Testcontainers starter lokale avhengigheter.
- Lokal utvikling bruker `src/test/kotlin/no/nav/syfo/LocalApplication.kt`, profilen `local`, MockOAuth2Server og Testcontainers for PostgreSQL og Kafka.
- Appen er tilgjengelig på [http://localhost:8811/syfomotebehov](http://localhost:8811/syfomotebehov).
- Hvis du vil beholde data mellom oppstarter, sett `testcontainers.reuse.enable=true` i `~/.testcontainers.properties`.

## Bruno

Bruno-collectionen ligger i [`bruno/`](./bruno/). Lokal auth bruker MockOAuth2Server, og beskyttede kall må sende bearer-token i `Authorization`-headeren. Der endepunktene er versjonert, er siste versjon lagt inn i Bruno.

## Les mer

- [Dialogmøtekandidat – varsel-flyt](./docs/dialogmotekandidat-varsel-flow.md)
- [Lokal konfigurasjon](./src/test/resources/application.yaml)

## Kontakt

Team eSYFO: [#esyfo på Slack](https://nav-it.slack.com/archives/C012X796B4L)
