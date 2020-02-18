package no.nav.doksikkerhetsnett.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnMottatteJournalposter.UbehandletJournalpost;
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

	public ArrayList<FinnOppgaveResponse> finnOppgaver(List<UbehandletJournalpost> ubehandledeJournalposter, boolean checkForOpenJournalPosts) {
		ArrayList<String> idStrings = Utils.journalpostListToJournalpostIdList(ubehandledeJournalposter);
		String status = getStatus(checkForOpenJournalPosts);
		ArrayList<FinnOppgaveResponse> oppgaveResponses = new ArrayList<>();
		
		for(String journalpostIdsAsString : idStrings){
			oppgaveResponses.add(finnOppgaveConsumer.finnOppgaveForJournalposter(journalpostIdsAsString+"&statuskategori="+status+"&sorteringsrekkefolge=ASC&limit=20"));
		}
		return oppgaveResponses;
	}

	public String getStatus(boolean checkForOpenJournalPosts){
		if(checkForOpenJournalPosts)
			return AAPEN;
		else
			return AVSLUTTET;
	}
}
