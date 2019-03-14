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
                ? "syfomotebehov_motebehov_besvart_nei_forklaring_lengde_at"
                : "syfomotebehov_motebehov_besvart_nei_forklaring_lengde";
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring);
    }

    public void tellMotebehovBesvartJaMedForklaringTegn(int antallTegnIForklaring, boolean erInnloggetBrukerArbeidstaker) {
        String navn = erInnloggetBrukerArbeidstaker
                ? "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_at"
                : "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_ag";
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring);
    }

    public void tellMotebehovBesvartJaMedForklaringAntall(boolean erInnloggetBrukerArbeidstaker) {
        String navn = erInnloggetBrukerArbeidstaker
                ? "syfomotebehov_motebehov_besvart_ja_forklaring_at"
                : "syfomotebehov_motebehov_besvart_ja_forklaring_ag";
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment();
    }

    public void tellHttpKall(int kode) {
        registry.counter(
                addPrefix("httpstatus"),
                Tags.of(
                        "type", "info",
                        "kode", String.valueOf(kode)
                )
        ).increment();
    }

    private String addPrefix(String navn) {
        String METRIKK_PREFIX = "syfomotebehov_";
        return METRIKK_PREFIX + navn;
    }
}
