# Feillogging

WARN og ERROR er navngitte hendelser med `event_type` og avgrensede felt.
Klienter som logger egne feil, gjør det én gang med oppstrømsdetaljene. Når feilen
når API-grensen, logger den i tillegg én `api_request_failed` for requesten.
Feil fra PDL og Dine sykmeldte logges bare av eieren: API-grensen, konsumenten
eller scheduleren. Kafka-forsøk der retry-policyen bestemmer neste steg logges som WARN;
endelige feil logges som ERROR.

## Diagnostikk

`withFailureDiagnostics` legger til `exception_type`, `cause_type` og `stack_trace`
med typenavn og maksimalt 30 rammer per årsak, aldri unntaksmeldinger.
`exception_type` og `cause_type` følger
[team-esyfos runtime-feilkontrakt](https://navikt.github.io/team-esyfo/utvikling/observability/runtime-feilkontrakt)
og slutter alltid på `Error` eller `Exception`. Når de finnes, legges også
`upstream_status` (100–599), gyldig `sql_state` og unntakets egne diagnosefelt
til, for eksempel `pdl_operation` og `pdl_errors`.

API-grensen logger 4xx som WARN `api_request_invalid` med `error_type`, 5xx som
ERROR `api_request_failed` og kansellering som WARN `api_request_cancelled`.
Egne oppslagsfeil beholder sine navngitte hendelser. 4xx og kansellering logges
uten stackspor eller unntaksmeldinger. `api_request_rejected` er forbeholdt
bevisste domeneavvisninger med en konkret `rejection_reason`, siden dashboardet
teller den som registrert avvisning.

## Personvern

Ikke logg personidentifikatorer, navn, organisasjonsnummer, token, forespørsels-
eller svarinnhold, unntaksmeldinger eller møtebehov og helseopplysninger.
PDL `errors[]` kan logges som `pdl_errors`; aldri PDL `data` eller variabler.
Bruk bare trygge, avgrensede felter for operasjon, status og korrelasjon.

## Testing

Test eieren med `captureApplicationLogs`, som bruker produksjonens JSON-enkoder
og avviser dupliserte JSON-nøkler. Sjekk nivå, antall hendelser og diagnosefelt,
og bruk kanariverdier for å kontrollere at sensitive data ikke havner i loggen.
