package no.nav.syfo.domain.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
@Accessors(fluent = true, chain = true)
@AllArgsConstructor
public class Fnr {
    
    @NotEmpty
    @Pattern(regexp = "^[0-9]{11}$")
    String fnr;

    @Override
    public boolean equals(Object o) {
        return fnr.equals(o);
    }

    @Override
    public int hashCode() {
        return fnr.hashCode();
    }

    @Override
    public String toString() {
        return fnr;
    }
}
