package no.nav.syfo.motebehov

import org.springframework.stereotype.Component
import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType

enum class MotebehovCreatorRole {
    ARBEIDSGIVER,
    ARBEIDSTAKER,
}

private val formFilloutFieldIds = mapOf(
    "svarHarBehovRadioGroupField" to "harBehovRadioGroupField",
    "meldOnskerMoteLegacyCheckboxField" to "onskerMoteCheckboxField",
    "onskerSykmelderDeltarCheckboxField" to "onskerSykmelderDeltarCheckboxField",
    "begrunnelseTextField" to "begrunnelseTextField",
)

private val legacyFormLabels = mapOf(
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

@Component
class ConvertingLegacyMotebehovSvarToFormFilloutHelper {
    fun createLegacyBegrunnelseTextField(
        begrunnelseTextValue: String,
        harMotebehov: Boolean
    ): FilloutTextField {
        return FilloutTextField(
            fieldID = formFilloutFieldIds["begrunnelseTextField"] ?: "",
            fieldLabel = legacyFormLabels["begrunnelseTextField"] ?: "",
            textValue = begrunnelseTextValue,
            isOptional = harMotebehov
        )
    }

    fun createLegacySvarBehovRadioGroupField(
        harMotebehov: Boolean,
        motebehovCreatorRole: MotebehovCreatorRole
    ): FilloutRadioGroupField {
        return FilloutRadioGroupField(
            fieldID = formFilloutFieldIds["svarHarBehovRadioGroupField"] ?: "",
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        legacyFormLabels["svarArbeidsgiverHarBehovField"] ?: ""

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        legacyFormLabels["svarArbeidstakerHarBehovField"] ?: ""
                }
            },
            selectedOptionId = if (harMotebehov)
                formFilloutOptionIds["svarHarBehovRadioOptionYes"] ?: ""
            else
                formFilloutOptionIds["svarHarBehovRadioOptionNo"] ?: "",
            selectedOptionLabel = if (harMotebehov)
                legacyFormLabels["svarHarBehovOptionYes"] ?: ""
            else
                legacyFormLabels["svarHarBehovOptionNo"] ?: "",
            options = listOf(
                FormFilloutFieldOption(
                    optionId = formFilloutOptionIds["svarHarBehovRadioOptionYes"] ?: "",
                    optionLabel = legacyFormLabels["svarHarBehovOptionYes"] ?: "",
                    isSelected = harMotebehov
                ), FormFilloutFieldOption(
                    optionId = formFilloutOptionIds["svarHarBehovRadioOptionNo"] ?: "",
                    optionLabel = legacyFormLabels["svarHarBehovOptionNo"] ?: "",
                    isSelected = !harMotebehov
                )
            )
        )
    }

    fun createLegacyMeldOnskerMoteCheckboxField(
        motebehovCreatorRole: MotebehovCreatorRole
    ): FilloutCheckboxField {
        return FilloutCheckboxField(
            fieldID = formFilloutFieldIds["meldOnskerMoteLegacyCheckboxField"] ?: "",
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        legacyFormLabels["meldArbeidsgiverOnskerMoteCheckbox"] ?: ""

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        legacyFormLabels["meldArbeidstakerOnskerMoteCheckbox"] ?: ""
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
            fieldID = formFilloutFieldIds["onskerSykmelderDeltarCheckboxField"] ?: "",
            fieldLabel = motebehovCreatorRole.let {
                when (it) {
                    MotebehovCreatorRole.ARBEIDSGIVER ->
                        legacyFormLabels["meldArbeidsgiverOnskerSykmelderDeltarCheckbox"] ?: ""

                    MotebehovCreatorRole.ARBEIDSTAKER ->
                        legacyFormLabels["meldArbeidstakerOnskerSykmelderDeltarCheckbox"] ?: ""
                }
            },
            isChecked = onskerSykmelderDeltar,
        )
    }

    data class ExtractedFromLegacyForklaring(
        val actualBegrunnelse: String,
        val onskerSykmelderDeltar: Boolean
    )

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
            "Jeg ønsker at den som sykmelder arbeidstakeren, også skal delta i møtet (valgfri).", ""
        )
        actualBegrunnelse = actualBegrunnelse.replace(
            "Jeg ønsker at den som sykmelder meg, også skal delta i møtet (valgfri).", ""
        )
        actualBegrunnelse = actualBegrunnelse.replace("undefined", "")

        actualBegrunnelse = actualBegrunnelse.trim()

        return ExtractedFromLegacyForklaring(actualBegrunnelse, onskerSykmelderDeltar)
    }

    /**
     * Converts a "legacy motebehovSvar" with fields harMotebehov and forklaring to a FormFillout.
     * The returned FormFillout matches what the forms looks like in production at the time of writing, which is before
     * an update to the frontend that will make the forms contain more fields, and that will make the frontend submit
     * a FormFillout instead of individual hard-coded field values.
     */
    fun convertLegacyMotebehovSvarToFormFillout(
        harMotebehov: Boolean,
        begrunnelse: String?,
        skjemaType: MotebehovSkjemaType?,
        motebehovCreatorRole: MotebehovCreatorRole
    ): FormFillout {
        val formFilloutFields = mutableListOf<FilloutField>()

        skjemaType.let {
            when (it) {
                MotebehovSkjemaType.SVAR_BEHOV -> {
                    formFilloutFields.add(
                        createLegacySvarBehovRadioGroupField(
                            harMotebehov,
                            motebehovCreatorRole
                        )
                    )
                }

                MotebehovSkjemaType.MELD_BEHOV -> {
                    formFilloutFields.add(
                        createLegacyMeldOnskerMoteCheckboxField(
                            motebehovCreatorRole
                        )
                    )
                }

                else -> {
                    // Don't add anything. Should not happen. If it does, it's not a big deal, as this part is only
                    // to make the formFillout replicate the "top" of the legacy forms.
                }
            }


        }

        val (actualBegrunnelse, onskerAtSykmelderDeltar) =
            extractActualUserBegrunnelseAndOnskerSykmelderDeltarFromLegacyForklaring(begrunnelse)

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
                    MotebehovSkjemaType.SVAR_BEHOV -> "motebehov-arbeidsgiver-svar"
                    MotebehovSkjemaType.MELD_BEHOV -> "motebehov-arbeidsgiver-meld"
                    else -> "motebehov-arbeidsgiver-unknown"
                }

            MotebehovCreatorRole.ARBEIDSTAKER ->
                when (skjemaType) {
                    MotebehovSkjemaType.SVAR_BEHOV -> "motebehov-arbeidstaker-svar"
                    MotebehovSkjemaType.MELD_BEHOV -> "motebehov-arbeidstaker-meld"
                    else -> "motebehov-arbeidstaker-unknown"
                }
        }

        return FormFillout(formIdentifier,"legacy", formFilloutFields)
    }

}
