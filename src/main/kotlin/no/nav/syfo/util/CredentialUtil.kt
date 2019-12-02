package no.nav.syfo.util

import java.util.*

fun basicCredentials(credentialUsername: String, credentialPassword: String): String {
    return "Basic " + Base64.getEncoder().encodeToString(java.lang.String.format("%s:%s", credentialUsername, credentialPassword).toByteArray())
}

fun bearerCredentials(token: String): String {
    return "Bearer $token"
}
