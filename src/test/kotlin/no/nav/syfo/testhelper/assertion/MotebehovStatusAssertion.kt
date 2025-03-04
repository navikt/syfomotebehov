package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.MotebehovSvar
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovSvar: MotebehovSvar?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expMotebehovSvar != null) {
        expMotebehovSvar shouldBe this.motebehov!!.motebehovSvar
        expSkjemaType shouldBe this.motebehov!!.skjemaType
    } else {
        expMotebehovSvar shouldBe this.motebehov
    }
}
