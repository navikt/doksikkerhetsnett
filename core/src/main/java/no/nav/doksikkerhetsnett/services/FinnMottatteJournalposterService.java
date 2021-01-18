package no.nav.doksikkerhetsnett.services;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
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

	public FinnMottatteJournalposterResponse finnMottatteJournalPoster(String tema, int eldreEnn) {
		return finnMottatteJournalposterConsumer.finnMottatteJournalposter(tema, eldreEnn);
	}

}
