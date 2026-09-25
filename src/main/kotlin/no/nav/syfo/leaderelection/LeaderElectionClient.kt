package no.nav.syfo.leaderelection

import no.nav.esyfo.observability.rethrowIfCancelled
import no.nav.syfo.metric.Metric
import no.nav.syfo.util.failureKind
import no.nav.syfo.util.withFailureDiagnostics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import javax.inject.Inject

@Service
class LeaderElectionClient
    @Inject
    constructor(
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
                    throw RuntimeException("Call to elector returned null")
                }
                return isHostLeader(response)
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                log
                    .atError()
                    .addKeyValue("event_type", "leader_election_failed")
                    .addKeyValue("operation", "leader_lookup")
                    .addKeyValue("upstream", "elector")
                    .addKeyValue("failure_stage", "upstream_request")
                    .addKeyValue("failure_kind", e.failureKind().value)
                    .addKeyValue("error_code", "LEADER_LOOKUP_FAILED")
                    .withFailureDiagnostics(e)
                    .log("Could not determine the elected leader")
                metric.tellHendelse("isLeader_feilet")
                throw RuntimeException("Could not determine the elected leader", e)
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
