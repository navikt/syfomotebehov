package no.nav.syfo.motebehov

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType

class ConvertLegacyMotebehovSvarFieldsHelperTest : DescribeSpec({

    val convertLegacyMotebehovSvarFieldsHelper = ConvertLegacyMotebehovSvarFieldsHelper()

    describe("ConvertLegacyMotebehovSvarFieldsHelper") {
        it("should convert legacy motebehovSvar from arbeidstaker of type svar ja correctly") {
            val harMotebehov = true
            val forklaring = "Jeg ønkser å snakke om bedre tilrettelegging"
            val skjemaType = MotebehovSkjemaType.SVAR_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSTAKER

            val formFillout = convertLegacyMotebehovSvarFieldsHelper.convertLegacyMotebehovSvarToFormFillout(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            "motebehov-arbeidstaker-svar" shouldBe formFillout.formIdentifier
            formFillout.formSemanticVersion shouldBe "0.1.0"

            formFillout.filloutFieldsList shouldContainExactly listOf(
                FilloutRadioGroupField(
                    "harBehovRadioGroup",
                    "Har du behov for et møte med NAV og arbeidsgiveren din?",
                    "ja",
                    "Ja, jeg mener det er behov for et møte",
                    listOf(
                        FormFilloutFieldOption("ja", "Ja, jeg mener det er behov for et møte", true),
                        FormFilloutFieldOption("nei", "Nei, jeg mener det ikke er behov for et møte")
                    )
                ),
                FilloutTextField(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    true
                )
            )

            formFillout.fieldValues shouldBe
                mapOf(
                    "harBehovRadioGroup" to "ja",
                    "begrunnelseText" to forklaring
                )
        }

        it("should convert legacy motebehovSvar from arbeidsgiver of type svar nei correctly") {
            val harMotebehov = false
            val forklaring = "Vi trenger et møte"
            val skjemaType = MotebehovSkjemaType.SVAR_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSGIVER

            val formFillout = convertLegacyMotebehovSvarFieldsHelper.convertLegacyMotebehovSvarToFormFillout(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formFillout.formIdentifier shouldBe "motebehov-arbeidsgiver-svar"
            formFillout.formSemanticVersion shouldBe "0.1.0"

            formFillout.filloutFieldsList shouldContainExactly listOf(
                FilloutRadioGroupField(
                    "harBehovRadioGroup",
                    "Har dere behov for et møte med NAV?",
                    "nei",
                    "Nei, jeg mener det ikke er behov for et møte",
                    listOf(
                        FormFilloutFieldOption("ja", "Ja, jeg mener det er behov for et møte"),
                        FormFilloutFieldOption("nei", "Nei, jeg mener det ikke er behov for et møte", true)
                    )
                ),
                FilloutTextField(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    false
                ),
            )

            formFillout.fieldValues shouldBe
                mapOf(
                    "harBehovRadioGroup" to "nei",
                    "begrunnelseText" to forklaring
                )
        }

        it("should convert legacy motebehovSvar from arbeidstaker of type meld correctly") {
            val harMotebehov = true
            val forklaring = "Dette er tekst i begrunnelsesfeltet"
            val skjemaType = MotebehovSkjemaType.MELD_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSTAKER

            val formFillout = convertLegacyMotebehovSvarFieldsHelper.convertLegacyMotebehovSvarToFormFillout(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formFillout.formIdentifier shouldBe "motebehov-arbeidstaker-meld"
            formFillout.formSemanticVersion shouldBe "0.1.0"

            formFillout.filloutFieldsList shouldContainExactly listOf(
                FilloutCheckboxField(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og arbeidsgiveren min.",
                    true,
                ),
                FilloutCheckboxField(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder meg, også skal delta i møtet.",
                    false,
                ),
                FilloutTextField(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    true
                ),
            )

            formFillout.fieldValues shouldBe
                mapOf(
                    "harBehovCheckbox" to true,
                    "onskerSykmelderDeltarCheckbox" to false,
                    "begrunnelseText" to forklaring
                )
        }

        it("should convert legacy motebehovSvar from arbeidsgiver of type meld and onsker sykmelder correctly") {
            val harMotebehov = true
            val forklaring = "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet (valgfri). " +
                "Vi trenger å ha et møte med NAV."
            val skjemaType = MotebehovSkjemaType.MELD_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSGIVER

            val formFillout = convertLegacyMotebehovSvarFieldsHelper.convertLegacyMotebehovSvarToFormFillout(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formFillout.formIdentifier shouldBe "motebehov-arbeidsgiver-meld"
            formFillout.formSemanticVersion shouldBe "0.1.0"

            formFillout.filloutFieldsList shouldContainExactly listOf(
                FilloutCheckboxField(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og den ansatte",
                    true,
                ),
                FilloutCheckboxField(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet.",
                    true,
                ),
                FilloutTextField(
                    "begrunnelseText",
                    "Begrunnelse",
                    "Vi trenger å ha et møte med NAV.",
                    true
                ),
            )

            formFillout.fieldValues shouldBe mapOf(
                "harBehovCheckbox" to true,
                "onskerSykmelderDeltarCheckbox" to true,
                "begrunnelseText" to "Vi trenger å ha et møte med NAV."
            )
        }
    }
})
