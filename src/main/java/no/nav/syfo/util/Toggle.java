package no.nav.syfo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Toggle {

    public static boolean enableNullstill;
    public static boolean endepunkterForMotebehov;

    public Toggle(@Value("${toggle.enable.nullstill:false}") boolean enableNullstill,
                  @Value("${toggle.enable.motebehov:false}") boolean endepunkterForMotebehov) {
        Toggle.enableNullstill = enableNullstill;
        Toggle.endepunkterForMotebehov = endepunkterForMotebehov;
    }
}