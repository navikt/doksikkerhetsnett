package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class FinnOppgaveTillaterIkkeTilknyttingFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
    public FinnOppgaveTillaterIkkeTilknyttingFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
