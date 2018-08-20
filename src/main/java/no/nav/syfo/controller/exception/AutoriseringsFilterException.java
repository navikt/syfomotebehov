package no.nav.syfo.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
public class AutoriseringsFilterException extends RuntimeException {
    public AutoriseringsFilterException(String message) {
        super(message);
    }
}
