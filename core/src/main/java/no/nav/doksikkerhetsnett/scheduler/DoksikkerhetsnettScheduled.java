package no.nav.doksikkerhetsnett.scheduler;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.OppgaveJson;
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

	public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
									  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									  FinnOppgaveService finnOppgaveService) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnOppgaveService = finnOppgaveService;
	}

	@Scheduled(initialDelay = 1000, fixedDelay = 10 * MINUTE)
	public void triggerOppdatering() {
		lagOppgaverForGlemteJournalposter();
	}

	public void lagOppgaverForGlemteJournalposter() {
		log.info("doksikkerhetsnett henter alle ubehandlede journalposter med alder > 1 uke (evt med tema i: {})", dokSikkerhetsnettProperties.getTemaer());

		try {
			finnJournalposterUtenTilknyttetOppgave();
		} catch (Exception e) {
			log.error("doksikkerhetsnett feilet under hentingen av alle journalposter (evt med tema i: {}): " + e.getMessage(), dokSikkerhetsnettProperties.getTemaer());
			return;
		}
	}

	private void finnJournalposterUtenTilknyttetOppgave() {
		FinnMottatteJournalposterResponse finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(dokSikkerhetsnettProperties
				.getTemaer());
		
		finnEksisterendeOppgaverFraUbehandledeJournalpostList( finnMottatteJournalposterResponse.getJournalposter());
		log.info("doksikkerhetsnett fant {} journalposter uten oppgave (evt med tema i: {})", dokSikkerhetsnettProperties.getTemaer());

		//TODO: burde fjernes at some point. For lettere testing av resultatet.
		String ret = "";
		for(UbehandletJournalpost ubhjp : finnMottatteJournalposterResponse.getJournalposter()) {
			ret += ubhjp.getJournalpostId() + " ";
		}
		log.info("journalpostene hadde ID'ene: " + ret);
	}

	private void finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<UbehandletJournalpost> ubehandledeJournalpostList){
		fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList, true);
		fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList, false);
	}

	private void fjernJournalposterMedEksisterendeOppgaverFraListe(List<UbehandletJournalpost> ubehandledeJournalpostList, boolean searchforOpenJournalposts){
		ArrayList<FinnOppgaveResponse> oppgaveResponseList = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList, searchforOpenJournalposts);
		oppgaveResponseList.removeIf((oppgave -> oppgave.getOppgaver() == null));

		List<String> journalposterMedOppgaver = oppgaveResponseList.stream()
				.flatMap(FinnOppgaveResponse -> FinnOppgaveResponse.getOppgaver().stream())
				.map(OppgaveJson::getJournalpostId)
				.collect(Collectors.toList());

		ubehandledeJournalpostList.removeIf(ubehandletJournalpost -> journalposterMedOppgaver.contains(""+ubehandletJournalpost.getJournalpostId()));

	}

}

