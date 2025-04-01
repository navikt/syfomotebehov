package no.nav.syfo.testhelper.generator

import no.nav.syfo.motebehov.motebehovstatus.MotebehovSkjemaType
import no.nav.syfo.motebehov.motebehovstatus.MotebehovStatus

private val motebehovGenerator = MotebehovGenerator()

val generateMotebehovStatus = MotebehovStatus(
    visMotebehov = true,
    skjemaType = MotebehovSkjemaType.SVAR_BEHOV,
    motebehov = motebehovGenerator.generateMotebehov()
).copy()
