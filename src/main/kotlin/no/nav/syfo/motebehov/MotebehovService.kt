package no.nav.syfo.motebehov

import no.nav.syfo.api.exception.ConflictException
import no.nav.syfo.consumer.behandlendeenhet.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.behandlendeenhet.getEnhetId
import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.metric.Metric
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.motebehov.database.toMotebehov
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.personoppgavehendelse.PersonoppgavehendelseService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@Service
class MotebehovService @Inject constructor(
    private val metric: Metric,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val personoppgavehendelseService: PersonoppgavehendelseService,
    private val motebehovDAO: MotebehovDAO,
    private val pdlConsumer: PdlConsumer,
) {
    @Transactional
    fun behandleUbehandledeMotebehov(arbeidstakerFnr: String, veilederIdent: String) {
        val arbeidstakerAktorId = pdlConsumer.aktorid(arbeidstakerFnr)
        val pMotebehovUbehandletList = motebehovDAO.hentUbehandledeMotebehov(arbeidstakerAktorId)
        if (pMotebehovUbehandletList.isNotEmpty()) {
            pMotebehovUbehandletList.forEach { pMotebehov ->
                val antallOppdatering =
                    motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(pMotebehov.uuid, veilederIdent)
                if (antallOppdatering == 1) {
                    personoppgavehendelseService.sendPersonoppgaveHendelseBehandlet(pMotebehov.uuid, arbeidstakerFnr)
                }
            }
        } else {
            metric.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke")
            log.warn(
                "Ugyldig tilstand: Veileder {} forsøkte å behandle motebehovsvar som ikke eksisterer. Kaster Http-409",
                veilederIdent,
            )
            throw ConflictException()
        }
    }

    @Transactional
    fun behandleUbehandledeMotebehovOpprettetTidligereEnnDato(dato: LocalDate, veilederIdent: String): Int {
        var updatedCount = 0
        motebehovDAO.hentUbehandledeMotebehovEldreEnnDato(dato)
            .forEach { pMotebehov ->
                pMotebehov.sykmeldtFnr?.let {
                    val antallOppdatering =
                        motebehovDAO.oppdaterUbehandledeMotebehovTilBehandlet(pMotebehov.uuid, veilederIdent)
                    if (antallOppdatering == 1) {
                        personoppgavehendelseService.sendPersonoppgaveHendelseBehandlet(pMotebehov.uuid, it)
                        updatedCount++
                    }
                }
            }
        return updatedCount
    }

    fun hentMotebehovListe(arbeidstakerFnr: String): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForAktoer(arbeidstakerAktoerId)
            .stream()
            .map { it.toMotebehov(arbeidstakerFnr) }
            .collect(Collectors.toList())
    }

    fun hentMotebehov(motebehovId: String): Motebehov? {
        return motebehovDAO
            .hentMotebehov(motebehovId)
            .stream()
            .map { it.toMotebehov() }
            .collect(Collectors.toList())
            .firstOrNull()
    }

    fun hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerFnr: String): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForOgOpprettetAvArbeidstaker(arbeidstakerAktoerId)
            .stream()
            .map { it.toMotebehov(arbeidstakerFnr) }
            .collect(Collectors.toList())
    }

    fun hentMotebehovListeForArbeidstakerOpprettetAvLeder(
        arbeidstakerFnr: String,
        isOwnLeader: Boolean,
        virksomhetsnummer: String,
    ): List<Motebehov> {
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        return motebehovDAO.hentMotebehovListeForArbeidstakerOpprettetAvLeder(
            arbeidstakerAktoerId,
            isOwnLeader,
            virksomhetsnummer,
        )
            .stream()
            .map { it.toMotebehov(arbeidstakerFnr) }
            .collect(Collectors.toList())
    }

    @Transactional
    fun lagreMotebehov(
        innloggetFNR: String,
        arbeidstakerFnr: String,
        virksomhetsnummer: String,
        skjemaType: MotebehovSkjemaType,
        innmelderType: MotebehovInnmelderType,
        motebehovFormSubmission: MotebehovFormSubmissionDTO,
    ): UUID {
        val innloggetBrukerAktoerId = pdlConsumer.aktorid(innloggetFNR)
        val arbeidstakerAktoerId = pdlConsumer.aktorid(arbeidstakerFnr)
        val arbeidstakerBehandlendeEnhet =
            behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr, null).getEnhetId()

        val motebehov = Motebehov(
            id = UUID.randomUUID(),
            opprettetDato = LocalDateTime.now(),
            aktorId = arbeidstakerAktoerId,
            opprettetAv = innloggetBrukerAktoerId,
            opprettetAvFnr = innloggetFNR,
            arbeidstakerFnr = arbeidstakerFnr,
            virksomhetsnummer = virksomhetsnummer,
            formSubmission = motebehovFormSubmission,
            tildeltEnhet = arbeidstakerBehandlendeEnhet,
            skjemaType = skjemaType,
            innmelderType = innmelderType,
        )

        val pMotebehov = motebehov.toPMotebehov()

        val uuid: UUID

        try {
            uuid = motebehovDAO.create(pMotebehov)
        } catch (ex: DataAccessException) {
            metric.tellHendelse("feil_lagre_motebehov")

            log.error(
                "DataAccessException ved lagring av motebehov med skjemaType {} og innmelderType {}: {}",
                skjemaType.name,
                innmelderType.name,
                ex.message,
            )

            throw ex
        } catch (ex: IllegalStateException) {
            metric.tellHendelse("feil_lagre_motebehov")

            log.error(
                "IllegalStateException ved lagring av motebehov med skjemaType {} og innmelderType {}: {}",
                skjemaType.name,
                innmelderType.name,
                ex.message,
            )

            throw ex
        }

        if (motebehovFormSubmission.harMotebehov) {
            personoppgavehendelseService.sendPersonoppgaveHendelseMottatt(uuid, arbeidstakerFnr)
        }
        return uuid
    }

    companion object {
        private val log = LoggerFactory.getLogger(MotebehovService::class.java)
    }
}
