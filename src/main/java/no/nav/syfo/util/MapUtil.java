package no.nav.syfo.util;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Optional.of;

public class MapUtil {
    private MapUtil() {
    }

    public static <T, R, S extends R> S map(T fra, S til, BiConsumer<T, R> exp) {
        return of(fra).map(f -> {
            exp.accept(f, til);
            return til;
        }).orElseThrow(() -> new RuntimeException("Resultatet fra exp ble null"));
    }

    public static <T, R> R map(T fra, Function<T, R> exp) {
        return of(fra).map(exp).orElseThrow(() -> new RuntimeException("Resultatet fra exp ble null"));
    }
}
