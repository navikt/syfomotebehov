package no.nav.syfo.motebehov.formFillout

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

// The kdoc comments are written with regards to FormFillout being used in a general context,
// not specifically for the motebehov use case.

/**
 * FormFillout is a data class describing details of some simple form and how it was filled out in a form submission.
 *
 * FormFillout can be used as a DTO for transmitting a form fillout between frontend and backend, and as a serializable
 * storage format. The main use case for the FormFillout format is to support storage and display of form responses for
 * forms where at least some of these conditions apply:
 * - The form will probably change over time (for example with labels changing, or fields being added or removed).
 * - The form responses are mainly submitted in order to be displayed to humans (as opposed to mainly being read
 *   programmatically).
 * - The users viewing the form responses are interested in seeing what the form looked like at the time of submission,
 *   as opposed to what the form looks like currently, i.e. in case of a label change, or in case of a more drastic
 *   change.
 * - The users viewing the form response might be interested in seeing all the options that was available to choose from
 *   for a radio buttons field, as opposed to just the selected option.
 *
 * The contents of a form fillout might be displayed to the "form filling" user themself on a receipt screeen, or to a
 * veileder in Modia.
 *
 * A form fillout is meant to describe what a form looked like at the time of submission, much like a paper copy of a
 * filled out form. It describes which fields the form consisted of and their types, labels, etc. A stored FormFillout
 * preserves this data, so that this data doesn't have to be stored elsewhere. If a form is changed, a form fillout for
 * an earlier version of the form will still be valid and describe the form at the time of submission.
 *
 * A form fillout consists of a list of *fillout fields*. All fillout fields have an id, a label, and a type.
 * The type of a field can be one of the following:
 * - TEXT: A field where the user could input text.
 * - CHECKBOX: A checkbox field.
 * - RADIO_OPTIONS: A radio buttons field where the user could select one of multiple options.
 */
data class FormFillout(
    /** An identifier or name identifying which form this is fillout is for. */
    val formIdentifier: String,
    /** This version tag can be used to signify which version of a form a form fillout is for, and how much is
     *  changed between two versions. If a label text is changed, it might be denoted with a patch version bump. If the
     *  ordering of the fields are changed, or the set of options for a radioGroup field is changed, it might count as a
     *  minor version bump. If the set of fieldIds for a form is changed, which can happen if new fields are added or
     *  existing fields are removed, or if an existing fieldId is changed, it might count as a major version bump. */
    val formSemanticVersion: String,
    @field:NotEmpty
    val filloutFieldsList: List<FilloutField>,
) : Serializable {
    @get:JsonProperty
    val fieldValues: Map<String, Any>
        get() = filloutFieldsList.associate { filloutField ->
            filloutField.fieldID to when (filloutField) {
                is FilloutTextField -> filloutField.textValue
                is FilloutCheckboxField -> filloutField.wasChecked
                is FilloutRadioGroupField -> filloutField.selectedOptionId
                else -> throw IllegalArgumentException("Unknown field type: ${filloutField.fieldType}")
            }
        }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

abstract class FilloutField(
    @field:NotEmpty
    open val fieldID: String,
    @field:NotEmpty
    open val fieldLabel: String,
    open val fieldType: FormFilloutFieldType
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class FilloutTextField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotEmpty
    val textValue: String,
    val wasOptional: Boolean? = false,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.TEXT)

data class FilloutCheckboxField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotNull
    val wasChecked: Boolean,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.CHECKBOX)

data class FilloutRadioGroupField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotEmpty
    val selectedOptionId: String,
    @field:NotEmpty
    val selectedOptionLabel: String,
    @field:NotEmpty
    val options: List<FormFilloutFieldOption>,
    val wasOptional: Boolean? = false,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.RADIO_OPTIONS)

data class FormFilloutFieldOption(
    @field:NotEmpty
    val optionId: String,
    @field:NotEmpty
    val optionLabel: String,
    val wasSelected: Boolean = false,
)

enum class FormFilloutFieldType(val type: String) {
    TEXT("text"),
    CHECKBOX("checkbox"),
    RADIO_OPTIONS("radioOptions")
}
