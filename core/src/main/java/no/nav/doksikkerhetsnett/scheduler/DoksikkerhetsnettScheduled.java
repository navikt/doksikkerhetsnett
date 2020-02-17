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
		log.info("doksikkerhetsnett henter alle ubehandlede journalposter");

		try {
			finnJournalposterUtenTilknyttetOppgave();
		} catch (Exception e) {
			log.error("doksikkerhetsnett feilet under hentingen av alle journalposter (evt med tema i: {}): " + e.getMessage(), dokSikkerhetsnettProperties.getTemaer());
			return;
		}
		log.info("doksikkerhetsnett har hentet alle ubehandlede journalposter");
	}

	private void finnJournalposterUtenTilknyttetOppgave() {
		FinnMottatteJournalposterResponse finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(dokSikkerhetsnettProperties
				.getTemaer());
		System.out.println("Fant: "+ finnMottatteJournalposterResponse.getJournalposter().size() +" relevante journalposter");
		finnEksisterendeOppgaverFraUbehandledeJournalpostList( finnMottatteJournalposterResponse.getJournalposter());
		System.out.println("Etter opprydning er det: "+ finnMottatteJournalposterResponse.getJournalposter().size() +" journalposter igjen");
	}

	private void finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<UbehandletJournalpost> ubehandledeJournalpostList){
		fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList, true);
		fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList, false);
	}

	private void fjernJournalposterMedEksisterendeOppgaverFraListe(List<UbehandletJournalpost> ubehandledeJournalpostList, boolean searchforOpenJournalposts){
		//Finn alle åpne journalposter fra lista med ubehandlede journalposter
		ArrayList<FinnOppgaveResponse> oppgaveResponseList = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList, searchforOpenJournalposts);

		oppgaveResponseList.removeIf((oppgave -> oppgave.getOppgaver() == null));

		//Samler alle journalpostId'er fra svaret fra oppgave
		List<String> journalposterMedOppgaver = oppgaveResponseList.stream()
				.flatMap(FinnOppgaveResponse -> FinnOppgaveResponse.getOppgaver().stream())
				.map(OppgaveJson::getJournalpostId)
				.collect(Collectors.toList());

		//Sletter journalpostene fra arbeidslista dersom de allerede har oppgaver i systemet
		ubehandledeJournalpostList.removeIf(ubehandletJournalpost -> journalposterMedOppgaver.contains(""+ubehandletJournalpost.getJournalpostId()));

	}

}

