package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.MotebehovSvarLegacyDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithLegacyMotebehovDTO

fun MotebehovStatusWithLegacyMotebehovDTO.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expLegacyMotebehovSvar: MotebehovSvarLegacyDTO?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expLegacyMotebehovSvar != null) {
        this.motebehov.shouldNotBeNull()
        expLegacyMotebehovSvar.harMotebehov shouldBe this.motebehov!!.motebehovSvar.harMotebehov
        expLegacyMotebehovSvar.forklaring shouldBe this.motebehov!!.motebehovSvar.forklaring
        expSkjemaType shouldBe this.motebehov!!.skjemaType
    }
}

fun MotebehovStatusWithFormValuesDTO.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovFormValues: MotebehovFormValuesOutputDTO?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expMotebehovFormValues != null) {
        expMotebehovFormValues shouldBe this.motebehovWithFormValues!!.formValues
        expSkjemaType shouldBe this.motebehovWithFormValues!!.skjemaType
    } else {
        expMotebehovFormValues shouldBe this.motebehovWithFormValues
    }
}
