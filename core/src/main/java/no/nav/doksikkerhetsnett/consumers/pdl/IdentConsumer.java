package no.nav.doksikkerhetsnett.consumers.pdl;

public interface IdentConsumer {
	/**
	 * Henter NAV intern aktørId for folkeregisterIdent.
	 *
	 * @param folkeregisterIdent Folkeregisterident tilhørende person
	 * @return NAV intern aktørId
	 * @throws PersonIkkeFunnetException Finner ikke person
	 */
	String hentAktoerId(final String folkeregisterIdent) throws PersonIkkeFunnetException;
}
