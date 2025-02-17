package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import org.springframework.stereotype.Component

enum class MotebehovCreatorRole {
    ARBEIDSGIVER,
    ARBEIDSTAKER,
}

@Component
class ConvertLegacyMotebehovSvarFieldsHelper {
    private val formIdentifierArbeidsgiverSvarBehov = "motebehov-arbeidsgiver-svar"
    private val formIdentifierArbeidsgiverMeldBehov = "motebehov-arbeidsgiver-meld"
    private val formIdentifierArbeidsgiverUnknownSvarMeldBehov = "motebehov-arbeidsgiver-unknown"
    private val formIdentifierArbeidstakerSvarBehov = "motebehov-arbeidstaker-svar"
    private val formIdentifierArbeidstakerMeldBehov = "motebehov-arbeidstaker-meld"
    private val formIdentifierArbeidstakerUnknownSvarMeldBehov = "motebehov-arbeidstaker-unknown"

    private val legacyFormsSemanticVersion = "0.1.0"

    private val motebehovFieldIds = mapOf(
        "svarHarBehovRadioGroupField" to "harBehovRadioGroupField",
        "meldOnskerMoteLegacyCheckboxField" to "onskerMoteCheckboxField",
        "onskerSykmelderDeltarCheckboxField" to "onskerSykmelderDeltarCheckboxField",
        "begrunnelseTextField" to "begrunnelseTextField",
    )

    private val motebehovLegacyLabels = mapOf(
        "svarArbeidsgiverHarBehovField" to "Har dere behov for et møte med NAV?",
        "svarArbeidstakerHarBehovField" to "Har du behov for et møte med NAV og arbeidsgiveren din?",
        "svarHarBehovRadioOptionYes" to "Ja, jeg mener det er behov for et møte",
        "svarHarBehovRadioOptionNo" to "Nei, jeg mener det ikke er behov for et møte",
        "meldArbeidsgiverOnskerMoteCheckbox" to "Jeg ønsker et møte med NAV og den ansatte",
        "meldArbeidstakerOnskerMoteCheckbox" to "Jeg ønsker et møte med NAV og arbeidsgiveren min.",
        "meldArbeidsgiverOnskerSykmelderDeltarCheckbox" to
            "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet.",
        "meldArbeidstakerOnskerSykmelderDeltarCheckbox" to
            "Jeg ønsker at den som sykmelder meg, også skal delta i møtet.",
        "begrunnelseTextField" to "Begrunnelse"
    )

    private val formFilloutOptionIds = mapOf(
        "svarHarBehovRadioOptionYes" to "ja",
        "svarHarBehovRadioOptionNo" to "nei"
    )

    data class ExtractedFromLegacyForklaring(
        val actualBegrunnelse: String,
        val onskerSykmelderDeltar: Boolean
    )

    fun createLegacyBegrunnelseTextField(
        begrunnelseTextValue: String,
        harMotebehov: Boolean
    ): FilloutTextField {
        return FilloutTextField(
            fieldID = motebehovFieldIds["begrunnelseTextField"] ?: "",
            fieldLabel = motebehovLegacyLabels["begrunnelseTextField"] ?: "",
            textValue = begrunnelseTextValue,
            isOptional = harMotebehov
        )
    }

    fun createLegacySvarBehovRadioGroupField(
        harMotebehov: Boolean,
        motebehovCreatorRole: MotebehovCreatorRole
    ): FilloutRadioGroupField {
        val optionIdYes = formFilloutOptionIds["svarHarBehovRadioOptionYes"]!!
        val optionIdNo = formFilloutOptionIds["svarHarBehovRadioOptionNo"]!!

        val optionLabelYes = motebehovLegacyLabels["svarHarBehovRadioOptionYes"]!!
        val optionLabelNo = motebehovLegacyLabels["svarHarBehovRadioOptionNo"]!!

        val selectedOptionId = if (harMotebehov) optionIdYes else optionIdNo
        val selectedOptionLabel = if (harMotebehov) optionLabelYes else optionLabelNo

        return FilloutRadioGroupField(
            fieldID = motebehovFieldIds["svarHarBehovRadioGroupField"]!!,
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        motebehovLegacyLabels["svarArbeidsgiverHarBehovField"]!!

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        motebehovLegacyLabels["svarArbeidstakerHarBehovField"]!!
                }
            },
            selectedOptionId,
            selectedOptionLabel,
            options = listOf(
                FormFilloutFieldOption(
                    optionId = optionIdYes,
                    optionLabel = optionLabelYes,
                    isSelected = harMotebehov
                ),
                FormFilloutFieldOption(
                    optionId = optionIdNo,
                    optionLabel = optionLabelNo,
                    isSelected = !harMotebehov
                )
            )
        )
    }

    fun createLegacyMeldOnskerMoteCheckboxField(
        motebehovCreatorRole: MotebehovCreatorRole
    ): FilloutCheckboxField {
        return FilloutCheckboxField(
            fieldID = motebehovFieldIds["meldOnskerMoteLegacyCheckboxField"] ?: "",
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        motebehovLegacyLabels["meldArbeidsgiverOnskerMoteCheckbox"] ?: ""

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        motebehovLegacyLabels["meldArbeidstakerOnskerMoteCheckbox"] ?: ""
                }
            },
            isChecked = true,
        )
    }

    fun createLegacyOnskerSykmelderDeltarCheckboxField(
        onskerSykmelderDeltar: Boolean,
        motebehovCreatorRole: MotebehovCreatorRole,
    ): FilloutCheckboxField {
        return FilloutCheckboxField(
            fieldID = motebehovFieldIds["onskerSykmelderDeltarCheckboxField"] ?: "",
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        motebehovLegacyLabels["meldArbeidsgiverOnskerSykmelderDeltarCheckbox"] ?: ""

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        motebehovLegacyLabels["meldArbeidstakerOnskerSykmelderDeltarCheckbox"] ?: ""
                }
            },
            isChecked = onskerSykmelderDeltar,
        )
    }

    // When a user checked the checkbox for onskerSykmelderDeltar in the legacy form, the text in the forklaring field
    // submitted from the frontend would contain the label text for that checkbox concatenated with the text value of
    // the begrunnelse text field.
    fun extractActualUserBegrunnelseAndOnskerSykmelderDeltarFromLegacyForklaring(
        legacyForklaring: String?
    ): ExtractedFromLegacyForklaring {
        if (legacyForklaring == null) return ExtractedFromLegacyForklaring("", false)

        var onskerSykmelderDeltar = false

        if (legacyForklaring.contains(
                "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet"
            ) || legacyForklaring.contains(
                "Jeg ønsker at den som sykmelder meg, også skal delta i møtet"
            )
        ) {
            onskerSykmelderDeltar = true
        }

        var actualBegrunnelse = legacyForklaring.replace(
            "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet (valgfri).",
            ""
        )
        actualBegrunnelse = actualBegrunnelse.replace(
            "Jeg ønsker at den som sykmelder meg, også skal delta i møtet (valgfri).", ""
        )

        // When the user didn't write anything in the begrunnelse text field, "undefined" would be appended to the
        // forklaring value, at least in some cases. We remove it here.
        actualBegrunnelse = actualBegrunnelse.replace("undefined", "")

        actualBegrunnelse = actualBegrunnelse.trim()

        return ExtractedFromLegacyForklaring(actualBegrunnelse, onskerSykmelderDeltar)
    }

    /**
     * Converts a "legacy motebehovSvar" with fields harMotebehov and forklaring to a FormFillout.
     * The returned FormFillout matches what the forms look like in production at the time of writing, which is before
     * an update to the frontend that will make the forms contain more fields, and that will make the frontend submit
     * a FormFillout instead of individual hard-coded field values.
     */
    fun convertLegacyMotebehovSvarToFormFillout(
        harMotebehov: Boolean,
        forklaring: String?,
        skjemaType: MotebehovSkjemaType?,
        motebehovCreatorRole: MotebehovCreatorRole
    ): FormFillout {
        val formFilloutFields = mutableListOf<FilloutField>()

        if (skjemaType == MotebehovSkjemaType.SVAR_BEHOV) {
            formFilloutFields.add(
                createLegacySvarBehovRadioGroupField(
                    harMotebehov,
                    motebehovCreatorRole
                )
            )
        } else if (skjemaType == MotebehovSkjemaType.MELD_BEHOV) {
            formFilloutFields.add(
                createLegacyMeldOnskerMoteCheckboxField(
                    motebehovCreatorRole
                )
            )
        }

        val (actualBegrunnelse, onskerAtSykmelderDeltar) =
            extractActualUserBegrunnelseAndOnskerSykmelderDeltarFromLegacyForklaring(forklaring)

        // It was only the MELD_BEHOV form that had the "onskerSykmelderDeltar" checkbox.
        if (skjemaType == MotebehovSkjemaType.MELD_BEHOV || onskerAtSykmelderDeltar) {
            formFilloutFields.add(
                createLegacyOnskerSykmelderDeltarCheckboxField(
                    onskerAtSykmelderDeltar,
                    motebehovCreatorRole
                )
            )
        }

        formFilloutFields.add(createLegacyBegrunnelseTextField(actualBegrunnelse, harMotebehov))

        val formIdentifier = when (motebehovCreatorRole) {
            MotebehovCreatorRole.ARBEIDSGIVER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> formIdentifierArbeidsgiverSvarBehov
                    MotebehovSkjemaType.MELD_BEHOV -> formIdentifierArbeidsgiverMeldBehov
                    else -> formIdentifierArbeidsgiverUnknownSvarMeldBehov
                }

            MotebehovCreatorRole.ARBEIDSTAKER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> formIdentifierArbeidstakerSvarBehov
                    MotebehovSkjemaType.MELD_BEHOV -> formIdentifierArbeidstakerMeldBehov
                    else -> formIdentifierArbeidstakerUnknownSvarMeldBehov
                }
        }

        return FormFillout(formIdentifier, legacyFormsSemanticVersion, formFilloutFields)
    }
}
