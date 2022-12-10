package no.nav.syfo.motebehov

import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject
import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.database.PMotebehov
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MotebehovService @Inject constructor(
    private val metric: Metric,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val personoppgavehendelseService: PersonoppgavehendelseService,
    private val motebehovDAO: MotebehovDAO,
    private val pdlConsumer: PdlConsumer
) {
    @Transactional
    fun behandleUbehandledeMotebehov(arbeidstakerFnr: String, veilederIdent: String) {
        val arbeidstakerAktorId = pdlConsumer.aktorid(arbeidstakerFnr)
        val pMotebehovUbehandletList = motebehovDAO.hentUbehandledeMotebehov(arbeidstakerAktorId)
        if (pMotebehovUbehandletList.isNotEmpty()) {
            pMotebehovUbehandletList.forEach { pMotebehov ->
                val antallOppdatering = motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(pMotebehov.uuid, veilederIdent)
                if (antallOppdatering == 1) {
                    personoppgavehendelseService.sendPersonoppgaveHendelseBehandlet(pMotebehov.uuid, arbeidstakerFnr)
                }
            }
        } else {
            metric.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke")
            log.warn("Ugyldig tilstand: Veileder {} forsøkte å behandle motebehovsvar som ikke eksisterer. Kaster Http-409", veilederIdent)
            throw ConflictException()
        }
    }

    fun hentMotebehovListe(arbeidstakerFnr: String): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
            .stream()
            .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
            .collect(Collectors.toList())
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr: String): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
            .stream()
            .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
            .collect(Collectors.toList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerFnr: String, isOwnLeader: Boolean, virksomhetsnummer: String): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(arbeidstakerAktoerId, isOwnLeader, virksomhetsnummer)
            .stream()
            .map { dbMotebehov: PMotebehov -> mapPMotebehovToMotebehov(arbeidstakerFnr, dbMotebehov) }
            .collect(Collectors.toList())
    }

    @Transactional
    fun lagreMotebehov(
        innloggetFNR: String,
        arbeidstakerFnr: String,
        virksomhetsnummer: String,
        skjemaType: MotebehovSkjemaType,
        motebehovSvar: MotebehovSvar
    ): UUID {
        val innloggetBrukerAktoerId = pdlConsumer.aktorid(innloggetFNR)
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        val arbeidstakerBehandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr, null).enhetId
        val motebehov = mapNyttMotebehovToPMotebehov(
            innloggetBrukerAktoerId,
            arbeidstakerAktoerId,
            innloggetFNR,
            arbeidstakerFnr,
            arbeidstakerBehandlendeEnhet,
            virksomhetsnummer,
            skjemaType,
            motebehovSvar
        )
        val uuid = motebehovDAO.create(motebehov)
        if (motebehovSvar.harMotebehov) {
            personoppgavehendelseService.sendPersonoppgaveHendelseMottatt(uuid, arbeidstakerFnr)
        }
        return uuid
    }

    private fun mapNyttMotebehovToPMotebehov(
        innloggetAktoerId: String,
        arbeidstakerAktoerId: String,
        innloggetFnr: String,
        arbeidstakerFnr: String,
        tildeltEnhet: String,
        virksomhetsnummer: String,
        skjemaType: MotebehovSkjemaType,
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
            behandletTidspunkt = null,
            skjemaType = skjemaType,
            sykmeldtFnr = arbeidstakerFnr,
            opprettetAvFnr = innloggetFnr
        )
    }

    private fun mapPMotebehovToMotebehov(arbeidstakerFnr: String, pMotebehov: PMotebehov): Motebehov {
        return Motebehov(
            id = pMotebehov.uuid,
            opprettetDato = pMotebehov.opprettetDato,
            aktorId = pMotebehov.aktoerId,
            opprettetAv = pMotebehov.opprettetAv,
            arbeidstakerFnr = arbeidstakerFnr,
            virksomhetsnummer = pMotebehov.virksomhetsnummer,
            motebehovSvar = MotebehovSvar(
                harMotebehov = pMotebehov.harMotebehov,
                forklaring = pMotebehov.forklaring
            ),
            tildeltEnhet = pMotebehov.tildeltEnhet,
            behandletTidspunkt = pMotebehov.behandletTidspunkt,
            behandletVeilederIdent = pMotebehov.behandletVeilederIdent,
            skjemaType = pMotebehov.skjemaType
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(MotebehovService::class.java)
    }
}
