package no.nav.syfo.config.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.OnBehalfOfOutInterceptor;
import no.nav.syfo.consumer.util.ws.STSClientConfig;
import no.nav.syfo.consumer.util.ws.WsOIDCClient;
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

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

@Slf4j
@Configuration
public class SykefravaersoppfoelgingConfig {

    @Value("${sykefravaersoppfoelging.v1.endpointurl}")
    protected String serviceUrl;

    private SykefravaersoppfoelgingV1 proxy;

    @SuppressWarnings("unchecked")
    @Bean
    @ConditionalOnProperty(value = "mockSykefravaeroppfoelging_V1", havingValue = "false", matchIfMissing = true)
    @Primary
    public SykefravaersoppfoelgingV1 sykefravaersoppfoelgingV1(@Value("${sykefravaersoppfoelging.v1.endpointurl}") String serviceUrl) {
        SykefravaersoppfoelgingV1 port = createPort(serviceUrl, SykefravaersoppfoelgingV1.class, singletonList(new LogErrorHandler()));
        return port;
    }


    public SykefravaersoppfoelgingConfig() {
    }

    public SykefravaersoppfoelgingV1 proxy() {
        if (proxy == null) {
            this.proxy = createPort(this.serviceUrl, SykefravaersoppfoelgingV1.class, singletonList(new LogErrorHandler()));
        }
        return this.proxy;
    }

    public SykefravaersoppfoelgingV1 createPort(String serviceUrl, Class<?> portType, List<Handler> handlers, PhaseInterceptor<? extends Message>... interceptors) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(SykefravaersoppfoelgingV1.class);
        jaxWsProxyFactoryBean.setAddress(serviceUrl);
        SykefravaersoppfoelgingV1 port = (SykefravaersoppfoelgingV1) jaxWsProxyFactoryBean.create();

        STSClientConfig.configureRequestSamlTokenOnBehalfOfOidc(port);

        return port;

//        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
//        jaxWsProxyFactoryBean.setServiceClass(portType);
//        jaxWsProxyFactoryBean.setAddress(Objects.requireNonNull(serviceUrl));
//        jaxWsProxyFactoryBean.getFeatures().add(new WSAddressingFeature());
//        SykefravaersoppfoelgingV1 port = (SykefravaersoppfoelgingV1) jaxWsProxyFactoryBean.create();
//        ((BindingProvider) port).getBinding().setHandlerChain(handlers);
//        Client client = ClientProxy.getClient(port);
//        Arrays.stream(interceptors).forEach(client.getOutInterceptors()::add);
//        STSClientConfig.configureRequestSamlTokenOnBehalfOfOidc(port);
//        return port;
    }

    public WSHentNaermesteLedersAnsattListeResponse hentNaermesteLedersAnsattListe(WSHentNaermesteLedersAnsattListeRequest request, String oidcToken) throws HentNaermesteLedersAnsattListeSikkerhetsbegrensning {

        Client c = ClientProxy.getClient(proxy());
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN_TYPE, OnBehalfOfOutInterceptor.TokenType.OIDC);
        c.getRequestContext().put(OnBehalfOfOutInterceptor.REQUEST_CONTEXT_ONBEHALFOF_TOKEN, oidcToken);

        log.info("JTRACE oidcToken {}", oidcToken);
        log.info("JTRACE aktoerId {}", request.getAktoerId());

        return proxy.hentNaermesteLedersAnsattListe(request);
    }


}
