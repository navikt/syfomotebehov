package no.nav.syfo.mappers;

import no.nav.syfo.domain.rest.Motebehov;
import no.nav.syfo.repository.domain.PDialogmotebehov;

import java.util.function.Function;

import static no.nav.syfo.consumer.ws.AktoerConsumer.aktoerConsumer;

public class PersistencyMappers {

    public static Function<Motebehov, PDialogmotebehov> rsMotebehov2p = motebehov ->
            PDialogmotebehov.builder()
                    .opprettetAv(motebehov.opprettetAv())
                    .aktoerId(aktoerConsumer().hentAktoerIdForFnr(motebehov.arbeidstaker().fnr()))
                    .harMotebehov(motebehov.motebehovSvar().harMotebehov())
                    .friskmeldingForventning(motebehov.motebehovSvar().friskmeldingForventning())
                    .tiltak(motebehov.motebehovSvar().tiltak())
                    .tiltakResultat(motebehov.motebehovSvar().tiltakResultat())
                    .forklaring(motebehov.motebehovSvar().forklaring())
                    .build();
}
