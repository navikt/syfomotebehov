package no.nav.syfo.mappers;

import no.nav.syfo.domain.rest.NaermesteLeder;
import no.nav.syfo.domain.rest.NaermesteLederStatus;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSNaermesteLeder;
import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSNaermesteLederStatus;

import java.util.function.Function;

import static no.nav.syfo.util.MapUtil.map;

public class WSNaermesteLederMapper {

    private static Function<WSNaermesteLederStatus, NaermesteLederStatus> ws2naermesteLederStatus = wsNaermesteLederStatus -> new NaermesteLederStatus()
            .erAktiv(wsNaermesteLederStatus.isErAktiv())
            .aktivFom(wsNaermesteLederStatus.getAktivFom())
            .aktivTom(wsNaermesteLederStatus.getAktivTom());

    public static Function<WSNaermesteLeder, NaermesteLeder> ws2naermesteLeder = wsNaermesteLeder -> new NaermesteLeder()
            .naermesteLederId(wsNaermesteLeder.getNaermesteLederId())
            .naermesteLederAktoerId(wsNaermesteLeder.getNaermesteLederAktoerId())
            .naermesteLederStatus(map(wsNaermesteLeder.getNaermesteLederStatus(), ws2naermesteLederStatus))
            .orgnummer(wsNaermesteLeder.getOrgnummer())
            .epost(wsNaermesteLeder.getEpost())
            .mobil(wsNaermesteLeder.getMobil())
            .navn(wsNaermesteLeder.getNavn());
}
