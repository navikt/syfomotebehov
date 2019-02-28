package no.nav.syfo.unleash.strategy;

import no.finn.unleash.strategy.Strategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ByEnvironmentStrategyTest {
    @Test
    public void getName() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.getName()).isEqualTo("byEnvironment");
    }

    @Test
    public void isEnabledParametersIsNull() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(null)).isFalse();
    }

    @Test
    public void isEnabledParametersIsEmpty() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(new HashMap<>())).isFalse();
    }

    @Test
    public void isEnabledParametersContainingWrongKey() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("wrongKey", "value");
                }})).isFalse();
    }

    @Test
    public void isEnabledParametersContainingNullKey() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put(null, null);
                }})).isFalse();
    }

    @Test
    public void isEnabledParametersContainingNullValue() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("miljø", null);
                }})).isFalse();
    }

    @Test
    public void isEnabledParametersContainingWrongEnvironment() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("miljø", "lacol");
                }})).isFalse();
    }

    @Test
    public void isEnabledParametersContainingRightEnvironment() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("miljø", "local");
                }})).isTrue();
    }

    @Test
    public void isEnabledParametersContainingMultipleEnvironments() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("miljø", "lacol,local,callo");
                }})).isTrue();
    }

    @Test
    public void isEnabledParametersContainingMultipleEnvironmentsWithSpaces() {
        Strategy strategy = new ByEnvironmentStrategy("local");
        assertThat(strategy.isEnabled(
                new HashMap<String, String>() {{
                    put("miljø", "lacol ,  , local , callo");
                }})).isTrue();
    }
}
