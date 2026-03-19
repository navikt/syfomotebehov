---
description: Sett opp Kafka-topic og consumer for team-esyfo — topicconfig, consumer, feilhåndtering
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Kafka-topic og consumer

Bruk denne skillen når du skal sette opp en ny Kafka-topic-konfigurasjon og en consumer for team-esyfo.

## Fremgangsmåte

1. Les eksisterende NAIS-manifest for å finne Kafka pool-konfigurasjon som allerede brukes i repoet.
2. Søk i kodebasen etter eksisterende Kafka-consumere og følg etablerte mønstre for struktur, oppsett og navngivning.
3. Sjekk `build.gradle.kts` for Kafka-avhengigheter, og se også på eksisterende consumer- og producer-implementasjoner før du skriver noe nytt.

## Sjekkliste

- [ ] Legg til Kafka pool i NAIS-manifestet hvis den ikke allerede finnes
- [ ] Opprett consumer-klassen etter etablerte mønstre i repoet
- [ ] Definer message payload- og key-typer som matcher topic-skjemaet
- [ ] Implementer idempotent behandling der det er nødvendig
- [ ] Legg inn feilhåndtering og logging som er konsistent med eksisterende consumere
- [ ] Legg til metrikker for antall prosesserte events og tidsbruk for prosessering
- [ ] Bruk strukturert logging med relevante identifikatorer — aldri logg sensitive data (fødselsnummer, tokens, personnavn)
- [ ] Skriv tester etter eksisterende Kafka-testmønstre i repoet
