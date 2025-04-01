package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.MotebehovFormSubmissionCombinedDTO
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO

fun MotebehovStatus.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType?,
    expMotebehovFormSubmission: MotebehovFormSubmissionCombinedDTO?
) {
    expVisMotebehov shouldBe this.visMotebehov
    expSkjemaType shouldBe this.skjemaType
    if (expMotebehovFormSubmission != null) {
        expMotebehovFormSubmission shouldBe this.motebehov!!.formSubmission
        expSkjemaType shouldBe this.motebehov!!.skjemaType
    } else {
        expMotebehovFormSubmission shouldBe this.motebehov
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
