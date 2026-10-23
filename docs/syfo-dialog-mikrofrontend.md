# Microfrontenden syfo-dialog via Budstikka

`syfo-dialog` er kortet «Dialogmøte med Nav» på Min side. Det viser dialogmøtet når siste brev er en
innkalling eller endring, ellers møtebehovet når et svar etterspørres, og ellers ingenting. Både
isdialogmote og syfomotebehov skrur kortet av og på, og i Budstikka vinner siste kommando per person.

## Regelen

Fordi kortet ikke viser noe når ingenting er aktuelt, er en overflødig enable ufarlig. Den eneste farlige
feilen er en disable mens et dialogmøte er aktivt. syfomotebehov følger derfor én regel:

- **Enable** når svar på møtebehov etterspørres. Det skjer bare når det ikke finnes et planlagt møte.
- **Disable** når møtebehovet ferdigstilles (svar, «kan ikke svare» og avsluttet kandidat), men **aldri
  mens personen har et aktivt dialogmøte**. Ved innkalling sendes aldri disable.

Et møte er aktivt så lenge `DialogmoteStatusService` har det lagret: etter innkalling, nytt tid/sted
eller endring, til avlysning eller referat.

## Utsending

Kommandoene lagres i `BUDSTIKKA_MIKROFRONTEND_UTSENDING` med en `EventId` før første sending.
`BudstikkaMikrofrontendScheduler` sender ventende kommandoer til `team-esyfo.budstikka.v1` og gjenbruker
samme `EventId` ved retry. En kommando sendes ikke før eldre ventende kommandoer for samme person er sendt.
En disable sjekkes mot aktive møter på nytt rett før sending, fordi en innkalling kan ha kommet etter at
den ble lagt i kø.

## Overgang fra esyfovarsel

`TOGGLE_MOTEBEHOV_MIKROFRONTEND_VIA_BUDSTIKKA` slår på sendingen via Budstikka. Samme flagg i esyfovarsel
gjør at `SM_DIALOGMOTE_SVAR_MOTEBEHOV` slutter å påvirke kortet der. Flagget må slås om samtidig i begge
appene. syfomotebehov må over før eller samtidig med isdialogmote. Flytter isdialogmote først, ser ikke
esyfovarsel lenger dialogmøtet, og skrur av kortet når møtebehovet ferdigstilles ved innkalling.

Møtebehovvarslene (brukervarsel, Dine Sykmeldte og så videre) går fortsatt via esyfovarsel.
