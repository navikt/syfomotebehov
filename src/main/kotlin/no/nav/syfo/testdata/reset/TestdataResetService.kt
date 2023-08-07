package no.nav.syfo.testdata.reset

import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.motebehov.database.MotebehovDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class TestdataResetService @Inject constructor(
    private val motebehovDAO: MotebehovDAO,
    private val dialogmoteDAO: DialogmoteDAO,
    private val pdlConsumer: PdlConsumer,
) {

    fun resetTestdata(fnr: String) {
        val aktoerId = pdlConsumer.aktorid(fnr)
        motebehovDAO.nullstillMotebehov(aktoerId)
        log.info("Nullstiller møtebehov for fnr $fnr")
        dialogmoteDAO.nullstillDialogmoter(fnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestdataResetService::class.java)
    }
}
