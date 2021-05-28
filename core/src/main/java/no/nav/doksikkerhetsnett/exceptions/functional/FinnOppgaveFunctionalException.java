package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(value = NOT_FOUND)
public class FinnOppgaveFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinnOppgaveFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
