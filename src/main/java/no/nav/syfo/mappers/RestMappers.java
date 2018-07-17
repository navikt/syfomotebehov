package no.nav.syfo.mappers;

import no.nav.syfo.domain.rest.LagreMotebehov;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.Person;
import no.nav.syfo.repository.domain.PMotebehov;

import java.util.function.Function;

import static no.nav.syfo.consumer.ws.AktoerConsumer.aktoerConsumer;

public class RestMappers {

    public static Function<LagreMotebehov, Motebehov> lagreMotebehov2motebehov = lagreMotebehov ->
            new Motebehov()
                    .arbeidstaker(new Person()
                            .fnr(lagreMotebehov.arbeidstakerFnr())
                    )
                    .motebehovSvar(lagreMotebehov.motebehovSvar());

    public static Function<PMotebehov, Motebehov> motebehov2rs = motebehov ->
            new Motebehov()
                    .id(motebehov.getUuid())
                    .opprettetDato(motebehov.getOpprettetDato())
                    .opprettetAv(motebehov.getOpprettetAv())
                    .arbeidstaker(new Person()
                            .fnr(aktoerConsumer().hentFnrForAktoerId(motebehov.getAktoerId()))
                    )
                    .motebehovSvar(new MotebehovSvar()
                            .friskmeldingForventning(motebehov.getFriskmeldingForventning())
                            .tiltak(motebehov.getTiltak())
                            .tiltakResultat(motebehov.getTiltakResultat())
                            .harMotebehov(motebehov.isHarMotebehov())
                            .forklaring(motebehov.getForklaring())
                    );
}
