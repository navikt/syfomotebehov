package no.nav.syfo.motebehov

import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.consumer.aktorregister.AktorregisterConsumer
import no.nav.syfo.consumer.aktorregister.domain.Fodselsnummer
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhetConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.oversikthendelse.OversikthendelseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class MotebehovService @Inject constructor(
        private val metric: Metric,
        private val aktorregisterConsumer: AktorregisterConsumer,
        private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
        private val oversikthendelseService: OversikthendelseService,
        private val motebehovDAO: MotebehovDAO
) {
    @Transactional
    fun behandleUbehandledeMotebehov(arbeidstakerFnr: Fodselsnummer, veilederIdent: String) {
        val antallOppdateringer = motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr), veilederIdent)
        if (antallOppdateringer > 0) {
            val behandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.value, null).enhetId
            oversikthendelseService.sendOversikthendelseBehandlet(arbeidstakerFnr, behandlendeEnhet)
        } else {
            metric.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke")
            log.warn("Ugyldig tilstand: Veileder {} forsøkte å behandle motebehovsvar som ikke eksisterer. Kaster Http-409", veilederIdent)
            throw ConflictException()
        }
    }

    fun hentMotebehovListe(arbeidstakerFnr: Fodselsnummer): List<Motebehov> {
        val arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
                .stream()
                .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
                .collect(Collectors.toList())
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr: Fodselsnummer): List<Motebehov> {
        val arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(arbeidstakerFnr.value))
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
                .stream()
                .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
                .collect(Collectors.toList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr: Fodselsnummer, virksomhetsnummer: String): List<Motebehov> {
        val arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(arbeidstakerFnr.value))
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktoerId, virksomhetsnummer)
                .stream()
                .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
                .collect(Collectors.toList())
    }

    @Transactional
    fun lagreMotebehov(innloggetFNR: Fodselsnummer, arbeidstakerFnr: Fodselsnummer, virksomhetsnummer: String, motebehovSvar: MotebehovSvar): UUID {
        val innloggetBrukerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(innloggetFNR)
        val arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr)
        val arbeidstakerBehandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.value, null).enhetId
        val motebehov = mapNyttMotebehovToPMotebehov(
                innloggetBrukerAktoerId,
                arbeidstakerAktoerId,
                arbeidstakerBehandlendeEnhet,
                virksomhetsnummer,
                motebehovSvar
        )
        val id = motebehovDAO.create(motebehov)
        if (motebehovSvar.harMotebehov) {
            oversikthendelseService.sendOversikthendelseMottatt(arbeidstakerFnr, arbeidstakerBehandlendeEnhet)
        }
        return id
    }

    private fun mapNyttMotebehovToPMotebehov(
            innloggetAktoerId: String,
            arbeidstakerAktoerId: String,
            tildeltEnhet: String,
            virksomhetsnummer: String,
            motebehovSvar: MotebehovSvar
    ): PMotebehov {
        return PMotebehov(
                uuid = UUID.randomUUID(),
                opprettetDato = LocalDateTime.now(),
                opprettetAv = innloggetAktoerId,
                aktoerId = arbeidstakerAktoerId,
                virksomhetsnummer = virksomhetsnummer,
                harMotebehov = motebehovSvar.harMotebehov,
                forklaring = motebehovSvar.forklaring,
                tildeltEnhet = tildeltEnhet,
                behandletVeilederIdent = null,
                behandletTidspunkt = null
        )
    }

    private fun mapPMotebehovToMotebehov(arbeidstakerFnr: Fodselsnummer, pMotebehov: PMotebehov): Motebehov {
        return Motebehov(
                id = pMotebehov.uuid,
                opprettetDato = pMotebehov.opprettetDato,
                aktorId = pMotebehov.aktoerId,
                opprettetAv = pMotebehov.opprettetAv,
                arbeidstakerFnr = arbeidstakerFnr.value,
                virksomhetsnummer = pMotebehov.virksomhetsnummer,
                motebehovSvar = MotebehovSvar(
                        harMotebehov = pMotebehov.harMotebehov,
                        forklaring = pMotebehov.forklaring
                ),
                tildeltEnhet = pMotebehov.tildeltEnhet,
                behandletTidspunkt = pMotebehov.behandletTidspunkt,
                behandletVeilederIdent = pMotebehov.behandletVeilederIdent
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(MotebehovService::class.java)
    }
}
