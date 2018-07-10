package no.nav.syfo.mappers;

import no.nav.syfo.domain.Dialogmotebehov;
import no.nav.syfo.repository.domain.PDialogmotebehov;

import java.util.function.Function;

public class PDialogmotebehovMapper {

    public static Function<PDialogmotebehov, Dialogmotebehov> p2dialogmotebehov = pDialogmotebehov ->
            Dialogmotebehov.builder()
                    .uuid(pDialogmotebehov.getUuid())
                    .opprettetDato(pDialogmotebehov.getOpprettetDato())
                    .opprettetAv(pDialogmotebehov.getOpprettetAv())
                    .aktoerId(pDialogmotebehov.getAktoerId())
                    .friskmeldingForventning(pDialogmotebehov.getFriskmeldingForventning())
                    .tiltak(pDialogmotebehov.getTiltak())
                    .tiltakResultat(pDialogmotebehov.getTiltakResultat())
                    .harMotebehov(pDialogmotebehov.isHarMotebehov())
                    .forklaring(pDialogmotebehov.getForklaring())
                    .build();
}
