package no.nav.syfo.motebehov

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.formSnapshot.FormSnapshotFieldOption
import no.nav.syfo.motebehov.formSnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.motebehov.formSnapshot.SingleCheckboxFieldSnapshot
import no.nav.syfo.motebehov.formSnapshot.TextFieldSnapshot
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType

class ConvertLegacyMotebehovSvarFieldsHelperTest : DescribeSpec({

    val createLegacyFormSnapshotHelper = CreateFormSnapshotFromLegacyMotebehovHelper()

    describe("ConvertLegacyMotebehovSvarFieldsHelper") {
        it("should convert legacy motebehovSvar from arbeidstaker of type svar ja correctly") {
            val harMotebehov = true
            val forklaring = "Jeg ønkser å snakke om bedre tilrettelegging"
            val skjemaType = MotebehovSkjemaType.SVAR_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSTAKER

            val formSnapshot = createLegacyFormSnapshotHelper.createFormSnapshotFromLegacyMotebehov(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            "motebehov-arbeidstaker-svar" shouldBe formSnapshot.formIdentifier
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                RadioGroupFieldSnapshot(
                    "harBehovRadioGroup",
                    "Har du behov for et møte med NAV og arbeidsgiveren din?",
                    "ja",
                    "Ja, jeg mener det er behov for et møte",
                    listOf(
                        FormSnapshotFieldOption("ja", "Ja, jeg mener det er behov for et møte", true),
                        FormSnapshotFieldOption("nei", "Nei, jeg mener det ikke er behov for et møte")
                    )
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    true
                )
            )

            formSnapshot.fieldValues shouldBe
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

            val formSnapshot = createLegacyFormSnapshotHelper.createFormSnapshotFromLegacyMotebehov(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidsgiver-svar"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                RadioGroupFieldSnapshot(
                    "harBehovRadioGroup",
                    "Har dere behov for et møte med NAV?",
                    "nei",
                    "Nei, jeg mener det ikke er behov for et møte",
                    listOf(
                        FormSnapshotFieldOption("ja", "Ja, jeg mener det er behov for et møte"),
                        FormSnapshotFieldOption("nei", "Nei, jeg mener det ikke er behov for et møte", true)
                    )
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    false
                ),
            )

            formSnapshot.fieldValues shouldBe
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

            val formSnapshot = createLegacyFormSnapshotHelper.createFormSnapshotFromLegacyMotebehov(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidstaker-meld"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                SingleCheckboxFieldSnapshot(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og arbeidsgiveren min.",
                    true,
                ),
                SingleCheckboxFieldSnapshot(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder meg, også skal delta i møtet.",
                    false,
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    forklaring,
                    true
                ),
            )

            formSnapshot.fieldValues shouldBe
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

            val formSnapshot = createLegacyFormSnapshotHelper.createFormSnapshotFromLegacyMotebehov(
                harMotebehov,
                forklaring,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidsgiver-meld"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                SingleCheckboxFieldSnapshot(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og den ansatte",
                    true,
                ),
                SingleCheckboxFieldSnapshot(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet.",
                    true,
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    "Vi trenger å ha et møte med NAV.",
                    true
                ),
            )

            formSnapshot.fieldValues shouldBe mapOf(
                "harBehovCheckbox" to true,
                "onskerSykmelderDeltarCheckbox" to true,
                "begrunnelseText" to "Vi trenger å ha et møte med NAV."
            )
        }
    }
})
