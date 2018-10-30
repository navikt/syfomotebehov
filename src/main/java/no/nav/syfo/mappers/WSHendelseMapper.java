package no.nav.syfo.mappers;

import no.nav.syfo.mappers.domain.Hendelse;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSHendelse;

import java.util.function.Function;

public class WSHendelseMapper {
    public static Function<WSHendelse, Hendelse> ws2Hendelse = wsHendelse -> new Hendelse()
            .hendelseId(wsHendelse.getId())
            .tidspunkt(wsHendelse.getTidspunkt())
            .type(wsHendelse.getType())
            .aktorId(wsHendelse.getAktoerId());
}
