package no.nav.syfo.varsel

import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel
import no.nav.syfo.service.MoterService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@Service
class VarselService @Inject constructor(
        private val moterService: MoterService,
        private val tredjepartsvarselProducer: TredjepartsvarselProducer
) {
    fun sendVarselTilNaermesteLeder(motebehovsvarVarselInfo: MotebehovsvarVarselInfo) {
        val startDatoINyesteOppfolgingstilfelle = LocalDateTime.now().minusDays(MOTEBEHOV_VARSEL_DAGER.toLong())
        if (!moterService.erMoteOpprettetForArbeidstakerEtterDato(motebehovsvarVarselInfo.sykmeldtAktorId, startDatoINyesteOppfolgingstilfelle)) {
            log.info("Sender varsel til naermeste leder")
            val kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo)
            tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel)
        } else {
            log.info("Sender ikke varsel til naermeste leder fordi moteplanleggeren er brukt i oppfolgingstilfellet")
        }
    }

    private fun mapTilKTredjepartsvarsel(motebehovsvarVarselInfo: MotebehovsvarVarselInfo): KTredjepartsvarsel {
        val kTredjepartsvarsel = KTredjepartsvarsel()
        kTredjepartsvarsel.type = VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name
        kTredjepartsvarsel.ressursId = UUID.randomUUID().toString()
        kTredjepartsvarsel.aktorId = motebehovsvarVarselInfo.sykmeldtAktorId
        kTredjepartsvarsel.orgnummer = motebehovsvarVarselInfo.orgnummer
        kTredjepartsvarsel.utsendelsestidspunkt = LocalDateTime.now()
        return kTredjepartsvarsel
    }

    companion object {
        private val log = LoggerFactory.getLogger(VarselService::class.java)
        private const val MOTEBEHOV_VARSEL_UKER = 16
        private const val MOTEBEHOV_VARSEL_DAGER = MOTEBEHOV_VARSEL_UKER * 7
    }
}
