package no.nav.doksikkerhetsnett.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FinnOppgaveService {
	private final String AAPEN = "AAPEN";
	private final String AVSLUTTET = "AVSLUTTET";


	FinnOppgaveConsumer finnOppgaveConsumer;

	@Inject
	public FinnOppgaveService(FinnOppgaveConsumer finnOppgaveConsumer) {
		this.finnOppgaveConsumer = finnOppgaveConsumer;
	}

	public FinnOppgaveResponse finnOppgaver(List<UbehandletJournalpost> ubehandledeJournalposter, boolean checkForOpenJournalPosts) {

		return finnOppgaveConsumer.finnOppgaveForJournalposter(ubehandledeJournalposter, getStatus(checkForOpenJournalPosts));
	}

	public String getStatus(boolean checkForOpenJournalPosts) {
		if (checkForOpenJournalPosts) {
			return AAPEN;
		} else {
			return AVSLUTTET;
		}
	}
}
