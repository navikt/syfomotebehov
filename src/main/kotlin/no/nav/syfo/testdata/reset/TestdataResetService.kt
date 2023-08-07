package no.nav.syfo.testdata.reset

import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.motebehov.database.MotebehovDAO
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class TestdataResetService @Inject constructor(
    private val motebehovDAO: MotebehovDAO,
    private val pdlConsumer: PdlConsumer,
) {

    fun resetTestdata(fnr: String) {
        val aktoerId = pdlConsumer.aktorid(fnr)
        motebehovDAO.nullstillMotebehov(aktoerId)
    }
}
