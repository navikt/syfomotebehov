package no.nav.syfo.kafka.producer.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@Accessors(fluent = true, chain = true)
public class KOversikthendelse {

    @NotEmpty
    @Pattern(regexp = "^[0-9]{11}$")
    public String fnr;
    @NotEmpty
    public String hendelseId;
    @NotEmpty
    public String enhetId;
    @NotNull
    public LocalDateTime tidspunkt;
}
