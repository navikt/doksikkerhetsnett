package no.nav.doksikkerhetsnett.exceptions.functional;

public class AbstractDoksikkerhetsnettFunctionalException extends RuntimeException {

	public AbstractDoksikkerhetsnettFunctionalException(String message) {
		super(message);
	}

	public AbstractDoksikkerhetsnettFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
