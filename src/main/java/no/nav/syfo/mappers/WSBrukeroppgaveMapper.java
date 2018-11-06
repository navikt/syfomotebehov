package no.nav.syfo.mappers;

import no.nav.syfo.mappers.domain.Brukeroppgave;
import no.nav.tjeneste.domene.digisyfo.brukeroppgave.v1.informasjon.WSBrukeroppgave;

import java.util.function.Function;

public class WSBrukeroppgaveMapper {
    public static Function<WSBrukeroppgave, Brukeroppgave> ws2Brukeroppgave = wsBrukeroppgave -> new Brukeroppgave()
            .oppgaveUUID(wsBrukeroppgave.getOppgaveUuid())
            .oppgavetype(wsBrukeroppgave.getOppgavetype())
            .ident(wsBrukeroppgave.getIdent())
            .ressursId(wsBrukeroppgave.getRessursId())
            .ressursEier(wsBrukeroppgave.getRessurseier())
            .opprettetTidspunkt(wsBrukeroppgave.getOpprettettidspunkt());
}
