package no.nav.syfo.batch

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.InetAddress
import javax.inject.Inject

@Service
class LeaderElectionService @Inject constructor(
    private val restTemplate: RestTemplate,
    @Value("\${elector.path}") private val electorpath: String
) {
    fun isLeader(): Boolean {
        val objectMapper = ObjectMapper()
        val url = "http://$electorpath"
        val response = restTemplate.getForObject(url, String::class.java)
        return try {
            val leader = objectMapper.readValue(response, LeaderPod::class.java)
            isHostLeader(leader)
        } catch (e: IOException) {
            log.error("Couldn't map response from electorPath to LeaderPod object", e)
            throw RuntimeException("Couldn't map response from electorpath to LeaderPod object", e)
        } catch (e: Exception) {
            log.error("Something went wrong when trying to check leader", e)
            throw RuntimeException("Got exception when trying to find leader", e)
        }
    }

    @Throws(Exception::class)
    private fun isHostLeader(leader: LeaderPod): Boolean {
        val hostName = InetAddress.getLocalHost().hostName
        val leaderName = leader.name
        return hostName == leaderName
    }

    companion object {
        private val log = LoggerFactory.getLogger(LeaderElectionService::class.java)
    }
}
