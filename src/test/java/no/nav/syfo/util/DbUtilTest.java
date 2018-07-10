package no.nav.syfo.util;

import org.junit.Test;

import static no.nav.syfo.repository.DbUtil.sanitizeUserInput;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.assertj.core.api.Assertions.assertThat;

public class DbUtilTest {

    @Test
    public void test1() {
        String sanitize = sanitizeUserInput("<html />");
        assertThat(sanitize).isEqualTo("");
    }

    @Test
    public void test2() {
        String sanitize = sanitizeUserInput("<p>test<p/>");
        assertThat(sanitize).isEqualTo("test");
    }

    @Test
    public void test3() {
        String sanitize = sanitizeUserInput("<br />");
        assertThat(sanitize).isEqualTo("");
    }

    @Test
    public void test4() {
        String sanitize = sanitizeUserInput("test <no er som skummelt> kommer test");
        assertThat(sanitize).isEqualTo("test  kommer test");
    }

    @Test
    public void test5() {
        String sanitize = sanitizeUserInput("3 er > enn fire men mindre enn <");
        assertThat(sanitize).isEqualTo("3 er > enn fire men mindre enn <");
    }

    @Test
    public void test6() {
        String sanitize = sanitizeUserInput("3 er < enn fire men mindre enn >");
        assertThat(sanitize).isEqualTo("3 er < enn fire men mindre enn >");
    }

    @Test
    public void test7() {
        String sanitize = sanitizeUserInput("man får lov til masse tegn æøå!==?||''\"\"///\\``");
        assertThat(sanitize).isEqualTo("man får lov til masse tegn æøå!==?||''\"\"///\\``");
    }

    @Test
    public void test8() {
        String sanitize = sanitizeUserInput("<div test />");
        assertThat(sanitize).isEqualTo("");
    }

    @Test
    public void test9() {
        String komplekstekst = "\"Dette er en ! #test 'for' å teste div. prosent % tegn & kanskje / eller´ men også \\\" og tegn som ; og : større enn >, men mindre enn < test@nav.no ^^\\n\"+\n" +
                "            \"\\n\"+\n" +
                "            \"\\n\"+\n" +
                "            \"Og også linjeskift bør fungere fint (ja) eller §3.1 -_- ** \\n\"+\n" +
                "            \"\\n\"+\n" +
                "            \"???\"";
        String sanitize = sanitizeUserInput(komplekstekst);
        assertThat(sanitize).isEqualTo(komplekstekst);
    }

    @Test
    public void test10() {
        String sanitize = sanitizeUserInput(escapeHtml4("<script>test</script>"));
        assertThat(sanitize).isEqualTo("");
    }
}
