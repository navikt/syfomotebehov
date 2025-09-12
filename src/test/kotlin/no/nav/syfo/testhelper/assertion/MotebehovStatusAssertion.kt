package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO

fun MotebehovStatusWithFormValuesDTO.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType? = null,
    expMotebehovFormValues: MotebehovFormValuesOutputDTO? = null
) {
    this.visMotebehov shouldBe expVisMotebehov

    if (expSkjemaType != null) {
        this.skjemaType shouldBe expSkjemaType
    }

    if (expMotebehovFormValues != null) {
        this.motebehovWithFormValues shouldNotBe null
        this.motebehovWithFormValues!!.formValues shouldBe expMotebehovFormValues
        this.motebehovWithFormValues!!.skjemaType shouldBe expSkjemaType
    } else {
        this.motebehovWithFormValues shouldBe null
    }
}
