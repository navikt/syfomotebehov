package no.nav.syfo.util

import java.util.Base64

fun basicCredentials(
    credentialUsername: String,
    credentialPassword: String,
): String =
    "Basic " +
        Base64.getEncoder().encodeToString(
            java.lang.String
                .format("%s:%s", credentialUsername, credentialPassword)
                .toByteArray(),
        )

fun bearerCredentials(token: String) = "Bearer $token"
