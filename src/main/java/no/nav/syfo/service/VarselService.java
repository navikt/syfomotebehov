package no.nav.syfo.service;

import no.nav.syfo.domain.rest.TredjepartsKontaktinfo;
import no.nav.syfo.kafka.producer.TredjepartsVarselNokkel;
import no.nav.syfo.kafka.producer.TredjepartsvarselProducer;
import no.nav.syfo.kafka.producer.model.KTredjepartsvarsel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@Service
public class VarselService {

    private TredjepartsvarselProducer tredjepartsvarselProducer;

    public VarselService(TredjepartsvarselProducer tredjepartsvarselProducer) {
        this.tredjepartsvarselProducer = tredjepartsvarselProducer;
    }

    public void sendVarselTilNaermesteLeder(TredjepartsKontaktinfo tredjepartsKontaktinfo) {
        KTredjepartsvarsel kTredjepartsvarsel = mapTilKTredjepartsvarsel(tredjepartsKontaktinfo);
        tredjepartsvarselProducer.sendTredjepartsvarselvarsel(kTredjepartsvarsel);
    }

    private KTredjepartsvarsel mapTilKTredjepartsvarsel(TredjepartsKontaktinfo tredjepartsKontaktinfo) {
        LocalDateTime utsendelsestidspunkt = now().plusMinutes(5);
        return KTredjepartsvarsel.builder()
                .type(TredjepartsVarselNokkel.NAERMESTE_LEDER_SVAR_MOTEBEHOV.name())
                .ressursId(UUID.randomUUID().toString())
                .aktorId(tredjepartsKontaktinfo.aktoerId)
                .epost(tredjepartsKontaktinfo.epost)
                .mobilnr(tredjepartsKontaktinfo.mobil)
                .orgnummer(tredjepartsKontaktinfo.orgnummer)
                .utsendelsestidspunkt(utsendelsestidspunkt)
                .build();
    }
}
