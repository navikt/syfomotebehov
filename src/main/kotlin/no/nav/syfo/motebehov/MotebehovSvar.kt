package no.nav.syfo.motebehov

import java.io.Serializable

data class MotebehovSvar(
    val harMotebehov: Boolean,
    // TODO: Delete forklaring here after other apps are updated to use new endpoints.
    val forklaring: String? = null,
    /** Each field in the submitted form is represented here with a unique fieldID. The order, number of
     * fields, field types, and  so on, should be easy to change in frontend app, so this app shouldn't put any
     * constraints on the contents. The fieldIDs should probably only be used for data collection purposes. */
    val dynamicFormSubmission: List<MotebehovFormSubmissionField>,
) : Serializable

data class MotebehovSvarSubmissionOldDTO(
    val harMotebehov: Boolean,
    val forklaring: String? = null,
)

data class MotebehovDynamicFormSubmissionDTO(
    val harMotebehov: Boolean,
    val dynamicFormSubmission: List<MotebehovFormSubmissionField>,
)

data class MotebehovFormSubmissionField(
    val fieldID: String,
    // TODOs
    val fieldValue: String,
    val fieldLabel: String,
    val fieldType: String,
)
