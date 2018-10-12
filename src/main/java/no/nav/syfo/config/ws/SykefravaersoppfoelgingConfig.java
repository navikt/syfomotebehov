package no.nav.syfo.config.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.OnBehalfOfOutInterceptor;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLedersAnsattListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.SykefravaersoppfoelgingV1;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeResponse;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static java.util.Collections.singletonList;

@Slf4j
@Configuration
public class SykefravaersoppfoelgingConfig {

    @Value("${sykefravaersoppfoelging.v1.endpointurl}")
    protected String serviceUrl;

    private SykefravaersoppfoelgingV1 port;

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockSykefravaeroppfoelging_V1", havingValue = "false", matchIfMissing = true)
    @Primary
    public SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1() {
        SykefravaersoppfoelgingV1 port = new WsClient<SykefravaersoppfoelgingV1>().createPort(serviceUrl, SykefravaersoppfoelgingV1.class, singletonList(new LogErrorHandler()));
        STSClientConfig.configureRequestSamlTokenOnBehalfOfOidc(port);
        this.port = port;
        return port;
    }


    public SykefravaersoppfoelgingConfig() {
    }

    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request, String OIDCToken) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {
        leggTilOidcOnBehalfOfOutInterceptor(port, OIDCToken);

        log.info("JTRACE oidcToken {}", OIDCToken);
        log.info("JTRACE aktoerId {}", request.getAktoerId());

        return port.hentNaermesteLedersAnsattListe(request);
    }

    private void leggTilOidcOnBehalfOfOutInterceptor(SykefravaersoppfoelgingV1 port, String OIDCToken) {
        Client c = ClientProxy.getClient(port);
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN_TYPE, OnBehalfOfOutInterceptor.TokenType.OIDC);
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN, OIDCToken);
    }
}
