package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovSvarOutputDTO: MotebehovFormValuesOutputDTO?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expMotebehovSvarOutputDTO != null) {
        expMotebehovSvarOutputDTO shouldBe this.motebehov!!.motebehovSvar
        expSkjemaType shouldBe this.motebehov!!.skjemaType
    } else {
        expMotebehovSvarOutputDTO shouldBe this.motebehov
    }
}
