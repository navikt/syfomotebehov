package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovFormValuesOutputDTO: MotebehovFormValuesOutputDTO?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expMotebehovFormValuesOutputDTO != null) {
        expMotebehovFormValuesOutputDTO shouldBe this.motebehov!!.formValues
        expSkjemaType shouldBe this.motebehov!!.skjemaType
    } else {
        expMotebehovFormValuesOutputDTO shouldBe this.motebehov
    }
}
