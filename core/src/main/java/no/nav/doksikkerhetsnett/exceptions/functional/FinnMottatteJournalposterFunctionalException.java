package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FinnMottatteJournalposterFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {

    public FinnMottatteJournalposterFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
