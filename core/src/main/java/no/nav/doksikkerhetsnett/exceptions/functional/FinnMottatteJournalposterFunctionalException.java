package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class FinnMottatteJournalposterFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinnMottatteJournalposterFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
