package no.nav.syfo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Toggle {

    public static boolean enableNullstill;
    public static boolean endepunkterForMotebehov;
    public static String pilotKontorer;

    public Toggle(
            @Value("${toggle.enable.nullstill:false}") boolean enableNullstill,
            @Value("${toggle.enable.motebehov:false}") boolean endepunkterForMotebehov,
            @Value("${toggle.mvp.kontorer:}") String pilotKontorer
    ) {
        Toggle.enableNullstill = enableNullstill;
        Toggle.endepunkterForMotebehov = endepunkterForMotebehov;
        Toggle.pilotKontorer = pilotKontorer;
    }
}
