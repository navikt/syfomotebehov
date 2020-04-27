package no.nav.syfo.motebehov

import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetConsumer
import no.nav.syfo.domain.rest.Motebehov
import no.nav.syfo.domain.rest.MotebehovSvar
import no.nav.syfo.domain.rest.NyttMotebehov
import no.nav.syfo.exception.ConflictException
import no.nav.syfo.metric.Metrikk
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
        private val metrikk: Metrikk,
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
            oversikthendelseService.sendOversikthendelse(arbeidstakerFnr.value, behandlendeEnhet)
        } else {
            metrikk.tellHendelse("feil_behandle_motebehov_svar_eksiterer_ikke")
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
    fun lagreMotebehov(innloggetFNR: Fodselsnummer, arbeidstakerFnr: Fodselsnummer, nyttMotebehov: NyttMotebehov): UUID {
        val innloggetBrukerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(innloggetFNR)
        val arbeidstakerAktoerId = aktorregisterConsumer.getAktorIdForFodselsnummer(arbeidstakerFnr)
        val arbeidstakerBehandlendeEnhet = behandlendeEnhetConsumer.getBehandlendeEnhet(arbeidstakerFnr.value, null).enhetId
        val motebehov = mapNyttMotebehovToPMotebehov(innloggetBrukerAktoerId, arbeidstakerAktoerId, arbeidstakerBehandlendeEnhet, nyttMotebehov)
        val id = motebehovDAO.create(motebehov)
        if (nyttMotebehov.motebehovSvar.harMotebehov) {
            nyttMotebehov.arbeidstakerFnr = arbeidstakerFnr.value
            nyttMotebehov.tildeltEnhet = arbeidstakerBehandlendeEnhet
            oversikthendelseService.sendOversikthendelse(nyttMotebehov)
        }
        return id
    }

    private fun mapNyttMotebehovToPMotebehov(innloggetAktoerId: String, arbeidstakerAktoerId: String, tildeltEnhet: String, nyttMotebehov: NyttMotebehov): PMotebehov {
        return PMotebehov(
                uuid = UUID.randomUUID(),
                opprettetDato = LocalDateTime.now(),
                opprettetAv = innloggetAktoerId,
                aktoerId = arbeidstakerAktoerId,
                virksomhetsnummer = nyttMotebehov.virksomhetsnummer,
                harMotebehov = nyttMotebehov.motebehovSvar.harMotebehov,
                forklaring = nyttMotebehov.motebehovSvar.forklaring,
                tildeltEnhet = tildeltEnhet,
                behandletVeilederIdent = null,
                behandletTidspunkt = null
        )
    }

    private fun mapPMotebehovToMotebehov(arbeidstakerFnr: Fodselsnummer, pMotebehov: PMotebehov): Motebehov {
        val motebehovSvar = MotebehovSvar()
        motebehovSvar.harMotebehov = pMotebehov.harMotebehov
        motebehovSvar.forklaring = pMotebehov.forklaring
        val motebehov = Motebehov()
        motebehov.id = pMotebehov.uuid
        motebehov.opprettetDato = pMotebehov.opprettetDato
        motebehov.aktorId = pMotebehov.aktoerId
        motebehov.opprettetAv = pMotebehov.opprettetAv
        motebehov.arbeidstakerFnr = arbeidstakerFnr.value
        motebehov.virksomhetsnummer = pMotebehov.virksomhetsnummer
        motebehov.motebehovSvar = motebehovSvar
        motebehov.tildeltEnhet = pMotebehov.tildeltEnhet
        motebehov.behandletTidspunkt = pMotebehov.behandletTidspunkt
        motebehov.behandletVeilederIdent = pMotebehov.behandletVeilederIdent
        return motebehov;
    }

    companion object {
        private val log = LoggerFactory.getLogger(MotebehovService::class.java)
    }
}
