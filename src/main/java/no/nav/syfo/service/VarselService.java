package no.nav.syfo.service;

import no.nav.syfo.domain.rest.MotebehovsvarVarselInfo;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.VarselType.NAERMESTE_LEDER_SVAR_MOTEBEHOV;

@Service
public class VarselService {

    private TredjepartsvarselProducer tredjepartsvarselProducer;

    public VarselService(TredjepartsvarselProducer tredjepartsvarselProducer) {
        this.tredjepartsvarselProducer = tredjepartsvarselProducer;
    }

    public void sendVarselTilNaermesteLeder(MotebehovsvarVarselInfo motebehovsvarVarselInfo) {
        KTredjepartsvarsel kTredjepartsvarsel = mapTilKTredjepartsvarsel(motebehovsvarVarselInfo);
        tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);
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
