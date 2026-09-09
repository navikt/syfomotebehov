---
description: "Nav-sikkerhetsstandarder — NAIS accessPolicy, hemmeligheter, PII, nettverkspolicyer"
applyTo: "**"
---

# Sikkerhet — Nav

Referanse: [sikkerhet.nav.no](https://sikkerhet.nav.no)

## NAIS-plattformen

- Autentisering og hemmeligheter håndteres av NAIS — sjekk manifestet
- Dependabot og Trivy for sårbarhetsskanning
- Chainguard/Distroless base images

## Nettverkspolicyer

Default-deny. Alle tilganger må deklareres eksplisitt:

```yaml
accessPolicy:
  inbound:
    rules:
      - application: calling-app
        namespace: team-calling
  outbound:
    rules:
      - application: target-app
        namespace: team-target
    external:
      - host: api.example.com
```

## Boundaries

- Parameteriserte spørringer — aldri string-interpolasjon i SQL
- Valider input ved systemgrenser
- Aldri logg PII (fødselsnummer, tokens, personnavn)
- Aldri commit hemmeligheter
- Eksplisitt `accessPolicy` i NAIS-manifest

## Syfomotebehov: data og tilgang

- Behovsvurderinger og skjemasvar kan inneholde sensitive helseopplysninger.
  Fødselsnummer, tokens og skjemainnhold skal ikke inn i vanlige logger eller
  metrikklabler.
- Arbeidstaker- og arbeidsgiver-API-ene bruker TokenX. Bevar `acr`-kontrollen
  i `@ProtectedWithClaims` og klientvalideringen i `TokenXUtil`.
- Veileder-API-et bruker Azure AD v2 og personident fra `Nav-Personident`.
  Tilgang til personen kontrolleres gjennom `VeilederTilgangConsumer` mot
  `istilgangskontroll`; autentisering alene gir ikke persontilgang.
- Bevar eksisterende tokenutveksling i `consumer/tokenx/tokendings/` og
  Azure AD-klientene. Endringer i tilgang, persondataeksponering, lagringstid
  eller sletting må være omfattet av oppdraget og gjennomgås særskilt.
