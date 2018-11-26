package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.syfo.domain.rest.MotebehovSvar;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private final MeterRegistry registry;

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellMotebehovSvar(MotebehovSvar motebehovSvar, boolean erInnloggetBrukerAT) {
        if (erInnloggetBrukerAT) {
            tellMotebehovBesvart(motebehovSvar.harMotebehov, "syfomotebehov_motebehov_besvart_at");
        } else {
            tellMotebehovBesvart(motebehovSvar.harMotebehov, "syfomotebehov_motebehov_besvart_ag");
        }

        if (!motebehovSvar.harMotebehov) {
            if (erInnloggetBrukerAT) {
                tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring.length(), "syfomotebehov_motebehov_besvart_nei_forklaring_lengde_at");
            } else {
                tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring.length(), "syfomotebehov_motebehov_besvart_nei_forklaring_lengde_ag");
            }
        }
    }

    private void tellMotebehovBesvart(boolean harMotebehov, String navn) {
        registry.counter(
                navn,
                Tags.of(
                        "type", "info",
                        "motebehov", harMotebehov ? "ja" : "nei"
                )
        ).increment();
    }

    private void tellMotebehovBesvartNeiAntallTegn(int antallTegnIForklaring, String navn) {
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring);
    }
}
