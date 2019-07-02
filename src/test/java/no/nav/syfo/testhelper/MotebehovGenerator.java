package no.nav.syfo.testhelper;

import no.nav.syfo.domain.rest.MotebehovSvar;
import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.repository.domain.PMotebehov;

import static no.nav.syfo.testhelper.UserConstants.*;

public class MotebehovGenerator {

    private final MotebehovSvar motebehovSvar = new MotebehovSvar()
            .harMotebehov(true)
            .friskmeldingForventning("Om en uke")
            .tiltak("Krykker")
            .tiltakResultat("Kommer seg fremover")
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
            .opprettetAv(ARBEIDSTAKER_AKTORID)
            .aktoerId(ARBEIDSTAKER_AKTORID)
            .virksomhetsnummer(VIRKSOMHETSNUMMER)
            .harMotebehov(true);

    public PMotebehov lagNyttPMotebehovFraAT(boolean harBehov) {
        return nyttPMotebehovArbeidstaker.harMotebehov(harBehov);
    }
}
