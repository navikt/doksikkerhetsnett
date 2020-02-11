package no.nav.doksikkerhetsnett.exceptions.technical;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class FinnMottatteJournalposterTechnicalException extends AbstractDoksikkerhetsnettTechnicalException {
	
	public FinnMottatteJournalposterTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
