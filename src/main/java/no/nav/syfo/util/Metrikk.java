package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private final MeterRegistry registry;

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellMotebehovBesvart(boolean harMotebehov, boolean erInnloggetBrukerArbeidstaker) {
        String navn = erInnloggetBrukerArbeidstaker
                ? "syfomotebehov_motebehov_besvart_at"
                : "syfomotebehov_motebehov_besvart";
        registry.counter(
                navn,
                Tags.of(
                        "type", "info",
                        "motebehov", harMotebehov ? "ja" : "nei"
                )
        ).increment();
    }

    public void tellMotebehovBesvartNeiAntallTegn(int antallTegnIForklaring, boolean erInnloggetBrukerArbeidstaker) {
        String navn = erInnloggetBrukerArbeidstaker
                ? "syfomotebehov_motebehov_nei_lengde_at"
                : "syfomotebehov_motebehov_besvart_nei_forklaring_lengde";
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring);
    }
}
