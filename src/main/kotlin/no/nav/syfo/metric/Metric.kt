package no.nav.syfo.metric

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import no.nav.syfo.motebehov.MotebehovFormSubmissionCombinedDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.oppfolgingstilfelle.database.PersonOppfolgingstilfelle
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

    fun tellMotebehovBesvart(
        activeOppfolgingstilfelle: PersonOppfolgingstilfelle?,
        motebehovSkjemaType: MotebehovSkjemaType?,
        harMotebehov: Boolean,
        erInnloggetBrukerArbeidstaker: Boolean
    ) {
        val dayInOppfolgingstilfelleMotebehovCreated = if (activeOppfolgingstilfelle != null) {
            ChronoUnit.DAYS.between(activeOppfolgingstilfelle.fom, LocalDate.now())
        } else null
        val navn =
            if (erInnloggetBrukerArbeidstaker) {
                "syfomotebehov_motebehov_besvart_at"
            } else {
                "syfomotebehov_motebehov_besvart"
            }
        registry.counter(
            navn,
            Tags.of(
                "type", "info",
                "motebehov", if (harMotebehov) "ja" else "nei",
                "dag", dayInOppfolgingstilfelleMotebehovCreated?.toString().orEmpty(),
                "skjematype",
                when (motebehovSkjemaType) {
                    MotebehovSkjemaType.MELD_BEHOV -> "meldbehov"
                    MotebehovSkjemaType.SVAR_BEHOV -> "svarbehov"
                    else -> "null"
                }
            )
        ).increment()
    }

    fun countDayInOppfolgingstilfelleMotebehovCreated(
        activeOppfolgingstilfelle: PersonOppfolgingstilfelle,
        motebehovSkjemaType: MotebehovSkjemaType?,
        harMotebehov: Boolean,
        harForklaring: Boolean,
        erInnloggetBrukerArbeidstaker: Boolean
    ) {
        val dayInOppfolgingstilfelleMotebehovCreated =
            ChronoUnit.DAYS.between(activeOppfolgingstilfelle.fom, LocalDate.now())
        val navn = if (erInnloggetBrukerArbeidstaker) {
            "syfomotebehov_motebehov_besvart_oppfolgingstilfelle_dag_at"
        } else "syfomotebehov_motebehov_besvart_oppfolgingstilfelle_dag_ag"
        registry.counter(
            navn,
            Tags.of(
                "type", "info",
                "motebehov", if (harMotebehov) "ja" else "nei",
                "forklaring", if (harForklaring) "ja" else "nei",
                "dag", dayInOppfolgingstilfelleMotebehovCreated.toString(),
                "skjematype",
                when (motebehovSkjemaType) {
                    MotebehovSkjemaType.MELD_BEHOV -> "meldbehov"
                    MotebehovSkjemaType.SVAR_BEHOV -> "svarbehov"
                    else -> "null"
                }
            )
        ).increment(dayInOppfolgingstilfelleMotebehovCreated.toDouble())
    }

    fun tellMotebehovBesvartNeiAntallTegn(antallTegnIForklaring: Int, erInnloggetBrukerArbeidstaker: Boolean) {
        val navn =
            if (erInnloggetBrukerArbeidstaker) {
                "syfomotebehov_motebehov_besvart_nei_forklaring_lengde_at"
            } else {
                "syfomotebehov_motebehov_besvart_nei_forklaring_lengde"
            }
        registry.counter(
            navn,
            Tags.of("type", "info")
        ).increment(antallTegnIForklaring.toDouble())
    }

    fun tellMotebehovBesvartJaMedForklaringTegn(antallTegnIForklaring: Int, erInnloggetBrukerArbeidstaker: Boolean) {
        val navn =
            if (erInnloggetBrukerArbeidstaker) {
                "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_at"
            } else {
                "syfomotebehov_motebehov_besvart_ja_forklaring_lengde_ag"
            }
        registry.counter(
            navn,
            Tags.of("type", "info")
        ).increment(antallTegnIForklaring.toDouble())
    }

    fun tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker: Boolean) {
        val navn =
            if (erInnloggetBrukerArbeidstaker) {
                "syfomotebehov_motebehov_besvart_ja_forklaring_at"
            } else {
                "syfomotebehov_motebehov_besvart_ja_forklaring_ag"
            }
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
                "kode", kode.toString()
            )
        ).increment()
    }

    fun tellBesvarMotebehov(
        activeOppfolgingstilfelle: PersonOppfolgingstilfelle,
        motebehovSkjemaType: MotebehovSkjemaType?,
        formSubmission: MotebehovFormSubmissionCombinedDTO,
        erInnloggetBrukerArbeidstaker: Boolean
    ) {
        val harForklaring = formSubmission.forklaring?.isNotBlank() ?: false

        tellMotebehovBesvart(
            activeOppfolgingstilfelle,
            motebehovSkjemaType,
            formSubmission.harMotebehov,
            erInnloggetBrukerArbeidstaker
        )
        countDayInOppfolgingstilfelleMotebehovCreated(
            activeOppfolgingstilfelle,
            motebehovSkjemaType,
            formSubmission.harMotebehov,
            harForklaring,
            erInnloggetBrukerArbeidstaker
        )

        if (!formSubmission.harMotebehov && formSubmission.forklaring !== null) {
            tellMotebehovBesvartNeiAntallTegn(formSubmission.forklaring.length, erInnloggetBrukerArbeidstaker)
        } else if (harForklaring) {
            tellMotebehovBesvartJaMedForklaringTegn(
                formSubmission.forklaring!!.length,
                erInnloggetBrukerArbeidstaker
            )
            tellMotebehovBesvartJaMedForklaringAntall(erInnloggetBrukerArbeidstaker)
        }
    }

    private fun addPrefix(navn: String): String {
        val metricPrefix = "syfomotebehov_"
        return metricPrefix + navn
    }
}
