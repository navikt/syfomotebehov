package no.nav.syfo.motebehov.formSnapshot

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType

class ConvertLegacyMotebehovSvarFieldsHelperTest : DescribeSpec({

    val legacyMotebehovToFormSnapshotHelper = LegacyMotebehovToFormSnapshotHelper()

    describe("ConvertLegacyMotebehovSvarFieldsHelper") {
        it(
            "should create a form snapshot matching the legacy version of the form 'motebehov-arbeidstaker-svar' " +
                "form filled in with ja and certain begrunnelse"
        ) {
            val harMotebehov = true
            val begrunnelse = "Jeg ønkser å snakke om bedre tilrettelegging"
            val skjemaType = MotebehovSkjemaType.SVAR_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSTAKER

            val formSnapshot = legacyMotebehovToFormSnapshotHelper.createFormSnapshotFromLegacyMotebehovValues(
                harMotebehov,
                begrunnelse,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidstaker-svar"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                RadioGroupFieldSnapshot(
                    "harBehovRadioGroup",
                    "Har du behov for et møte med NAV og arbeidsgiveren din?",
                    null,
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
                    null,
                    begrunnelse,
                    false
                )
            )

            formSnapshot.fieldValues shouldBe
                mapOf(
                    "harBehovRadioGroup" to "ja",
                    "begrunnelseText" to begrunnelse
                )
        }

        it(
            "should create a form snapshot matching the legacy version of the 'motebehov-arbeidsgiver-svar' " +
                "form filled in with nei and certain begrunnelse"
        ) {
            val harMotebehov = false
            val begrunnelse = "Vi har avtalt det vi trenger"
            val skjemaType = MotebehovSkjemaType.SVAR_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSGIVER

            val formSnapshot = legacyMotebehovToFormSnapshotHelper.createFormSnapshotFromLegacyMotebehovValues(
                harMotebehov,
                begrunnelse,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidsgiver-svar"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                RadioGroupFieldSnapshot(
                    "harBehovRadioGroup",
                    "Har dere behov for et møte med NAV?",
                    null,
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
                    null,
                    begrunnelse,
                    true
                ),
            )

            formSnapshot.fieldValues shouldBe
                mapOf(
                    "harBehovRadioGroup" to "nei",
                    "begrunnelseText" to begrunnelse
                )
        }

        it(
            "should create a form snapshot matching the legacy version of the 'motebehov-arbeidstaker-meld' " +
                "form filled in with certain begrunnelse"
        ) {
            val harMotebehov = true
            val begrunnelse = "Dette er tekst i begrunnelsesfeltet"
            val skjemaType = MotebehovSkjemaType.MELD_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSTAKER

            val formSnapshot = legacyMotebehovToFormSnapshotHelper.createFormSnapshotFromLegacyMotebehovValues(
                harMotebehov,
                begrunnelse,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidstaker-meld"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                SingleCheckboxFieldSnapshot(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og arbeidsgiveren min.",
                    null,
                    true,
                ),
                SingleCheckboxFieldSnapshot(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder meg, også skal delta i møtet.",
                    null,
                    false,
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    null,
                    begrunnelse,
                    false
                ),
            )

            formSnapshot.fieldValues shouldBe
                mapOf(
                    "harBehovCheckbox" to true,
                    "onskerSykmelderDeltarCheckbox" to false,
                    "begrunnelseText" to begrunnelse
                )
        }

        it(
            "should create a form snapshot matching the legacy version of the 'motebehov-arbeidsgiver-meld' " +
                "form filled in with certain begrunnelse"
        ) {
            val harMotebehov = true
            val begrunnelse = "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet (valgfri). " +
                "Vi trenger å ha et møte med NAV."
            val skjemaType = MotebehovSkjemaType.MELD_BEHOV
            val motebehovInnmelderType = MotebehovInnmelderType.ARBEIDSGIVER

            val formSnapshot = legacyMotebehovToFormSnapshotHelper.createFormSnapshotFromLegacyMotebehovValues(
                harMotebehov,
                begrunnelse,
                skjemaType,
                motebehovInnmelderType
            )

            formSnapshot.formIdentifier shouldBe "motebehov-arbeidsgiver-meld"
            formSnapshot.formSemanticVersion shouldBe "0.1.0"

            formSnapshot.fieldSnapshots shouldContainExactly listOf(
                SingleCheckboxFieldSnapshot(
                    "harBehovCheckbox",
                    "Jeg ønsker et møte med NAV og den ansatte",
                    null,
                    true,
                ),
                SingleCheckboxFieldSnapshot(
                    "onskerSykmelderDeltarCheckbox",
                    "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet.",
                    null,
                    true,
                ),
                TextFieldSnapshot(
                    "begrunnelseText",
                    "Begrunnelse",
                    null,
                    "Vi trenger å ha et møte med NAV.",
                    false
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
