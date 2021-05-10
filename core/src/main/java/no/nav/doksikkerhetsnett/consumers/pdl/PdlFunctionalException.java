package no.nav.doksikkerhetsnett.consumers.pdl;

public class PdlFunctionalException extends RuntimeException {
    public PdlFunctionalException(String message) {
        super(message);
    }

    public PdlFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
