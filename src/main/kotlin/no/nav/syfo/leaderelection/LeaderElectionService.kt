package no.nav.syfo.leaderelection

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.metric.Metric
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.InetAddress
import javax.inject.Inject

@Service
class LeaderElectionService @Inject constructor(
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
        val objectMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val url = "http://$electorpath"

        val response: String = restTemplate.getForObject(url, String::class.java)

        return try {
            val leader = objectMapper.readValue(response, LeaderPod::class.java)
            isHostLeader(leader)
        } catch (e: IOException) {
            log.error("Couldn't map response from electorPath to LeaderPod object", e)
            metric.tellHendelse("isLeader_feilet")
            throw RuntimeException("Couldn't map response from electorpath to LeaderPod object", e)
        } catch (e: Exception) {
            log.error("Something went wrong when trying to check leader", e)
            metric.tellHendelse("isLeader_feilet")
            throw RuntimeException("Got exception when trying to find leader", e)
        }
    }

    private fun isHostLeader(leader: LeaderPod): Boolean {
        val hostName = InetAddress.getLocalHost().hostName
        val leaderName: String = leader.name
        return hostName == leaderName
    }

    companion object {
        private val log = LoggerFactory.getLogger(LeaderElectionService::class.java)
    }
}
