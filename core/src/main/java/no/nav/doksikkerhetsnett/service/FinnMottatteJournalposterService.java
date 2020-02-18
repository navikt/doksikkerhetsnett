package no.nav.doksikkerhetsnett.service;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class FinnMottatteJournalposterService {

	private FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer;

	@Inject
	public FinnMottatteJournalposterService(FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer) {
		this.finnMottatteJournalposterConsumer = finnMottatteJournalposterConsumer;
	}

	public FinnMottatteJournalposterResponse finnMottatteJournalPoster(String temaer) {
		if (temaer == null) {
			return finnMottatteJournalPoster();
		}
		return finnMottatteJournalposterConsumer.finnMottateJournalposter(temaer);
	}

	public FinnMottatteJournalposterResponse finnMottatteJournalPoster(List<String> temaer) {
		return finnMottatteJournalposterConsumer.finnMottateJournalposter(Utils.formatTemaList(temaer));
	}


	public FinnMottatteJournalposterResponse finnMottatteJournalPoster() {
		return finnMottatteJournalposterConsumer.finnMottateJournalposter("");
	}


}
