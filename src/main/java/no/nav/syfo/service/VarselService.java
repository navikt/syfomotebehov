package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.domain.rest.OppfolgingstilfelleDTO;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.util.Optional.ofNullable;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;

@Service
@Slf4j
public class VarselService {

    private MoterService moterService;
    private SyketilfelleService syketilfelleService;
    private TredjepartsvarselProducer tredjepartsvarselProducer;

    @Inject
    public VarselService(
            MoterService moterService,
            SyketilfelleService syketilfelleService,
            TredjepartsvarselProducer tredjepartsvarselProducer) {
        this.moterService = moterService;
        this.syketilfelleService = syketilfelleService;
        this.tredjepartsvarselProducer = tredjepartsvarselProducer;
    }

    public void sendVarselTilNaermesteLeder(MotebehovsvarVarselInfo motebehovsvarVarselInfo) {
        OppfolgingstilfelleDTO oppfolgingstilfelle = syketilfelleService.hentNyesteOppfolgingstilfelle(motebehovsvarVarselInfo.sykmeldtAktorId);
        log.info("L-TRACE: Fikk oppfolgingstilfelle: {}", oppfolgingstilfelle);

        if (!ofNullable(oppfolgingstilfelle).isPresent() || !ofNullable(oppfolgingstilfelle.arbeidsgiverperiode).isPresent()) {
            log.info("L-TRACE: Fikk ikke oppfolgingstilfelle!");
            log.error("Fant ikke oppfolgingstilfelle hos syfosyketilfelle, dette skal ikke skje, da syfoservice har sagt at motebehov-varsel skal sendes!");
            throw new NullPointerException("Fikk ikke oppfolgingstilfelle fra syfosyketilfelle, dette skal ikke skje!");
        }

        if (!moterService.erMoteOpprettetForArbeidstakerEtterDato(motebehovsvarVarselInfo.sykmeldtAktorId, oppfolgingstilfelle)) {
            log.info("L-TRACE: Mote er ikke opprettet etter dato, sender varsel!");
            log.info("Sender varsel til naermeste leder");
            KTredjepartsvarsel kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo);
            tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);
        } else {
            log.info("L-TRACE: Mote er opprettet etter dato, sender ikke varsel!");
            log.info("Sender ikke varsel til naermeste leder fordi moteplanleggeren er brukt i oppfolgingstilfellet");
        }
    }

    private KTredjepartsvarsel mapTilKTredjepartsvarsel(MotebehovsvarVarselInfo motebehovsvarVarselInfo) {
        return KTredjepartsvarsel.builder()
                .type(NAERMESTE_LEDER_SVAR_MOTEBEHOV.name())
                .ressursId(UUID.randomUUID().toString())
                .aktorId(motebehovsvarVarselInfo.sykmeldtAktorId)
                .orgnummer(motebehovsvarVarselInfo.orgnummer)
                .utsendelsestidspunkt(now())
                .build();
    }
}
