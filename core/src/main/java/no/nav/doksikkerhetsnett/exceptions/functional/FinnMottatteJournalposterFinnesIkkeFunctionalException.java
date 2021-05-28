package no.nav.doksikkerhetsnett.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(value = NOT_FOUND)
public class FinnMottatteJournalposterFinnesIkkeFunctionalException extends AbstractDoksikkerhetsnettFunctionalException {
	public FinnMottatteJournalposterFinnesIkkeFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
