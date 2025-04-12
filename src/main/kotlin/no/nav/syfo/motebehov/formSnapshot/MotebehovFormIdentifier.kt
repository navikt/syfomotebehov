package no.nav.syfo.motebehov.formSnapshot

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

// The @JsonValue annotation tells Jackson how to serialize this enum to a JSON string.
// The @JsonCreator annotation tells Jackson how to deserialize a JSON string to this enum.
enum class MotebehovFormIdentifier(@JsonValue val identifier: String) {
    ARBEIDSGIVER_SVAR("motebehov-arbeidsgiver-svar"),
    ARBEIDSGIVER_MELD("motebehov-arbeidsgiver-meld"),
    ARBEIDSTAKER_SVAR("motebehov-arbeidstaker-svar"),
    ARBEIDSTAKER_MELD("motebehov-arbeidstaker-meld");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromStringToEnum(identifier: String): MotebehovFormIdentifier {
            return entries.find { it.identifier == identifier }
                ?: throw IllegalArgumentException("Unknown formIdentifier: $identifier")
        }
    }
}
