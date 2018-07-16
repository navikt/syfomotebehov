package no.nav.syfo.mappers;

import no.nav.syfo.domain.rest.LagreMotebehov;
import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.Person;
import no.nav.syfo.repository.domain.PDialogmotebehov;

import java.util.function.Function;

import static no.nav.syfo.consumer.ws.AktoerConsumer.aktoerConsumer;

public class RestMappers {

    public static Function<LagreMotebehov, Motebehov> lagreMotebehov2motebehov = lagreMotebehov ->
            new Motebehov()
                    .arbeidstaker(new Person()
                            .fnr(lagreMotebehov.arbeidstakerFnr())
                    )
                    .motebehovSvar(lagreMotebehov.motebehovSvar());

    public static Function<PDialogmotebehov, Motebehov> dialogmotebehov2rs = dialogmotebehov ->
            new Motebehov()
                    .id(dialogmotebehov.getUuid())
                    .opprettetDato(dialogmotebehov.getOpprettetDato())
                    .opprettetAv(dialogmotebehov.getOpprettetAv())
                    .arbeidstaker(new Person()
                            .fnr(aktoerConsumer().hentFnrForAktoerId(dialogmotebehov.getAktoerId()))
                    )
                    .motebehovSvar(new MotebehovSvar()
                            .friskmeldingForventning(dialogmotebehov.getFriskmeldingForventning())
                            .tiltak(dialogmotebehov.getTiltak())
                            .tiltakResultat(dialogmotebehov.getTiltakResultat())
                            .harMotebehov(dialogmotebehov.isHarMotebehov())
                            .forklaring(dialogmotebehov.getForklaring())
                    );
}
