package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;

@Service
@Slf4j
public class VarselService {

    private MoterService moterService;
    private TredjepartsvarselProducer tredjepartsvarselProducer;

    @Inject
    public VarselService(
            MoterService moterService,
            TredjepartsvarselProducer tredjepartsvarselProducer) {
        this.moterService = moterService;
        this.tredjepartsvarselProducer = tredjepartsvarselProducer;
    }

    public void sendVarselTilNaermesteLeder(MotebehovsvarVarselInfo motebehovsvarVarselInfo) {
        if (!moterService.harArbeidstakerMoteIOppfolgingstilfelle(motebehovsvarVarselInfo.sykmeldtAktorId)) {
            log.info("Sender varsel til naermeste leder");
            KTredjepartsvarsel kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo);
            tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);
        }
        log.info("Sender ikke varsel til naermeste leder fordi moteplanleggeren er brukt i oppfolgingstilfellet");
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
