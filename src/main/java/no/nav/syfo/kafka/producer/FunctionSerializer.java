package no.nav.syfo.kafka.producer;

import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.common.serialization.Serializer;

public class FunctionSerializer<T> implements Serializer<T> {
    private final Function<T, byte[]> serializer;

    public FunctionSerializer(Function<T, byte[]> serializer) {
        this.serializer = serializer;
    }

    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    public byte[] serialize(String topic, T t) {
        if (t == null) {
            return null;
        } else {
            try {
                return (byte[])this.serializer.apply(t);
            } catch (Exception var4) {
                throw new RuntimeException("Feil ved konvertering av søknad til bytes", var4);
            }
        }
    }

    public void close() {
    }
}
