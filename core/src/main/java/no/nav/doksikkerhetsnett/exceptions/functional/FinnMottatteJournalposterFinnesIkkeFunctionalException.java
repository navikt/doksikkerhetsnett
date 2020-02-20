package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FinnMottatteJournalposterFinnesIkkeFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {

    public FinnMottatteJournalposterFinnesIkkeFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
