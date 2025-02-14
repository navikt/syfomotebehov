package no.nav.syfo.motebehov

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
    /** An id identifying which form this is fillout is for. */
    val formIdentifier: String,
    /** This version tag can be used to signify which version of a form a form fillout is for.
     * If the form is changed in a significant way that the consumer of this formFillout should know about,
     * the formVersionTag might be updated. */
    val formVersionTag: String,
    @field:NotEmpty
    val filloutFields: List<FilloutField>,
) : Serializable

abstract class FilloutField(
    @field:NotEmpty
    open val fieldID: String,
    @field:NotEmpty
    open val fieldLabel: String,
    open val fieldType: FormFilloutFieldType,
    // val conditionalOptionalExplanation: String? = null
) : Serializable

data class FilloutTextField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotEmpty
    val textValue: String,
    val isOptional: Boolean? = false,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.TEXT)

data class FilloutCheckboxField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotNull
    val isChecked: Boolean,
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
    val isOptional: Boolean? = false,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.RADIO_OPTIONS)

data class FilloutSelectField(
    override val fieldID: String,
    override val fieldLabel: String,
    @field:NotEmpty
    val selectedOptionId: String,
    @field:NotEmpty
    val selectedOptionLabel: String,
    @field:NotEmpty
    val options: List<FormFilloutFieldOption>,
    val isOptional: Boolean? = false,
) : FilloutField(fieldID, fieldLabel, FormFilloutFieldType.SELECT_OPTIONS)

data class FormFilloutFieldOption(
    @field:NotEmpty
    val optionId: String,
    @field:NotEmpty
    val optionLabel: String,
    val isSelected: Boolean = false,
)

enum class FormFilloutFieldType(val type: String) {
    TEXT("text"),
    CHECKBOX("checkbox"),
    RADIO_OPTIONS("radioOptions"),
    SELECT_OPTIONS("selectOptions")
}
