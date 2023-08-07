package no.nav.syfo.testdata.reset

import no.nav.syfo.consumer.pdl.PdlConsumer
import no.nav.syfo.dialogmote.database.DialogmoteDAO
import no.nav.syfo.dialogmotekandidat.database.DialogmotekandidatDAO
import no.nav.syfo.motebehov.database.MotebehovDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class TestdataResetService @Inject constructor(
    private val motebehovDAO: MotebehovDAO,
    private val dialogmoteDAO: DialogmoteDAO,
    private val dialogmotekandidatDAO: DialogmotekandidatDAO,
    private val pdlConsumer: PdlConsumer,
) {

    fun resetTestdata(fnr: String) {
        val aktoerId = pdlConsumer.aktorid(fnr)
        log.info("Nullstiller møtebehov, dialogmøter, kandidat for fnr $fnr")
        motebehovDAO.nullstillMotebehov(aktoerId)
        dialogmoteDAO.nullstillDialogmoter(fnr)
        dialogmotekandidatDAO.delete(fnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestdataResetService::class.java)
    }
}
