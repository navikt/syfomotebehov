package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.oidc.OIDCIssuer;
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
        List<Oppfolgingstilfelle> oppfolgingstilfelleperioder = sykefravaeroppfoelgingConsumer.hentOppfolgingstilfelleperioder(aktorId, orgnummer, OIDCIssuer.INTERN);

        Optional<Oppfolgingstilfelle> forstePeriode = forstePeriodeITilfelle(oppfolgingstilfelleperioder);

        if (!forstePeriode.isPresent()) {
            log.error("Fant ikke oppfolgingstilfelle hos syfoservice, dette skal ikke skje, da syfoservice har sagt at motebehov-varsel skal sendes!");
            throw new NullPointerException("Fikk ikke oppfolgingstilfelle fra syfoservice, dette skal ikke skje!");
        }

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
