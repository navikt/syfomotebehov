package no.nav.syfo.util

import io.kotest.core.spec.style.DescribeSpec
import org.apache.commons.text.StringEscapeUtils
import org.assertj.core.api.Assertions.assertThat

class DbUtilTest : DescribeSpec({
    describe("Test av DbUtil.sanitizeUserInput") {
        it("Skal fjerne html tags") {
            val sanitize = DbUtil.sanitizeUserInput("<html />")
            assertThat(sanitize).isEqualTo("")
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("<p>test<p/>")
            assertThat(sanitize).isEqualTo("test")
        }

        it("Skal fjerne br tag") {
            val sanitize = DbUtil.sanitizeUserInput("<br />")
            assertThat(sanitize).isEqualTo("")
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("test <no er som skummelt> kommer test")
            assertThat(sanitize).isEqualTo("test  kommer test")
        }

        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("3 er > enn fire men mindre enn <")
            assertThat(sanitize).isEqualTo("3 er > enn fire men mindre enn <")
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("3 er < enn fire men mindre enn >")
            assertThat(sanitize).isEqualTo("3 er < enn fire men mindre enn >")
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("man får lov til masse tegn æøå!==?||''\"\"///\\``")
            assertThat(sanitize).isEqualTo("man får lov til masse tegn æøå!==?||''\"\"///\\``")
        }
        it("Skal fjerne html tags og beholde tekst") {
            val sanitize = DbUtil.sanitizeUserInput("<div test />")
            assertThat(sanitize).isEqualTo("")
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
            assertThat(sanitize).isEqualTo(komplekstekst)
        }
        it("test10") {
            val sanitize = DbUtil.sanitizeUserInput(StringEscapeUtils.escapeHtml4("<script>test</script>"))
            assertThat(sanitize).isEqualTo("")
        }
    }
})
