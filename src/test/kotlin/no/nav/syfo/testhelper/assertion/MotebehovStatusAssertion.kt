package no.nav.syfo.testhelper.assertion

import no.nav.syfo.motebehov.MotebehovSvarOutputDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import org.junit.jupiter.api.Assertions

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovSvarOutputDTO: MotebehovSvarOutputDTO?
) {
    Assertions.assertEquals(expVisMotebehov, this.visMotebehov)
    Assertions.assertEquals(expSkjemaType, this.skjemaType)
    if (expMotebehovSvarOutputDTO != null) {
        Assertions.assertEquals(expMotebehovSvarOutputDTO, this.motebehov!!.motebehovSvar)
        Assertions.assertEquals(expSkjemaType, this.motebehov!!.skjemaType)
    } else {
        Assertions.assertEquals(expMotebehovSvarOutputDTO, this.motebehov)
    }
}
