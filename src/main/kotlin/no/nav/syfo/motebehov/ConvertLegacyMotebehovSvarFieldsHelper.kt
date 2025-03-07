package no.nav.syfo.motebehov

import no.nav.syfo.motebehov.formFillout.*
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import org.springframework.stereotype.Component

enum class MotebehovInnmelderType {
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

    enum class MotebehovLegacyLabel(val label: String) {
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
        harMotebehov: Boolean,
        skjemaType: MotebehovSkjemaType?,
    ): FilloutTextField = FilloutTextField(
        fieldID = BEGRUNNELSE_TEXT_FIELD_ID,
        fieldLabel = MotebehovLegacyLabel.BEGRUNNELSE_TEXT_FIELD.label,
        textValue = begrunnelseTextValue,
        wasOptional = skjemaType == MotebehovSkjemaType.MELD_BEHOV || harMotebehov
    )

    fun createLegacySvarBehovRadioGroupField(
        harMotebehov: Boolean,
        motebehovInnmelderType: MotebehovInnmelderType
    ): FilloutRadioGroupField {
        val optionIdYes = formFilloutOptionIds["svarHarBehovRadioOptionYes"]!!
        val optionIdNo = formFilloutOptionIds["svarHarBehovRadioOptionNo"]!!

        val optionLabelYes = MotebehovLegacyLabel.SVAR_HAR_BEHOV_RADIO_OPTION_YES.label
        val optionLabelNo = MotebehovLegacyLabel.SVAR_HAR_BEHOV_RADIO_OPTION_NO.label

        val selectedOptionId = if (harMotebehov) optionIdYes else optionIdNo
        val selectedOptionLabel = if (harMotebehov) optionLabelYes else optionLabelNo

        return FilloutRadioGroupField(
            fieldID = SVAR_HAR_BEHOV_RADIO_GROUP_FIELD_ID,
            fieldLabel = motebehovInnmelderType.let {
                when (it) {
                    MotebehovInnmelderType.ARBEIDSGIVER ->
                        MotebehovLegacyLabel.SVAR_ARBEIDSGIVER_HAR_BEHOV_FIELD.label
                    MotebehovInnmelderType.ARBEIDSTAKER ->
                        MotebehovLegacyLabel.SVAR_ARBEIDSTAKER_HAR_BEHOV_FIELD.label
                }
            },
            selectedOptionId,
            selectedOptionLabel,
            options = listOf(
                FormFilloutFieldOption(
                    optionId = optionIdYes,
                    optionLabel = optionLabelYes,
                    wasSelected = harMotebehov
                ),
                FormFilloutFieldOption(
                    optionId = optionIdNo,
                    optionLabel = optionLabelNo,
                    wasSelected = !harMotebehov
                )
            )
        )
    }

    fun createLegacyMeldOnskerMoteCheckboxField(
        motebehovInnmelderType: MotebehovInnmelderType
    ): FilloutCheckboxField = FilloutCheckboxField(
        fieldID = MELD_HAR_BEHOV_LEGACY_CHECKBOX_FIELD_ID,
        fieldLabel = motebehovInnmelderType.let {
            when (it) {
                MotebehovInnmelderType.ARBEIDSGIVER ->
                    MotebehovLegacyLabel.MELD_ARBEIDSGIVER_ONSKER_MOTE_CHECKBOX.label
                MotebehovInnmelderType.ARBEIDSTAKER ->
                    MotebehovLegacyLabel.MELD_ARBEIDSTAKER_ONSKER_MOTE_CHECKBOX.label
            }
        },
        wasChecked = true,
    )

    fun createLegacyOnskerSykmelderDeltarCheckboxField(
        onskerSykmelderDeltar: Boolean,
        motebehovInnmelderType: MotebehovInnmelderType,
    ): FilloutCheckboxField = FilloutCheckboxField(
        fieldID = ONSKER_SYKMELDER_DELTAR_CHECKBOX_FIELD_ID,
        fieldLabel = motebehovInnmelderType.let {
            when (it) {
                MotebehovInnmelderType.ARBEIDSGIVER ->
                    MotebehovLegacyLabel.MELD_ARBEIDSGIVER_ONSKER_SYKMELDER_DELTAR_CHECKBOX.label

                MotebehovInnmelderType.ARBEIDSTAKER ->
                    MotebehovLegacyLabel.MELD_ARBEIDSTAKER_ONSKER_SYKMELDER_DELTAR_CHECKBOX.label
            }
        },
        wasChecked = onskerSykmelderDeltar,
    )

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
        motebehovInnmelderType: MotebehovInnmelderType
    ): FormFillout {
        val formFilloutFields = mutableListOf<FilloutField>()

        if (skjemaType == MotebehovSkjemaType.SVAR_BEHOV) {
            formFilloutFields.add(
                createLegacySvarBehovRadioGroupField(
                    harMotebehov,
                    motebehovInnmelderType
                )
            )
        } else if (skjemaType == MotebehovSkjemaType.MELD_BEHOV) {
            formFilloutFields.add(
                createLegacyMeldOnskerMoteCheckboxField(
                    motebehovInnmelderType
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
                    motebehovInnmelderType
                )
            )
        }

        formFilloutFields.add(createLegacyBegrunnelseTextField(actualBegrunnelse, harMotebehov, skjemaType))

        val formIdentifier = when (motebehovInnmelderType) {
            MotebehovInnmelderType.ARBEIDSGIVER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> formIdentifierArbeidsgiverSvarBehov
                    MotebehovSkjemaType.MELD_BEHOV -> formIdentifierArbeidsgiverMeldBehov
                    else -> formIdentifierArbeidsgiverUnknownSvarMeldBehov
                }

            MotebehovInnmelderType.ARBEIDSTAKER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> formIdentifierArbeidstakerSvarBehov
                    MotebehovSkjemaType.MELD_BEHOV -> formIdentifierArbeidstakerMeldBehov
                    else -> formIdentifierArbeidstakerUnknownSvarMeldBehov
                }
        }

        return FormFillout(formIdentifier, legacyFormsSemanticVersion, formFilloutFields)
    }
}
