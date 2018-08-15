package no.nav.syfo.mappers;

import no.nav.syfo.domain.rest.Fnr;
import no.nav.syfo.domain.rest.LagreMotebehov;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.repository.domain.PMotebehov;

import java.util.function.Function;

import static no.nav.syfo.consumer.ws.AktoerConsumer.aktoerConsumer;

public class RestMappers {

    public static Function<LagreMotebehov, Motebehov> lagreMotebehov2motebehov = lagreMotebehov ->
            new Motebehov()
                    .arbeidstaker(lagreMotebehov.arbeidstakerFnr())
                    .motebehovSvar(lagreMotebehov.motebehovSvar());

    public static Function<PMotebehov, Motebehov> motebehov2rs = pMotebehov ->
            new Motebehov()
                    .id(pMotebehov.getUuid())
                    .opprettetDato(pMotebehov.getOpprettetDato())
                    .opprettetAv(pMotebehov.getOpprettetAv())
                    .arbeidstaker(new Fnr(aktoerConsumer().hentFnrForAktoerId(pMotebehov.getAktoerId())))
                    .virksomhetsnummer(pMotebehov.getVirksomhetsnummer())
                    .motebehovSvar(new MotebehovSvar()
                            .friskmeldingForventning(pMotebehov.getFriskmeldingForventning())
                            .tiltak(pMotebehov.getTiltak())
                            .tiltakResultat(pMotebehov.getTiltakResultat())
                            .harMotebehov(pMotebehov.isHarMotebehov())
                            .forklaring(pMotebehov.getForklaring())
                    );
}
