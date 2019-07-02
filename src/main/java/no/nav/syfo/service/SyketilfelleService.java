package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.OIDCIssuer;
import no.nav.syfo.consumer.ws.SykefravaeroppfoelgingConsumer;
import no.nav.syfo.domain.rest.Oppfolgingstilfelle;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.*;
import java.util.*;

@Slf4j
@Service
public class SyketilfelleService {

    private final SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer;

    @Inject
    public SyketilfelleService(
            SykefravaeroppfoelgingConsumer sykefravaeroppfoelgingConsumer
    ) {
        this.sykefravaeroppfoelgingConsumer = sykefravaeroppfoelgingConsumer;
    }

    public LocalDateTime hentStartDatoINyesteOppfolgingstilfelle(String aktorId, String orgnummer) {
        log.info("L-TRACE: Skal hente oppfolgingstilfelleperioder fra syfoservice");
        List<Oppfolgingstilfelle> oppfolgingstilfelleperioder = sykefravaeroppfoelgingConsumer.hentOppfolgingstilfelleperioder(aktorId, orgnummer, OIDCIssuer.INTERN);
        log.info("L-TRACE: Fikk oppfolgingstilfelleperioder fra syfoservice: {}", oppfolgingstilfelleperioder);

        Optional<Oppfolgingstilfelle> forstePeriode = forstePeriodeITilfelle(oppfolgingstilfelleperioder);

        if (!forstePeriode.isPresent()) {
            log.info("L-TRACE: Fant ikke forste periode!!!");
            log.error("Fant ikke oppfolgingstilfelle hos syfoservice, dette skal ikke skje, da syfoservice har sagt at motebehov-varsel skal sendes!");
            throw new NullPointerException("Fikk ikke oppfolgingstilfelle fra syfoservice, dette skal ikke skje!");
        }
        log.info("L-TRACE: Forste periode er {}", forstePeriode.get());

        return localDate2LocalDateTime(forstePeriode.get().fom);
    }

    private Optional<Oppfolgingstilfelle> forstePeriodeITilfelle(List<Oppfolgingstilfelle> oppfolgingstilfelleperioder) {
        return Optional.of(oppfolgingstilfelleperioder
                .stream()
                .min(Comparator.comparing(o -> o.fom)))
                .orElse(Optional.empty());
    }

    private LocalDateTime localDate2LocalDateTime(LocalDate forstePeriodeFom) {
        return LocalDateTime.of(forstePeriodeFom, LocalTime.MIN);
    }
}
