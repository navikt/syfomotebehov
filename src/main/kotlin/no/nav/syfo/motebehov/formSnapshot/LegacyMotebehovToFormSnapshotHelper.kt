package no.nav.syfo.motebehov.formSnapshot

import no.nav.syfo.motebehov.MotebehovInnmelderType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import org.springframework.stereotype.Component

/**
 * Used for creating a FormSnapshot from a stored "legacy" motebehov.
 */
@Component
class LegacyMotebehovToFormSnapshotHelper {

    private val legacyFormsSemanticVersion = "0.1.0"

    enum class MotebehovLegacyFormLabel(val label: String) {
        SVAR_ARBEIDSGIVER_HAR_BEHOV_FIELD("Har dere behov for et møte med NAV?"),
        SVAR_ARBEIDSTAKER_HAR_BEHOV_FIELD("Har du behov for et møte med NAV og arbeidsgiveren din?"),
        SVAR_HAR_BEHOV_RADIO_OPTION_YES("Ja, jeg mener det er behov for et møte"),
        SVAR_HAR_BEHOV_RADIO_OPTION_NO("Nei, jeg mener det ikke er behov for et møte"),
        MELD_ARBEIDSGIVER_ONSKER_MOTE_CHECKBOX("Jeg ønsker et møte med NAV og den ansatte"),
        MELD_ARBEIDSTAKER_ONSKER_MOTE_CHECKBOX("Jeg ønsker et møte med NAV og arbeidsgiveren min."),
        MELD_ARBEIDSGIVER_ONSKER_SYKMELDER_DELTAR_CHECKBOX(
            "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet."
        ),
        MELD_ARBEIDSTAKER_ONSKER_SYKMELDER_DELTAR_CHECKBOX(
            "Jeg ønsker at den som sykmelder meg, også skal delta i møtet."
        ),
        BEGRUNNELSE_TEXT_FIELD("Begrunnelse")
    }

    private val formSnapshotOptionIds = mapOf(
        "svarHarBehovRadioOptionYes" to "ja",
        "svarHarBehovRadioOptionNo" to "nei"
    )

    data class ExtractedFromLegacyForklaring(
        val actualBegrunnelse: String,
        val onskerSykmelderDeltar: Boolean
    )

    /**
     * Creates a FormSnapshot from legacy motebehov form field values harMotebehov and forklaring.
     * The returned FormSnapshot matches what the "legacy" motebehov forms looked like, which was before an update to
     * the frontend that makes the forms contain more fields, and that makes the frontend submit a FormSnapshot instead
     * of values for specific fields.
     */
    fun createFormSnapshotFromLegacyMotebehovValues(
        harMotebehov: Boolean,
        forklaring: String?,
        skjemaType: MotebehovSkjemaType,
        motebehovInnmelderType: MotebehovInnmelderType
    ): FormSnapshot {
        val fieldSnapshots = mutableListOf<FieldSnapshot>()

        if (skjemaType == MotebehovSkjemaType.SVAR_BEHOV) {
            fieldSnapshots.add(
                createLegacySvarBehovRadioGroupField(
                    harMotebehov,
                    motebehovInnmelderType
                )
            )
        } else if (skjemaType == MotebehovSkjemaType.MELD_BEHOV) {
            fieldSnapshots.add(
                createLegacyMeldOnskerMoteCheckboxField(
                    motebehovInnmelderType
                )
            )
        }

        val (actualBegrunnelse, onskerAtSykmelderDeltar) =
            extractActualUserBegrunnelseAndOnskerSykmelderDeltarFromLegacyForklaring(forklaring)

        // It was only the MELD_BEHOV form that had the "onskerSykmelderDeltar" checkbox.
        if (skjemaType == MotebehovSkjemaType.MELD_BEHOV || onskerAtSykmelderDeltar) {
            fieldSnapshots.add(
                createLegacyOnskerSykmelderDeltarCheckboxField(
                    onskerAtSykmelderDeltar,
                    motebehovInnmelderType
                )
            )
        }

        fieldSnapshots.add(createLegacyBegrunnelseTextField(actualBegrunnelse, harMotebehov, skjemaType))

        val formIdentifier = when (motebehovInnmelderType) {
            MotebehovInnmelderType.ARBEIDSGIVER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> FORM_IDENTIFIER_ARBEIDSGIVER_SVAR
                    MotebehovSkjemaType.MELD_BEHOV -> FORM_IDENTIFIER_ARBEIDSGIVER_MELD
                }

            MotebehovInnmelderType.ARBEIDSTAKER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> FORM_IDENTIFIER_ARBEIDSTAKER_SVAR
                    MotebehovSkjemaType.MELD_BEHOV -> FORM_IDENTIFIER_ARBEIDSTAKER_MELD
                }
        }

        return FormSnapshot(formIdentifier, legacyFormsSemanticVersion, fieldSnapshots)
    }

    private fun createLegacyBegrunnelseTextField(
        begrunnelseTextValue: String,
        harMotebehov: Boolean,
        skjemaType: MotebehovSkjemaType?,
    ): TextFieldSnapshot = TextFieldSnapshot(
        fieldId = BEGRUNNELSE_TEXT_FIELD_ID,
        fieldLabel = MotebehovLegacyFormLabel.BEGRUNNELSE_TEXT_FIELD.label,
        null,
        value = begrunnelseTextValue,
        wasRequired = skjemaType == MotebehovSkjemaType.SVAR_BEHOV && !harMotebehov
    )

    private fun createLegacySvarBehovRadioGroupField(
        harMotebehov: Boolean,
        motebehovInnmelderType: MotebehovInnmelderType
    ): RadioGroupFieldSnapshot {
        val optionIdYes = formSnapshotOptionIds["svarHarBehovRadioOptionYes"]!!
        val optionIdNo = formSnapshotOptionIds["svarHarBehovRadioOptionNo"]!!

        val optionLabelYes = MotebehovLegacyFormLabel.SVAR_HAR_BEHOV_RADIO_OPTION_YES.label
        val optionLabelNo = MotebehovLegacyFormLabel.SVAR_HAR_BEHOV_RADIO_OPTION_NO.label

        val selectedOptionId = if (harMotebehov) optionIdYes else optionIdNo
        val selectedOptionLabel = if (harMotebehov) optionLabelYes else optionLabelNo

        return RadioGroupFieldSnapshot(
            fieldId = SVAR_HAR_BEHOV_RADIO_GROUP_FIELD_ID,
            fieldLabel = motebehovInnmelderType.let {
                when (it) {
                    MotebehovInnmelderType.ARBEIDSGIVER ->
                        MotebehovLegacyFormLabel.SVAR_ARBEIDSGIVER_HAR_BEHOV_FIELD.label
                    MotebehovInnmelderType.ARBEIDSTAKER ->
                        MotebehovLegacyFormLabel.SVAR_ARBEIDSTAKER_HAR_BEHOV_FIELD.label
                }
            },
            null,
            selectedOptionId,
            selectedOptionLabel,
            options = listOf(
                FormSnapshotFieldOption(
                    optionId = optionIdYes,
                    optionLabel = optionLabelYes,
                    wasSelected = harMotebehov
                ),
                FormSnapshotFieldOption(
                    optionId = optionIdNo,
                    optionLabel = optionLabelNo,
                    wasSelected = !harMotebehov
                )
            )
        )
    }

    private fun createLegacyMeldOnskerMoteCheckboxField(
        motebehovInnmelderType: MotebehovInnmelderType
    ): SingleCheckboxFieldSnapshot = SingleCheckboxFieldSnapshot(
        fieldId = MELD_HAR_BEHOV_LEGACY_CHECKBOX_FIELD_ID,
        fieldLabel = motebehovInnmelderType.let {
            when (it) {
                MotebehovInnmelderType.ARBEIDSGIVER ->
                    MotebehovLegacyFormLabel.MELD_ARBEIDSGIVER_ONSKER_MOTE_CHECKBOX.label
                MotebehovInnmelderType.ARBEIDSTAKER ->
                    MotebehovLegacyFormLabel.MELD_ARBEIDSTAKER_ONSKER_MOTE_CHECKBOX.label
            }
        },
        null,
        value = true,
    )

    private fun createLegacyOnskerSykmelderDeltarCheckboxField(
        onskerSykmelderDeltar: Boolean,
        motebehovInnmelderType: MotebehovInnmelderType,
    ): SingleCheckboxFieldSnapshot = SingleCheckboxFieldSnapshot(
        fieldId = ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID,
        fieldLabel = motebehovInnmelderType.let {
            when (it) {
                MotebehovInnmelderType.ARBEIDSGIVER ->
                    MotebehovLegacyFormLabel.MELD_ARBEIDSGIVER_ONSKER_SYKMELDER_DELTAR_CHECKBOX.label

                MotebehovInnmelderType.ARBEIDSTAKER ->
                    MotebehovLegacyFormLabel.MELD_ARBEIDSTAKER_ONSKER_SYKMELDER_DELTAR_CHECKBOX.label
            }
        },
        null,
        value = onskerSykmelderDeltar,
    )

    // When a user checked the checkbox for onskerSykmelderDeltar in the legacy form, the text in the forklaring field
    // submitted from the frontend would contain the label text for that checkbox concatenated with the text value of
    // the begrunnelse text field.
    private fun extractActualUserBegrunnelseAndOnskerSykmelderDeltarFromLegacyForklaring(
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
}
