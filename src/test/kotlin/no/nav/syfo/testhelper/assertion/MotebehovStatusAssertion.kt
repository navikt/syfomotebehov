package no.nav.syfo.testhelper.assertion

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.motebehov.MotebehovFormValuesOutputDTO
import no.nav.syfo.motebehov.MotebehovSvarLegacyDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithFormValuesDTO
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatusWithLegacyMotebehovDTO

fun MotebehovStatusWithLegacyMotebehovDTO.assertMotebehovStatus(
    expVisMotebehov: Boolean,
    expSkjemaType: MotebehovSkjemaType? = null,
    expLegacyMotebehovSvar: MotebehovSvarLegacyDTO? = null
) {
    this.visMotebehov shouldBe expVisMotebehov

    if (expSkjemaType != null) {
        this.skjemaType shouldBe expSkjemaType
    }

    if (expLegacyMotebehovSvar != null) {
        this.motebehov shouldNotBe null
        this.motebehov!!.motebehovSvar shouldBe expLegacyMotebehovSvar
        this.motebehov!!.skjemaType shouldBe expSkjemaType
    } else {
        this.motebehov shouldBe null
    }
}

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
