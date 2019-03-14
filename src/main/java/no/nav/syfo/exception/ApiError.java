package no.nav.syfo.exception;

import lombok.Getter;

@Getter
public class ApiError {
    private int status;
    private String message;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}

