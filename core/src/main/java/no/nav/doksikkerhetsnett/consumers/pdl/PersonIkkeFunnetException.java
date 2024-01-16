package no.nav.doksikkerhetsnett.consumers.pdl;

import no.nav.doksikkerhetsnett.exceptions.functional.AbstractDoksikkerhetsnettFunctionalException;

public class PersonIkkeFunnetException extends AbstractDoksikkerhetsnettFunctionalException {

	public PersonIkkeFunnetException(String message) {
		super(message);
	}
}