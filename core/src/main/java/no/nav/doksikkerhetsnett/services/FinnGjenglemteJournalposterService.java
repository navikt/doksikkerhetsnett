package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.metrics.MetricsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class FinnGjenglemteJournalposterService {

	private final MetricsService metricsService;
	private final FinnOppgaveConsumer finnOppgaveConsumer;
	private final FinnMottatteJournalposterConsumer journalpostConsumer;

	public FinnGjenglemteJournalposterService(MetricsService metricsService, FinnOppgaveConsumer finnOppgaveConsumer,
											  FinnMottatteJournalposterConsumer journalpostConsumer) {
		this.metricsService = metricsService;
		this.journalpostConsumer = journalpostConsumer;
		this.finnOppgaveConsumer = finnOppgaveConsumer;
	}

	public List<Journalpost> finnJournalposterUtenOppgaveUpdateMetrics(String tema, int dager) {
		List<Journalpost> ubehandledeJournalposter;
		List<Journalpost> ubehandledeJournalposterUtenOppgave;
		log.info("Doksikkerhetsnett henter alle ubehandlede journalposter eldre enn {} dager fra tema: {}", dager, tema);

		ubehandledeJournalposter = journalpostConsumer.finnMottatteJournalposter(tema, dager).getJournalposter();
		ubehandledeJournalposterUtenOppgave = findUbehandledeJournalposterUtenOppgave(tema, ubehandledeJournalposter, dager);

		metricsService.UpdateGauges(ubehandledeJournalposter, ubehandledeJournalposterUtenOppgave, dager);
		return ubehandledeJournalposterUtenOppgave;
	}

	private List<Journalpost> findUbehandledeJournalposterUtenOppgave(String tema, List<Journalpost> ubehandledeJournalposter, int dagerGamle) {
		List<Journalpost> ubehandledeJournalposterUtenOppgave;
		ubehandledeJournalposterUtenOppgave = fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalposter);
		log.info("Fant {} journalposter med tema {} som er eldre enn {} dag(er) og mangler oppgave. {}",
				ubehandledeJournalposterUtenOppgave.size(),
				tema,
				dagerGamle,
				ubehandledeJournalposterUtenOppgave.isEmpty() ? "" :
						"Journalpostene hadde ID'ene:" + ubehandledeJournalposterUtenOppgave + ".");
		return ubehandledeJournalposterUtenOppgave;
	}

	private ArrayList<Journalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<Journalpost> ubehandledeJournalpostList) {
		FinnOppgaveResponse oppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(ubehandledeJournalpostList);

		if (oppgaveResponse.getOppgaver() == null) {
			return new ArrayList<>();
		}

		List<String> journalposterMedOppgaver = oppgaveResponse.getOppgaver().stream()
				.map(Oppgave::getJournalpostId)
				.collect(Collectors.toList());

		return new ArrayList<>(ubehandledeJournalpostList.stream()
				.filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains(Long.toString(ubehandletJournalpost.getJournalpostId())))
				.collect(Collectors.toList()));
	}
}
