package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FinnOppgaveFinnesIkkeFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
    public FinnOppgaveFinnesIkkeFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
