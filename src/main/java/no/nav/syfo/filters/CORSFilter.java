package no.nav.syfo.filters;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class CORSFilter implements Filter {

    private List<String> whitelist = Arrays.asList(
            "https://syfomodiaperson.nais.adeo.no",
            "https://syfomodiaperson.nais.preprod.local",
            "https://syfooversikt.nais.adeo.no",
            "https://syfooversikt.nais.preprod.local",
            "https://syfooversikt-q1.nais.preprod.local",
            "https://modiasyfofront.nais.adeo.no",
            "https://modiasyfofront.nais.preprod.local",
            "https://modiasyfofront-q1.nais.preprod.local",
            "https://fastlegefront.nais.adeo.no",
            "https://fastlegefront.nais.preprod.local",
            "https://fastlegefront-q1.nais.preprod.local",
            "https://app.adeo.no",
            "https://app-q1.adeo.no"
    );
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String reqUri = httpRequest.getRequestURI();
        if (requestUriErIkkeMotFeedEllerInternalEndepunkt(reqUri)) {
            String origin = httpRequest.getHeader("origin");
            if (erWhitelisted(origin)) {
                httpResponse.addHeader("Access-Control-Allow-Origin", origin);
                httpResponse.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
                httpResponse.addHeader("Access-Control-Allow-Credentials", "true");
                httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            }
        }
        chain.doFilter(request, httpResponse);
    }

    public void init(FilterConfig filterConfig) throws ServletException { }

    public void destroy() { }

    private boolean requestUriErIkkeMotFeedEllerInternalEndepunkt(String reqUri) {
        return !(reqUri.contains("/feed") || reqUri.contains("/internal"));
    }

    private boolean erWhitelisted(String origin) {
        return origin != null && whitelist.contains(origin);
    }

}
