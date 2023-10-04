package no.nav.syfo.leaderelection

import no.nav.syfo.metric.Metric
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.InetAddress
import javax.inject.Inject

@Service
class LeaderElectionClient @Inject constructor(
    private val metric: Metric,
    private val restTemplate: RestTemplate,
    @Value("\${elector.path}")
    private val electorpath: String,
) {

    fun isLeader(): Boolean {
        if (electorpath.equals("dont_look_for_leader")) {
            return false
        }
        metric.tellHendelse("isLeader_kalt")
        val url = "http://$electorpath"

        try {
            val response: Leader? = restTemplate.getForObject(url, Leader::class.java)
            if (response == null) {
                log.error("Call to elector returned null")
                metric.tellHendelse("isLeader_feilet")
                throw RuntimeException("Call to elector returned null")
            }
            return isHostLeader(response)
        } catch (e: IOException) {
            log.error("Couldn't map response from electorPath to Leader object", e)
            metric.tellHendelse("isLeader_feilet")
            throw RuntimeException("Couldn't map response from electorpath to LeaderPod object", e)
        } catch (e: Exception) {
            log.error("Something went wrong when trying to check leader", e)
            metric.tellHendelse("isLeader_feilet")
            throw RuntimeException("Got exception when trying to find leader", e)
        }
    }

    private fun isHostLeader(leader: Leader): Boolean {
        val hostName = InetAddress.getLocalHost().hostName
        return hostName == leader.name
    }

    private data class Leader(
        val name: String,
    )

    companion object {
        private val log = LoggerFactory.getLogger(LeaderElectionClient::class.java)
    }
}
