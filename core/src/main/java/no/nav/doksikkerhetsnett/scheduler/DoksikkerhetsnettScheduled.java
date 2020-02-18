package no.nav.doksikkerhetsnett.scheduler;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.OppgaveJson;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {

	private final FinnMottatteJournalposterService finnMottatteJournalposterService;
	private final FinnOppgaveService finnOppgaveService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final int MINUTE = 60_000;
	private final int HOUR = 60*MINUTE;

	public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
									  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									  FinnOppgaveService finnOppgaveService) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnOppgaveService = finnOppgaveService;
	}

	@Scheduled(initialDelay = 1000, fixedDelay = 24 * HOUR)
	public void triggerOppdatering() {
		lagOppgaverForGlemteJournalposter();
	}

	public void lagOppgaverForGlemteJournalposter() {

		log.info("doksikkerhetsnett henter alle ubehandlede journalposter med alder > 1 uke (evt med tema i: {})", dokSikkerhetsnettProperties
				.getTemaer());
		FinnMottatteJournalposterResponse finnMottatteJournalposterResponse;
		try {
			finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(dokSikkerhetsnettProperties
					.getTemaer());
		} catch (Exception e) {
			log.error("doksikkerhetsnett feilet under hentingen av alle journalposter (evt med tema i: {}): " + e.getMessage(), dokSikkerhetsnettProperties
					.getTemaer());
			return;
		}

		try{
			ArrayList<UbehandletJournalpost> ubehandletJournalposts = finnEksisterendeOppgaverFraUbehandledeJournalpostList(finnMottatteJournalposterResponse
					.getJournalposter());
			log.info("doksikkerhetsnett fant {} journalposter uten oppgave (evt med tema i: {})",
					ubehandletJournalposts.size(), dokSikkerhetsnettProperties.getTemaer());
			log.info("journalpostene hadde ID'ene: {}", ubehandletJournalposts);
		}  catch (Exception e){
			log.error("doksikkerhetsnett feilet under hentingen av alle oppgaver");
		}
	}

	private ArrayList<UbehandletJournalpost> finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<UbehandletJournalpost> ubehandledeJournalpostList) {
		ArrayList<UbehandletJournalpost> retList = fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList, true);
		return fjernJournalposterMedEksisterendeOppgaverFraListe(retList, false);
	}

	private ArrayList<UbehandletJournalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<UbehandletJournalpost> ubehandledeJournalpostList, boolean searchforOpenJournalposts) {
		FinnOppgaveResponse oppgaveResponse = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList, searchforOpenJournalposts);

		if (oppgaveResponse.getOppgaver() == null) {
			return new ArrayList<UbehandletJournalpost>();
		}

		List<String> journalposterMedOppgaver = oppgaveResponse.getOppgaver().stream()
				.map(OppgaveJson::getJournalpostId)
				.collect(Collectors.toList());

		return new ArrayList<>(ubehandledeJournalpostList.stream()
				.filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains("" + ubehandletJournalpost.getJournalpostId()))
				.collect(Collectors.toList()));
	}

}

