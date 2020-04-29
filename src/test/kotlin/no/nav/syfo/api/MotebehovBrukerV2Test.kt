package no.nav.syfo.api

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.syfo.LocalApplication
import no.nav.syfo.aktorregister.AktorregisterConsumer
import no.nav.syfo.aktorregister.domain.Fodselsnummer
import no.nav.syfo.mote.MoterService
import no.nav.syfo.motebehov.DAYS_END_DIALOGMOTE2
import no.nav.syfo.motebehov.DAYS_START_DIALOGMOTE2
import no.nav.syfo.motebehov.MotebehovSkjemaType
import no.nav.syfo.motebehov.database.MotebehovDAO
import no.nav.syfo.oppfolgingstilfelle.database.OppfolgingstilfelleDAO
import no.nav.syfo.oversikthendelse.OversikthendelseProducer
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.sts.StsConsumer
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.OidcTestHelper.loggInnBruker
import no.nav.syfo.testhelper.OidcTestHelper.loggUtAlle
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.LEDER_AKTORID
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER_2
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.util.function.Consumer
import javax.inject.Inject

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [LocalApplication::class])
@DirtiesContext
class MotebehovBrukerV2Test {
    @Value("\${syfobehandlendeenhet.url}")
    private lateinit var behandlendeenhetUrl: String

    @Value("\${syfobrukertilgang.url}")
    private lateinit var brukertilgangUrl: String

    @Value("\${security.token.service.rest.url}")
    private lateinit var stsUrl: String

    @Value("\${srv.username}")
    private lateinit var srvUsername: String

    @Value("\${srv.password}")
    private lateinit var srvPassword: String

    @Inject
    private lateinit var motebehovController: MotebehovBrukerV2Controller

    @Inject
    private lateinit var oidcRequestContextHolder: OIDCRequestContextHolder

    @Inject
    private lateinit var motebehovDAO: MotebehovDAO

    @Inject
    private lateinit var cacheManager: CacheManager

    @Inject
    private lateinit var stsConsumer: StsConsumer

    @Inject
    private lateinit var oppfolgingstilfelleDAO: OppfolgingstilfelleDAO

    @Inject
    private lateinit var restTemplate: RestTemplate

    @MockBean
    private lateinit var aktorregisterConsumer: AktorregisterConsumer

    @MockBean
    private lateinit var moterService: MoterService

    @MockBean
    private lateinit var pdlConsumer: PdlConsumer

    @MockBean
    private lateinit var oversikthendelseProducer: OversikthendelseProducer

    private lateinit var mockRestServiceServer: MockRestServiceServer

    private val motebehovGenerator = MotebehovGenerator()

    @Before
    fun setUp() {
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(ARBEIDSTAKER_AKTORID)
        Mockito.`when`(aktorregisterConsumer.getAktorIdForFodselsnummer(Fodselsnummer(LEDER_FNR))).thenReturn(LEDER_AKTORID)
        Mockito.`when`(pdlConsumer.person(Fodselsnummer(ARBEIDSTAKER_FNR))).thenReturn(generatePdlHentPerson(null, null))
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
        loggInnBruker(oidcRequestContextHolder, LEDER_FNR)
        cleanDB()
        mockAndExpectBrukertilgangRequest(mockRestServiceServer, brukertilgangUrl, ARBEIDSTAKER_FNR)
    }

    @After
    fun tearDown() {
        loggUtAlle(oidcRequestContextHolder)
        mockRestServiceServer.reset()
        cacheManager.cacheNames
                .forEach(Consumer { cacheName: String ->
                    val cache = cacheManager.getCache(cacheName)
                    cache?.clear()
                })
        cleanDB()
    }

    @Test
    fun getMotebehovStatusWithNoOppfolgingstilfelle() {
        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithTodayOutsideOppfolgingstilfelle() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(10)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(1)
                        )
                )
        ))
        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getAsArbeidstakerMotebehovStatusWithTodayOutsideOppfolgingstilfelle() {
        loggUtAlle(oidcRequestContextHolder)
        loggInnBruker(oidcRequestContextHolder, ARBEIDSTAKER_FNR)

        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(10)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(1)
                        )
                )
        ))
        val motebehovStatus = motebehovController.motebehovStatus(null, "")
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleBeforeDialogmote2StartDate() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(DAYS_START_DIALOGMOTE2).plusDays(1)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().plusDays(1)
                        )
                )
        ))
        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithTodayInsideOppfolgingstilfelleAfterDialogmote2EndDate() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(DAYS_END_DIALOGMOTE2)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().plusDays(1)
                        )
                )
        ))
        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMoteInsideInsideDialogmote2UpperLimit() {
        val kOppfolgingstilfelle = generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(DAYS_END_DIALOGMOTE2).plusDays(1)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().plusDays(1)
                        )
                )
        )
        Mockito.`when`(moterService.erMoteOpprettetForArbeidstakerEtterDato(
                ARBEIDSTAKER_AKTORID,
                kOppfolgingstilfelle.tidslinje.first().dag.atStartOfDay()
        )).thenReturn(false)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertTrue(motebehovStatus.visMotebehov)
        Assert.assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMoteInsideInsideDialogmote2LowerLimit() {
        val kOppfolgingstilfelle = generateKOppfolgingstilfelle().copy(
                tidslinje = listOf(
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().minusDays(DAYS_START_DIALOGMOTE2)
                        ),
                        generateKSyketilfelledag().copy(
                                dag = LocalDate.now().plusDays(1)
                        )
                )
        )
        Mockito.`when`(moterService.erMoteOpprettetForArbeidstakerEtterDato(
                ARBEIDSTAKER_AKTORID,
                kOppfolgingstilfelle.tidslinje.first().dag.atStartOfDay()
        )).thenReturn(false)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertTrue(motebehovStatus.visMotebehov)
        Assert.assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndNoMote() {
        val kOppfolgingstilfelle = generateKOppfolgingstilfelle()
        Mockito.`when`(moterService.erMoteOpprettetForArbeidstakerEtterDato(
                ARBEIDSTAKER_AKTORID,
                kOppfolgingstilfelle.tidslinje.first().dag.atStartOfDay()
        )).thenReturn(false)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertTrue(motebehovStatus.visMotebehov)
        Assert.assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getMotebehovStatusWithNoMotebehovAndMote() {
        val kOppfolgingstilfelle = generateKOppfolgingstilfelle()
        Mockito.`when`(moterService.erMoteOpprettetForArbeidstakerEtterDato(
                ARBEIDSTAKER_AKTORID,
                kOppfolgingstilfelle.tidslinje.first().dag.atStartOfDay()
        )).thenReturn(true)

        oppfolgingstilfelleDAO.create(kOppfolgingstilfelle)

        val motebehovStatus = motebehovController.motebehovStatus(ARBEIDSTAKER_FNR, VIRKSOMHETSNUMMER)
        Assert.assertFalse(motebehovStatus.visMotebehov)
        Assert.assertNull(motebehovStatus.skjemaType)
        Assert.assertNull(motebehovStatus.motebehov)
    }

    @Test
    fun getAsArbeidstakerMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                aktorId = ARBEIDSTAKER_AKTORID,
                orgnummer = VIRKSOMHETSNUMMER
        ))
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true, getAsArbeidsgiver = false)
    }


    @Test
    fun getAsArbeidstakerbehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle().copy(
                aktorId = ARBEIDSTAKER_AKTORID,
                orgnummer = VIRKSOMHETSNUMMER_2
        ))
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false, getAsArbeidsgiver = false)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovTrue() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle())
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = true, getAsArbeidsgiver = true)
    }

    @Test
    fun getMotebehovStatusAndSendOversikthendelseWithMotebehovHarBehovFalse() {
        oppfolgingstilfelleDAO.create(generateKOppfolgingstilfelle())
        lagreOgHentMotebehovOgSendOversikthendelse(harBehov = false, getAsArbeidsgiver = true)
    }

    private fun mockSTS() {
        if (!stsConsumer.isTokenCached()) {
            mockAndExpectSTSService(mockRestServiceServer, stsUrl, srvUsername, srvPassword)
        }
    }

    private fun lagreOgHentMotebehovOgSendOversikthendelse(harBehov: Boolean, getAsArbeidsgiver: Boolean) {
        if (!getAsArbeidsgiver) {
            mockRestServiceServer.reset()
            loggUtAlle(oidcRequestContextHolder)
            loggInnBruker(oidcRequestContextHolder, ARBEIDSTAKER_FNR)
        }
        mockSTS()
        mockAndExpectBehandlendeEnhetRequest(mockRestServiceServer, behandlendeenhetUrl, ARBEIDSTAKER_FNR)

        val motebehovSvar = motebehovGenerator.lagMotebehovSvar(harBehov)

        motebehovController.lagreMotebehov(motebehovGenerator.lagNyttMotebehovFraAT().copy(
                motebehovSvar = motebehovSvar
        ))

        val motebehovStatus = motebehovController.motebehovStatus(
                if (getAsArbeidsgiver) ARBEIDSTAKER_FNR else null,
                if (getAsArbeidsgiver) VIRKSOMHETSNUMMER else ""
        )
        Assert.assertTrue(motebehovStatus.visMotebehov)
        Assert.assertEquals(MotebehovSkjemaType.SVAR_BEHOV, motebehovStatus.skjemaType)
        val motebehov = motebehovStatus.motebehov!!
        Assert.assertNotNull(motebehov)
        if (getAsArbeidsgiver) {
            Assertions.assertThat(motebehov.opprettetAv).isEqualTo(LEDER_AKTORID)
        } else {
            Assertions.assertThat(motebehov.opprettetAv).isEqualTo(ARBEIDSTAKER_AKTORID)
        }
        Assertions.assertThat(motebehov.arbeidstakerFnr).isEqualTo(ARBEIDSTAKER_FNR)
        Assertions.assertThat(motebehov.virksomhetsnummer).isEqualTo(VIRKSOMHETSNUMMER)
        Assertions.assertThat(motebehov.motebehovSvar).isEqualToComparingFieldByField(motebehovSvar)
        if (harBehov) {
            Mockito.verify(oversikthendelseProducer).sendOversikthendelse(any())
        } else {
            Mockito.verify(oversikthendelseProducer, Mockito.never()).sendOversikthendelse(any())
        }
    }


    private fun cleanDB() {
        motebehovDAO.nullstillMotebehov(ARBEIDSTAKER_AKTORID)
        oppfolgingstilfelleDAO.nullstillOppfolgingstilfeller(ARBEIDSTAKER_AKTORID)
    }
}

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T
