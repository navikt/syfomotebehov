package no.nav.syfo.config.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.util.ws.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.*;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

import static java.util.Collections.singletonList;

@Slf4j
@Configuration
public class SykefravaersoppfoelgingConfig {

    @Value("${sykefravaersoppfoelging.v1.url}")
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

    public WSHentNaermesteLederResponse hentNaermesteLeder(WSHentNaermesteLederRequest request, String OIDCToken) throws HentNaermesteLederSikkerhetsbegrensning {
        leggTilOnBehalfOfOutInterceptorForOIDC(ClientProxy.getClient(port), OIDCToken);
        return port.hentNaermesteLeder(request);
    }

    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request, String OIDCToken) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {
        leggTilOnBehalfOfOutInterceptorForOIDC(ClientProxy.getClient(port), OIDCToken);
        return port.hentNaermesteLedersAnsattListe(request);
    }

    public WSHentNaermesteLederListeResponse hentNaermesteLederListe(WSHentNaermesteLederListeRequest request, String OIDCToken) throws HentNaermesteLederListeSikkerhetsbegrensning {
        leggTilOnBehalfOfOutInterceptorForOIDC(ClientProxy.getClient(port), OIDCToken);
        return port.hentNaermesteLederListe(request);
    }

    public WSHentHendelseListeResponse hentHendelseListe(WSHentHendelseListeRequest request, String OIDCToken) throws HentHendelseListeSikkerhetsbegrensning {
        leggTilOnBehalfOfOutInterceptorForOIDC(ClientProxy.getClient(port), OIDCToken);
        return port.hentHendelseListe(request);
    }

    public WSHentSykeforlopperiodeResponse hentSykeforlopperiode(WSHentSykeforlopperiodeRequest request, String OIDCToken) throws HentSykeforlopperiodeSikkerhetsbegrensning {
        leggTilOnBehalfOfOutInterceptorForOIDC(ClientProxy.getClient(port), OIDCToken);
        return port.hentSykeforlopperiode(request);
    }


    private void leggTilOnBehalfOfOutInterceptorForOIDC(Client client, String OIDCToken) {
        client.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN_TYPE, OnBehalfOfOutInterceptor.TokenType.OIDC);
        client.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN, OIDCToken);
    }
}
