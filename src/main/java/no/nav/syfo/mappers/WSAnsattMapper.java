package no.nav.syfo.mappers;

import no.nav.tjeneste.virksomhet.sykefravaersoppfoelging.v1.informasjon.WSAnsatt;

import java.util.function.Function;

public class WSAnsattMapper {

    public static Function<WSAnsatt, String> wsAnsatt2AktorId = wsAnsatt -> wsAnsatt.getAktoerId();
}
