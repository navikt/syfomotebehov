package no.nav.syfo.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.apache.commons.text.StringEscapeUtils

class DbUtilTest : DescribeSpec({
    describe("Test av DbUtil.sanitizeUserInput") {
        it("Skal fjerne html tags") {
            val sanitize = DbUtil.sanitizeUserInput("<html />")
            sanitize shouldBe ""
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("<p>test<p/>")
            sanitize shouldBe "test"
        }

        it("Skal fjerne br tag") {
            val sanitize = DbUtil.sanitizeUserInput("<br />")
            sanitize shouldBe ""
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("test <no er som skummelt> kommer test")
            sanitize shouldBe "test  kommer test"
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("3 er > enn fire men mindre enn <")
            sanitize shouldBe "3 er > enn fire men mindre enn <"
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("3 er < enn fire men mindre enn >")
            sanitize shouldBe "3 er < enn fire men mindre enn >"
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("man får lov til masse tegn æøå!==?||''\"\"///\\``")
            sanitize shouldBe "man får lov til masse tegn æøå!==?||''\"\"///\\``"
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("<div test />")
            sanitize shouldBe ""
        }
        it("test9") {
            val komplekstekst =
                """"Dette er en ! #test 'for' å teste div. prosent % tegn & kanskje / eller´ men også \" og tegn som ; og : større enn >, men mindre enn < test@nav.no ^^\n"+
            "\n"+
            "\n"+
            "Og også linjeskift bør fungere fint (ja) eller §3.1 -_- ** \n"+
            "\n"+
            "???""""
            val sanitize = DbUtil.sanitizeUserInput(komplekstekst)
            sanitize shouldBe komplekstekst
        }
        it("test10") {
            val sanitize = DbUtil.sanitizeUserInput(StringEscapeUtils.escapeHtml4("<script>test</script>"))
            sanitize shouldBe ""
        }
    }
})
