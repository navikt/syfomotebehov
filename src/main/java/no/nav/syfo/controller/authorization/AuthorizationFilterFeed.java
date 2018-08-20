package no.nav.syfo.controller.authorization;

import no.nav.syfo.controller.exception.AutoriseringsFilterException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static no.nav.syfo.util.AuthorizationFilterUtils.basicCredentials;
import static no.nav.syfo.util.AuthorizationFilterUtils.erRequestAutorisert;

public class AuthorizationFilterFeed implements Filter {
    private static final String BASIC_CREDENTIALS = basicCredentials("syfoveilederoppgaver.systemapi");

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (!erRequestAutorisert(httpServletRequest, BASIC_CREDENTIALS)) {
            throw new AutoriseringsFilterException("Access denied");
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
