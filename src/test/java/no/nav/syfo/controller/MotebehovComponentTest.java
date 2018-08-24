//package no.nav.syfo.controller;
//
//import com.nimbusds.jwt.SignedJWT;
//import no.nav.security.oidc.context.OIDCClaims;
//import no.nav.security.oidc.context.OIDCRequestContextHolder;
//import no.nav.security.oidc.context.OIDCValidationContext;
//import no.nav.security.oidc.context.TokenContext;
//import no.nav.security.spring.oidc.test.JwtTokenGenerator;
//import no.nav.syfo.LocalApplication;
//import no.nav.syfo.domain.rest.Fnr;
//import no.nav.syfo.domain.rest.Motebehov;
//import no.nav.syfo.domain.rest.MotebehovSvar;
//import no.nav.syfo.domain.rest.NyttMotebehov;
//import no.nav.syfo.mock.AktoerMock;
//import no.nav.syfo.repository.dao.MotebehovDAO;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.inject.Inject;
//import java.util.List;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Komponent / blackbox test av møtebehovsfunskjonaliteten - test at input til endepunktet (controlleren, for enkelhets skyld)
// * lagres og hentes riktig fra minnedatabasen.
// */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = LocalApplication.class)
//@DirtiesContext
//public class MotebehovComponentTest {
//
//    private static final String ARBEIDSTAKER_FNR = "12345678910";
//    private static final String LEDER_FNR = "10987654321";
//    private static final String LEDER_AKTORID = AktoerMock.mockAktorId(LEDER_FNR);
//    private static final String VIRKSOMHETSNUMMER = "1234";
//
//    @Inject
//    private MotebehovController motebehovController;
//
//    @Inject
//    private OIDCRequestContextHolder oidcRequestContextHolder;
//
//    @Inject
//    private MotebehovDAO motebehovDAO;
//
//    @Before
//    public void setUp() {
//        mockOIDC(LEDER_FNR);
//        cleanDB();
//    }
//
//    @After
//    public void tearDown() {
//        oidcRequestContextHolder.setOIDCValidationContext(null);
//        cleanDB();
//    }
//
//    @Test
//    public void lagreOgHentMotebehov() {
//        final MotebehovSvar motebehovSvar = new MotebehovSvar()
//                .harMotebehov(true)
//                .friskmeldingForventning("Om en uke")
//                .tiltak("Krykker")
//                .tiltakResultat("Kommer seg fremover")
//                .forklaring("");
//
//        final NyttMotebehov lagreMotebehov = new NyttMotebehov()
//                .arbeidstakerFnr(Fnr.of(ARBEIDSTAKER_FNR))
//                .virksomhetsnummer(VIRKSOMHETSNUMMER)
//                .motebehovSvar(
//                        motebehovSvar
//                );
//
//        // Lagre
//        UUID uuid = motebehovController.lagreMotebehov(lagreMotebehov);
//
//        // Hent
//        List<Motebehov> motebehovListe = motebehovController.hentMotebehovListe(ARBEIDSTAKER_FNR);
//        assertThat(motebehovListe).size().isOne();
//
//        Motebehov motebehov = motebehovListe.get(0);
//        assertThat(motebehov.id).isEqualTo(uuid);
//        assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID);
//        assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR);
//        assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER);
//        assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar);
//    }
//
//    private void mockOIDC(String subject) {
//        //OIDC-hack - legg til token og oidcclaims for en test-person
//        SignedJWT jwt = JwtTokenGenerator.createSignedJWT(subject);
//        String issuer = "selvbetjening";
//        TokenContext tokenContext = new TokenContext(issuer, jwt.serialize());
//        OIDCClaims oidcClaims = new OIDCClaims(jwt);
//        OIDCValidationContext oidcValidationContext = new OIDCValidationContext();
//        oidcValidationContext.addValidatedToken(issuer, tokenContext, oidcClaims);
//        oidcRequestContextHolder.setOIDCValidationContext(oidcValidationContext);
//    }
//
//    private void cleanDB() {
//        motebehovDAO.nullstillMotebehov(LEDER_AKTORID);
//    }
//
//}