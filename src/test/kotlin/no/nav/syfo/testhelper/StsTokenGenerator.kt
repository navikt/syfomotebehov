package no.nav.syfo.testhelper

import no.nav.syfo.sts.STSToken

val stsToken = STSToken(
        access_token = UserConstants.STS_TOKEN,
        token_type = "Bearer",
        expires_in = 3600
)

fun generateStsToken(): STSToken {
    return stsToken
}
