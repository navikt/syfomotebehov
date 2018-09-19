package no.nav.syfo.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class AuthorizationFilterUtils {
    public static boolean erRequestAutorisert(HttpServletRequest httpServletRequest, String credential) {
        return ofNullable(httpServletRequest.getHeader(AUTHORIZATION)).map(credential::equals).orElse(false);
    }

    public static String basicCredentials(String credential) {
        log.info("Cred: " + credential);
        log.info("User: " + getenv(credential + ".username"));
        log.info("Pass: " + getenv(credential + ".password"));

        return "Basic " + Base64.getEncoder().encodeToString(format("%s:%s", getenv(credential + ".username"), getenv(credential + ".password")).getBytes());
    }
}
