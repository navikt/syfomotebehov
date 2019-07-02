package no.nav.syfo.kafka.producer.model;

import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Value
@Builder
@Getter
public class KOversikthendelse {

    @NotEmpty
    @Pattern(regexp = "^[0-9]{11}$")
    private String fnr;
    @NotEmpty
    private String hendelseId;
    @NotEmpty
    private String enhetId;
    @NotNull
    private LocalDateTime tidspunkt;
}
