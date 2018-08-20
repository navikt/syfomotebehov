package no.nav.syfo.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.spring.oidc.validation.api.Unprotected;
import no.nav.syfo.consumer.ws.AktoerConsumer;
import no.nav.syfo.domain.rest.VeilederOppgaveFeedItem;
import no.nav.syfo.repository.dao.MotebehovDAO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.syfo.util.RestUtils.baseUrl;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/feed/motebehov")
public class MotebehovOppgaveFeedController {

    private AktoerConsumer aktoerConsumer;
    private MotebehovDAO motebehovDAO;

    public MotebehovOppgaveFeedController(final AktoerConsumer aktoerConsumer,
                                          final MotebehovDAO motebehovDAO) {
        this.aktoerConsumer = aktoerConsumer;
        this.motebehovDAO = motebehovDAO;
    }

    @Unprotected
    @ResponseBody
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<VeilederOppgaveFeedItem> hentMotebehovListe(@RequestParam("timestamp") String timestamp) {
        return motebehovDAO.finnMotebehovOpprettetSiden(LocalDateTime.parse(timestamp))
                .stream()
                .map(motebehov -> {
                    String fnr = aktoerConsumer.hentFnrForAktoerId(motebehov.getAktoerId());
                    return new VeilederOppgaveFeedItem()
                            .uuid(motebehov.getUuid())
                            .fnr(fnr)
                            .lenke(baseUrl() + "/sykefravaer/" + fnr + "/motebehov/")
                            .type(VeilederOppgaveFeedItem.FeedHendelseType.MOTEBEHOV_MOTTATT.toString())
                            .created(motebehov.opprettetDato)
                            .status("IKKE_STARTET")
                            .virksomhetsnummer(motebehov.getVirksomhetsnummer());
                })
                .collect(toList());
    }
}
