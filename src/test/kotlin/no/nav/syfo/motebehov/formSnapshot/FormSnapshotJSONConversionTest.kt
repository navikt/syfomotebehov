package no.nav.syfo.motebehov.formSnapshot

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FormSnapshotJSONConversionTest : DescribeSpec({

    describe("FormSnapshotJSONConversion") {

        mockFormSnapshots.forEach { (formSnapshotName, formSnapshotToConvert) ->

            it("should get the same FormSnapshot back after converting to json and back for $formSnapshotName") {
                val json = convertFormSnapshotToJsonString(formSnapshotToConvert)
                println(json)

                val formSnapshotConvertedBackFromJson = convertJsonStringToFormSnapshot(json)

                formSnapshotConvertedBackFromJson shouldBe formSnapshotToConvert
            }
        }
    }
})
