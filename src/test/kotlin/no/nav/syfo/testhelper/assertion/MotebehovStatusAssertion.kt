package no.nav.syfo.testhelper.assertion

import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import org.assertj.core.api.Assertions
import org.junit.Assert

fun MotebehovStatus.assertMotebehovStatus(
        expVisMotebehov: Boolean,
        expSkjemaType: MotebehovSkjemaType?,
        expMotebehovSvar: MotebehovSvar?
) {
    Assert.assertEquals(expVisMotebehov, this.visMotebehov)
    Assert.assertEquals(expSkjemaType, this.skjemaType)
    if (expMotebehovSvar != null) {
        Assert.assertEquals(expMotebehovSvar, this.motebehov!!.motebehovSvar)
        Assert.assertEquals(expSkjemaType, this.motebehov!!.skjemaType)
    } else {
        Assert.assertEquals(expMotebehovSvar, this.motebehov)
    }
}