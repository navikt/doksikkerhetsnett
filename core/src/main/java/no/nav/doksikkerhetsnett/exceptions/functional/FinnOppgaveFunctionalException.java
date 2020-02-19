package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FinnOppgaveFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinnOppgaveFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
