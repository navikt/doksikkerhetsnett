package no.nav.doksikkerhetsnett.mappers;

import no.nav.doksikkerhetsnett.entities.Journalpost;

import static no.nav.doksikkerhetsnett.entities.Oppgave.TEMA_BID;
import static no.nav.doksikkerhetsnett.entities.Oppgave.TEMA_FAR;
import static no.nav.doksikkerhetsnett.entities.Oppgave.TEMA_GENERELL;
import static no.nav.doksikkerhetsnett.entities.Oppgave.TEMA_UKJENT;

public class OppgaveTemaMapper {


	public static String mapJpTemaToOppgaveTema(Journalpost jp) {
		if (jp.getTema() == null || TEMA_UKJENT.equals(jp.getTema())) {
			return TEMA_GENERELL;
			//Farskapssaker som blir plukket opp i doksikkerhetsnett skal sparkes videre til Bidrag, ref MMA-6349
		} else if (TEMA_FAR.equals(jp.getTema())){
			return TEMA_BID;
		}
		return jp.getTema();
	}
}
