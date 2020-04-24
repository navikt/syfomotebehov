package no.nav.syfo.api.cors

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class CORSFilter : Filter {
    private val whitelist = listOf(
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
    )

    @Throws(ServletException::class, IOException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val reqUri = httpRequest.requestURI
        if (requestUriErIkkeMotFeedEllerInternalEndepunkt(reqUri)) {
            val origin = httpRequest.getHeader("origin")
            if (erWhitelisted(origin)) {
                httpResponse.addHeader("Access-Control-Allow-Origin", origin)
                httpResponse.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                httpResponse.addHeader("Access-Control-Allow-Credentials", "true")
                httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
            }
        }
        chain.doFilter(request, httpResponse)
    }

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig) {
    }

    override fun destroy() {}
    private fun requestUriErIkkeMotFeedEllerInternalEndepunkt(reqUri: String): Boolean {
        return !(reqUri.contains("/feed") || reqUri.contains("/internal"))
    }

    private fun erWhitelisted(origin: String?): Boolean {
        return origin != null && whitelist.contains(origin)
    }
}