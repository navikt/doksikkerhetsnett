package no.nav.doksikkerhetsnett.consumers.pdl;


import no.nav.doksikkerhetsnett.exceptions.functional.AbstractDoksikkerhetsnettFunctionalException;

/**
 * Exception PersonIkkeFunnetException.
 *
 * @author Tak Wai Wang (Capgemini)
 */
public class PersonIkkeFunnetException extends AbstractDoksikkerhetsnettFunctionalException {
	public PersonIkkeFunnetException(String message) {
		super(message);
	}

	public PersonIkkeFunnetException(Throwable cause, String message) {
		super(message, cause);
	}
}
