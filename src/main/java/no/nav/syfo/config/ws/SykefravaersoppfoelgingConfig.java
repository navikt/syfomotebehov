package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.*;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.HentNaermesteLedersAnsattListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.SykefravaersoppfoelgingV1;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeRequest;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.meldinger.WSHentNaermesteLedersAnsattListeResponse;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

@Component
public class SykefravaersoppfoelgingConfig {
//    @SuppressWarnings("unchecked")
//    @Bean
//    @ConditionalOnProperty(value = "mockSykefravaeroppfoelging_V1", havingValue = "false", matchIfMissing = true)
//    @Primary
//    public SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1(@Value("${sykefravaersoppfoelging.v1.endpointurl}") String serviceUrl) {
//        SykefravaersoppfoelgingV1 port = new WsOIDCClient<SykefravaersoppfoelgingV1>().createPort();
//        return port;
//    }

    @Value("${sykefravaersoppfoelging.v1.endpointurl}")
    protected String adress;
    
    private SykefravaersoppfoelgingV1 proxy;

    public SykefravaersoppfoelgingConfig() {
    }

    @SuppressWarnings("unchecked")
    public SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1() {
        if (proxy == null) {
            this.proxy = createPort(this.adress, SykefravaersoppfoelgingV1.class, singletonList(new LogErrorHandler()));
        }
        return this.proxy;
    }

    public SykefravaersoppfoelgingV1 createPort(String serviceUrl, Class<?> portType, List<Handler> handlers, PhaseInterceptor<? extends Message>... interceptors) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(portType);
        jaxWsProxyFactoryBean.setAddress(Objects.requireNonNull(serviceUrl));
        jaxWsProxyFactoryBean.getFeatures().add(new WSAddressingFeature());
        SykefravaersoppfoelgingV1 port = (SykefravaersoppfoelgingV1) jaxWsProxyFactoryBean.create();
        ((BindingProvider) port).getBinding().setHandlerChain(handlers);
        Client client = ClientProxy.getClient(port);
        Arrays.stream(interceptors).forEach(client.getOutInterceptors()::add);
        STSClientConfig.configureRequestSamlTokenOnBehalfOfOidc(port);
        return port;
    }

    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request, String oidcToken) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {

        Client c = ClientProxy.getClient(sykefravaersoppfoelgingV1());
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN_TYPE, OnBehalfOfOutInterceptor.TokenType.OIDC);
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN, oidcToken);

        return proxy.hentNaermesteLedersAnsattListe(request);
    }


}
