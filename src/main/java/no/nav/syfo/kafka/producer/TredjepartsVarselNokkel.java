package no.nav.syfo.kafka.producer;

public enum TredjepartsVarselNokkel {
    NAERMESTE_LEDER_SVAR_MOTEBEHOV("syfoNaermesteLederSvarMotebehov");

    private String id;

    TredjepartsVarselNokkel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
