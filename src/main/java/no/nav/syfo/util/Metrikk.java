package no.nav.syfo.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

@Controller
public class Metrikk {

    private final MeterRegistry registry;

    public enum BRUKER {
        ARBEIDSTAKER,
        ARBEIDSGIVER,
    }

    @Inject
    public Metrikk(MeterRegistry registry) {
        this.registry = registry;
    }

    public void tellMotebehovBesvart(boolean harMotebehov, BRUKER bruker) {
        registry.counter(
                "syfomotebehov_motebehov_besvart",
                Tags.of(
                        "type", "info",
                        "motebehov", harMotebehov ? "ja" : "nei",
                        "bruker", bruker.name().toLowerCase()
                )
        ).increment();
    }

    public void tellMotebehovBesvartNeiAntallTegn(int antallTegnIForklaring, BRUKER bruker) {
        registry.counter(
                "syfomotebehov_motebehov_besvart_nei_forklaring_lengde",
                Tags.of(
                        "type", "info",
                        "bruker", bruker.name().toLowerCase()
                )
        ).increment(antallTegnIForklaring);
    }
}
