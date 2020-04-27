package no.nav.syfo.testhelper

object UserConstants {
    const val ARBEIDSTAKER_FNR = "12345678912"
    @JvmField
    val ARBEIDSTAKER_AKTORID = mockAktorId(ARBEIDSTAKER_FNR)
    const val LEDER_FNR = "12987654321"
    @JvmField
    val LEDER_AKTORID = mockAktorId(LEDER_FNR)
    const val VIRKSOMHETSNUMMER = "1234"
    const val NAV_ENHET = "0330"
    const val NAV_ENHET_NAVN = "Bjerke"
    const val VEILEDER_ID = "Z999999"
    const val VEILEDER_2_ID = "Z888888"
    const val STS_TOKEN = "123456789"
    const val PERSON_NAME_FIRST = "First"
    const val PERSON_NAME_MIDDLE = "Middle"
    const val PERSON_NAME_LAST = "Last"
    const val PERSON_FULL_NAME = "$PERSON_NAME_FIRST $PERSON_NAME_MIDDLE $PERSON_NAME_LAST"
}