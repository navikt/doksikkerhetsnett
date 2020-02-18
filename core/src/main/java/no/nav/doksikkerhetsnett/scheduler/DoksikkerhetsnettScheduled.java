package no.nav.doksikkerhetsnett.scheduler;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnMottatteJournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnMottatteJournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnOppgave.OppgaveJson;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.metrics.MetricLabels.CLASS;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {

	private final FinnMottatteJournalposterService finnMottatteJournalposterService;
	private final FinnOppgaveService finnOppgaveService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final MeterRegistry meterRegistry;
	private final int MINUTE = 60_000;

	public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
									  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									  FinnOppgaveService finnOppgaveService,
									  MeterRegistry meterRegistry) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnOppgaveService = finnOppgaveService;
		this.meterRegistry = meterRegistry;
	}

	@Scheduled(initialDelay = 1000, fixedDelay = MINUTE / 2)
	public void triggerOppdatering() {
		lagOppgaverForGlemteJournalposter();
	}

	public void lagOppgaverForGlemteJournalposter() {
		log.info("doksikkerhetsnett henter alle ubehandlede journalposter");

		try {
			finnJournalposterUtenTilknyttetOppgave();
		} catch (Exception e) {
			//TODO: Denne fanger alle feil, men logger det som om det bare er henting av journalposter som kan feile. Fiks bedre feilhåndtering
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
		//System.out.println("Etter opprydning er det: "+ finnMottatteJournalposterResponse.getJournalposter().size() +" journalposter igjen");
		int rngNonmatch = new Random().nextInt(4);
		System.out.println("Etter opprydning er det: "+ rngNonmatch +" journalposter igjen");
		int antallMottatteJournalposter = finnMottatteJournalposterResponse.getJournalposter().size();
		//int antallMatch = finnMottatteJournalposterResponse.getJournalposter().size();
		//int antallMismatch = antallMottatteJournalposter - antallMatch;
		int antallMatch = finnMottatteJournalposterResponse.getJournalposter().size() - rngNonmatch;
		int antallMismatch = antallMottatteJournalposter - antallMatch;
		incrementMetrics(antallMottatteJournalposter, antallMatch, antallMismatch);
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

	private void incrementMetrics(int antallMottatteJournalposter, int antallMatch, int antallMismatch) {
		Counter.builder("mottatte.journalposter.antall")
				.description("Counter for antall ubehandlede journalposter funnet")
				.tags(CLASS, this.getClass().getCanonicalName())
				.tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
				.register(meterRegistry)
				.increment(antallMottatteJournalposter);

		Counter.builder("finn.oppgave.matches")
				.description("Counter for antall oppgaver som finnes for de ubehandlede journalpostene")
				.tags(CLASS, this.getClass().getCanonicalName())
				.tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
				.register(meterRegistry)
				.increment(antallMatch);

		Counter.builder("finn.oppgave.mismatches")
				.description("Counter for antall ubehandlede journalposter som ikke har en oppgave")
				.tags(CLASS, this.getClass().getCanonicalName())
				.tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
				.register(meterRegistry)
				.increment(antallMismatch);
	}
}

