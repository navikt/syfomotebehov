package no.nav.syfo.unleash;

import no.finn.unleash.Unleash;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ToggleImplTest {
    @Mock
    private Unleash unleash;

    @Test
    public void isTestEnvironmentFalseForProd() {
        Toggle toggle = new ToggleImpl(unleash, "p");
        assertThat(toggle.isTestEnvironment()).isFalse();
    }

    @Test
    public void isTestEnvironmentTrueForTest() {
        Toggle toggle = new ToggleImpl(unleash, "q1");
        assertThat(toggle.isTestEnvironment()).isTrue();
    }
}
