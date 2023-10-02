package no.nav.syfo.leaderelection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.syfo.metric.Metric;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class LeaderElectionService {

    private static final Logger log = getLogger(LeaderElectionService.class);

    private final Metric metrikk;
    private final RestTemplate restTemplate;
    private final String electorpath;

    @Inject
    public LeaderElectionService(
            Metric metrikk,
            RestTemplate restTemplate,
            @Value("${elector.path}") String electorpath
    ) {
        this.metrikk = metrikk;
        this.restTemplate = restTemplate;
        this.electorpath = electorpath;
    }

    public boolean isLeader() {
        if (electorpath.equals("dont_look_for_leader"))
            return false;
        metrikk.tellHendelse("isLeader_kalt");
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String url = "http://" + electorpath;

        String response = restTemplate.getForObject(url, String.class);

        try {
            LeaderPod leader = objectMapper.readValue(response, LeaderPod.class);
            return isHostLeader(leader);
        } catch (IOException e) {
            log.error("Couldn't map response from electorPath to LeaderPod object", e);
            metrikk.tellHendelse("isLeader_feilet");
            throw new RuntimeException("Couldn't map response from electorpath to LeaderPod object", e);
        } catch (Exception e) {
            log.error("Something went wrong when trying to check leader", e);
            metrikk.tellHendelse("isLeader_feilet");
            throw new RuntimeException("Got exception when trying to find leader", e);
        }
    }

    private boolean isHostLeader(LeaderPod leader) throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName();
        String leaderName = leader.getName();

        return hostName.equals(leaderName);
    }
}
