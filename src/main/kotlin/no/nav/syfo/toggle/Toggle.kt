package no.nav.syfo.toggle

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Toggle(
    @Value("\${toggle.enable.nullstill:false}") enableNullstill: Boolean
) {
    companion object {
        var enableNullstill = false
    }

    init {
        Companion.enableNullstill = enableNullstill
    }
}
