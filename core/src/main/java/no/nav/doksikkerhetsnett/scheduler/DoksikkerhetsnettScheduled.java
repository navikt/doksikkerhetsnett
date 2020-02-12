package no.nav.doksikkerhetsnett.scheduler;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {

	private final FinnMottatteJournalposterService finnMottatteJournalposterService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final int MINUTE = 60_000;

	public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
									  DokSikkerhetsnettProperties dokSikkerhetsnettProperties) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
	}

	@Scheduled(initialDelay = 1000, fixedDelay = 10 * MINUTE)
	public void triggerOppdatering() {
		lagOppgaverForGlemteJournalposter();
	}

	public void lagOppgaverForGlemteJournalposter() {
		log.info("doksikkerhetsnett lager oppgaver for alle gjenglemte journalposter");

		try {
			tildelOppgave();
		} catch (Exception e) {
			log.error("doksikkerhetsnett feilet under hentingen av alle journalposter: " + e.getMessage());
			return;
		}

		//log.info("doksikkerhetsnett har laget oppgaver for alle gjenglemte journalposter");
	}

	private void tildelOppgave() {
		FinnMottatteJournalposterResponse finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(dokSikkerhetsnettProperties
				.getTemaer());

		if (finnMottatteJournalposterResponse.getJournalposter().size() > 0) {
			log.info("Finnmottattejournalposter har funnet {} poster med tema i{}", finnMottatteJournalposterResponse.getJournalposter()
					.size(), dokSikkerhetsnettProperties.getTemaer());
		}

		/* TODO:
		 * Del 1:  Finn hvilke poster som ikke har en oppgave
		 * Del 2:  Opprett en oppgave for disse.
		 */
	}

}

