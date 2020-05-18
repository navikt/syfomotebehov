package no.nav.syfo.metric

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.syfo.motebehov.MotebehovSvar
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Controller
import javax.inject.Inject

@Controller
class Metric @Inject constructor(
        private val registry: MeterRegistry
) {
    fun countOutgoingReponses(navn: String, statusCode: Int) {
        registry.counter(
                addPrefix(navn),
                Tags.of(
                        "type", "info",
                        "status", statusCode.toString()
                )
        ).increment()
    }

    fun tellHendelse(navn: String) {
        registry.counter(
                addPrefix(navn),
                Tags.of("type", "info")
        ).increment()
    }

    fun tellEndepunktKall(navn: String) {
        registry.counter(
                addPrefix(navn),
                Tags.of("type", "info")
        ).increment()
    }

    fun tellMotebehovBesvart(harMotebehov: Boolean, erInnloggetBrukerArbeidstaker: Boolean) {
        val navn = if (erInnloggetBrukerArbeidstaker) "syfomotebehov_motebehov_besvart_at" else "syfomotebehov_motebehov_besvart"
        registry.counter(
                navn,
                Tags.of(
                        "type", "info",
                        "motebehov", if (harMotebehov) "ja" else "nei"
                )
        ).increment()
    }

    fun tellMotebehovBesvartNeiAntallTegn(antallTegnIForklaring: Int, erInnloggetBrukerArbeidstaker: Boolean) {
        val navn = if (erInnloggetBrukerArbeidstaker) "syfomotebehov_motebehov_besvart_nei_forklaring_lengde_at" else "syfomotebehov_motebehov_besvart_nei_forklaring_lengde"
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring.toDouble())
    }

    fun tellMotebehovBesvartJaMedForklaringTegn(antallTegnIForklaring: Int, erInnloggetBrukerArbeidstaker: Boolean) {
        val navn = if (erInnloggetBrukerArbeidstaker) "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_at" else "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_ag"
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment(antallTegnIForklaring.toDouble())
    }

    fun tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker: Boolean) {
        val navn = if (erInnloggetBrukerArbeidstaker) "syfomotebehov_motebehov_besvart_ja_forklaring_at" else "syfomotebehov_motebehov_besvart_ja_forklaring_ag"
        registry.counter(
                navn,
                Tags.of("type", "info")
        ).increment()
    }

    fun tellHttpKall(kode: Int) {
        registry.counter(
                addPrefix("httpstatus"),
                Tags.of(
                        "type", "info",
                        "kode", kode.toString())
        ).increment()
    }

    fun tellBesvarMotebehov(motebehovSvar: MotebehovSvar, erInnloggetBrukerArbeidstaker: Boolean) {
        tellMotebehovBesvart(motebehovSvar.harMotebehov, erInnloggetBrukerArbeidstaker)
        if (!motebehovSvar.harMotebehov) {
            tellMotebehovBesvartNeiAntallTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
        } else if (!StringUtils.isEmpty(motebehovSvar.forklaring)) {
            tellMotebehovBesvartJaMedForklaringTegn(motebehovSvar.forklaring!!.length, erInnloggetBrukerArbeidstaker)
            tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }

    private fun addPrefix(navn: String): String {
        val metricPrefix = "syfomotebehov_"
        return metricPrefix + navn
    }
}
