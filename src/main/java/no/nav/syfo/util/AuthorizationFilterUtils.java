package no.nav.syfo.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class AuthorizationFilterUtils {
    public static boolean erRequestAutorisert(HttpServletRequest httpServletRequest, String credential) {
        return ofNullable(httpServletRequest.getHeader(AUTHORIZATION)).map(credential::equals).orElse(false);
    }

    public static String basicCredentials(String credential) {
        return "Basic " + Base64.getEncoder().encodeToString(format("%s:%s", getProperty(credential + ".username"), getProperty(credential + ".password")).getBytes());
    }
}
