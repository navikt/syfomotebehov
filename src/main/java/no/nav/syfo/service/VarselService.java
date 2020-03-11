package no.nav.syfo.service;

import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class VarselService {

    private static final Logger log = getLogger(VarselService.class);

    private static final int MOTEBEHOV_VARSEL_UKER = 16;
    private static final int MOTEBEHOV_VARSEL_DAGER = MOTEBEHOV_VARSEL_UKER * 7;

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
        LocalDateTime startDatoINyesteOppfolgingstilfelle = LocalDateTime.now().minusDays(MOTEBEHOV_VARSEL_DAGER);

        if (!moterService.erMoteOpprettetForArbeidstakerEtterDato(motebehovsvarVarselInfo.sykmeldtAktorId, startDatoINyesteOppfolgingstilfelle)) {
            log.info("Sender varsel til naermeste leder");
            KTredjepartsvarsel kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo);
            tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);
        } else {
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
