package no.nav.doksikkerhetsnett.services;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FinnMottatteJournalposterService {

	private final FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer;

	@Autowired
	public FinnMottatteJournalposterService(FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer) {
		this.finnMottatteJournalposterConsumer = finnMottatteJournalposterConsumer;
	}

	public FinnMottatteJournalposterResponse finnMottatteJournalPoster(String tema, int eldreEnn) {
		return finnMottatteJournalposterConsumer.finnMottatteJournalposter(tema, eldreEnn);
	}

}
