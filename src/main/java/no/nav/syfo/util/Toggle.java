package no.nav.syfo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Toggle {

    public static boolean enableNullstill;

    public Toggle(
            @Value("${toggle.enable.nullstill:false}") boolean enableNullstill
    ) {
        Toggle.enableNullstill = enableNullstill;
    }
}
