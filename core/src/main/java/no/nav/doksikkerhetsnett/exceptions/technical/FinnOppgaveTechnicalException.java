package no.nav.doksikkerhetsnett.exceptions.technical;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ResponseStatus(value = INTERNAL_SERVER_ERROR)
public class FinnOppgaveTechnicalException extends AbstractDoksikkerhetsnettTechnicalException {
	public FinnOppgaveTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
