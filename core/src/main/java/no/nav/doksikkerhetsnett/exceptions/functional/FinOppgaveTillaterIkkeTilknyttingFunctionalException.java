package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class FinOppgaveTillaterIkkeTilknyttingFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinOppgaveTillaterIkkeTilknyttingFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
