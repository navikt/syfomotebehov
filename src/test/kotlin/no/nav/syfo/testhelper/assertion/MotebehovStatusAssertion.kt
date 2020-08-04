package no.nav.syfo.testhelper.assertion

import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import org.junit.jupiter.api.Assertions

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovSvar: MotebehovSvar?
) {
    Assertions.assertEquals(expVisMotebehov, this.visMotebehov)
    Assertions.assertEquals(expSkjemaType, this.skjemaType)
    if (expMotebehovSvar != null) {
        Assertions.assertEquals(expMotebehovSvar, this.motebehov!!.motebehovSvar)
        Assertions.assertEquals(expSkjemaType, this.motebehov!!.skjemaType)
    } else {
        Assertions.assertEquals(expMotebehovSvar, this.motebehov)
    }
}
