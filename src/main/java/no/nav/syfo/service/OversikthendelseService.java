package no.nav.syfo.service;

import no.nav.syfo.domain.rest.NyttMotebehov;
import no.nav.syfo.kafka.producer.OversikthendelseProducer;
import no.nav.syfo.kafka.producer.model.KOversikthendelse;
import org.springframework.stereotype.Service;

import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static no.nav.syfo.kafka.producer.OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET;
import static no.nav.syfo.kafka.producer.OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT;
import static no.nav.syfo.util.MapUtil.map;

@Service
public class OversikthendelseService {

    private OversikthendelseProducer oversikthendelseProducer;

    public OversikthendelseService(OversikthendelseProducer oversikthendelseProducer) {
        this.oversikthendelseProducer = oversikthendelseProducer;
    }

    public void sendOversikthendelse(NyttMotebehov nyttMotebehov) {
        KOversikthendelse kOversikthendelse = map(nyttMotebehov, nyttMotebehov2KOversikthendelse);
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse);
    }

    public void sendOversikthendelse(String fnr, String tildeltEnhet) {
        KOversikthendelse kOversikthendelse = KOversikthendelse.builder()
                .fnr(fnr)
                .hendelseId(MOTEBEHOV_SVAR_BEHANDLET.name())
                .enhetId(tildeltEnhet)
                .tidspunkt(now()
                ).build();
        oversikthendelseProducer.sendOversikthendelse(kOversikthendelse);
    }

    public static Function<NyttMotebehov, KOversikthendelse> nyttMotebehov2KOversikthendelse = nyttMotebehov -> KOversikthendelse.builder()
            .fnr(nyttMotebehov.arbeidstakerFnr)
            .hendelseId(MOTEBEHOV_SVAR_MOTTATT.name())
            .enhetId(nyttMotebehov.tildeltEnhet)
            .tidspunkt(now())
            .build();
}
