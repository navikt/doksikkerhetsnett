package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.CONFLICT;

@ResponseStatus(value = CONFLICT)
public class FinnOppgaveTillaterIkkeTilknyttingFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinnOppgaveTillaterIkkeTilknyttingFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
