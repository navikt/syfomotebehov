package no.nav.syfo.util;

import static java.lang.System.getenv;

public class RestUtils {

    private final static String NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME";
    final static String DEV_FSS = "dev-fss";

    //TODO Hent fra Fasit via property i nais.yaml i stedet
    public static String baseUrl() {
        return "https://app" + miljo() + ".adeo.no";
    }

    private static String miljo() {
        String environmentName = getenv(NAIS_CLUSTER_NAME);
        if (DEV_FSS.equals(environmentName)) {
            return "-q1";
        }
        return "";
    }
}
