package no.nav.syfo.testhelper;

import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.repository.domain.PMotebehov;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.repository.DbUtil.MOTEBEHOVSVAR_GYLDIGHET_DAGER;
import static no.nav.syfo.testhelper.UserConstants.*;

public class MotebehovGenerator {

    private final MotebehovSvar motebehovSvar = new MotebehovSvar()
            .harMotebehov(true)
            .forklaring("");

    private final NyttMotebehov nyttMotebehovArbeidstaker = new NyttMotebehov()
            .arbeidstakerFnr(ARBEIDSTAKER_FNR)
            .virksomhetsnummer(VIRKSOMHETSNUMMER)
            .motebehovSvar(
                    motebehovSvar
            )
            .tildeltEnhet(NAV_ENHET);

    public MotebehovSvar lagMotebehovSvar(boolean harBehov) {
        return motebehovSvar
                .harMotebehov(harBehov);
    }

    public NyttMotebehov lagNyttMotebehovFraAT() {
        return nyttMotebehovArbeidstaker;
    }

    public NyttMotebehov lagNyttMotebehovFraAT(boolean harBehov) {
        MotebehovSvar svar = motebehovSvar.harMotebehov(harBehov);
        return nyttMotebehovArbeidstaker.motebehovSvar(svar);
    }

    private final PMotebehov nyttPMotebehovArbeidstaker = new PMotebehov()
            .opprettetDato(getOpprettetDato(true))
            .opprettetAv(LEDER_AKTORID)
            .aktoerId(ARBEIDSTAKER_AKTORID)
            .virksomhetsnummer(VIRKSOMHETSNUMMER)
            .forklaring("Megling")
            .harMotebehov(true)
            .tildeltEnhet(NAV_ENHET);

    public PMotebehov lagNyttPMotebehovFraAT(boolean harBehov) {
        return nyttPMotebehovArbeidstaker.harMotebehov(harBehov);
    }

    public LocalDateTime getOpprettetDato(boolean erGyldig) {
        return erGyldig
                ? now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER)
                : now().minusDays(MOTEBEHOVSVAR_GYLDIGHET_DAGER + 1);
    }

    public PMotebehov generatePmotebehov() {
        return nyttPMotebehovArbeidstaker;
    }
}
