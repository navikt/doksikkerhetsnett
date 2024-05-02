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

import static org.apache.commons.collections4.ListUtils.partition;

@Slf4j
@Component
public class FinnGjenglemteJournalposterService {

	public static final int JOURNALPOSTER_PARTITION_LIMIT = 50;

	// journalpostId med mange elementer kan knekke visning i kibana
	private static final int MAX_JOURNALPOSTID_LOGGING = 400;

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
		log.info("Doksikkerhetsnett henter alle ubehandlede journalposter eldre enn {} dager fra tema: {}", dager, tema);

		List<Journalpost> ubehandledeJournalposter = journalpostConsumer.finnMottatteJournalposter(tema, dager).getJournalposter();
		List<Journalpost> ubehandledeJournalposterUtenOppgave = findUbehandledeJournalposterUtenOppgave(tema, ubehandledeJournalposter, dager);

		metricsService.updateGauges(ubehandledeJournalposter, ubehandledeJournalposterUtenOppgave, dager);
		return ubehandledeJournalposterUtenOppgave;
	}

	private List<Journalpost> findUbehandledeJournalposterUtenOppgave(String tema, List<Journalpost> ubehandledeJournalposter, int dagerGamle) {
		List<Journalpost> ubehandledeJournalposterUtenOppgave = fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalposter);
		loggUbehandledeJournalposter(tema, dagerGamle, ubehandledeJournalposterUtenOppgave);
		return ubehandledeJournalposterUtenOppgave;
	}

	private static void loggUbehandledeJournalposter(String tema, int dagerGamle, List<Journalpost> ubehandledeJournalposterUtenOppgave) {
		if (ubehandledeJournalposterUtenOppgave.size() > MAX_JOURNALPOSTID_LOGGING) {
			var partitions = partition(ubehandledeJournalposterUtenOppgave, MAX_JOURNALPOSTID_LOGGING);
			loggUbehandledeJournalposterFunnOverMAX_JOURNALPOSTID_LOGGING(tema, dagerGamle, partitions.get(0), ubehandledeJournalposterUtenOppgave.size());
		} else {
			loggUbehandledeJournalposterFunn(tema, dagerGamle, ubehandledeJournalposterUtenOppgave);
		}
	}

	private static void loggUbehandledeJournalposterFunn(String tema, int dagerGamle, List<Journalpost> ubehandledeJournalposterUtenOppgave) {
		log.info("Fant {} journalposter med tema {} som er eldre enn {} dag(er) og mangler oppgave. {}",
				ubehandledeJournalposterUtenOppgave.size(), tema, dagerGamle, ubehandledeJournalposterLoggSetning(ubehandledeJournalposterUtenOppgave));
	}

	private static void loggUbehandledeJournalposterFunnOverMAX_JOURNALPOSTID_LOGGING(String tema, int dagerGamle, List<Journalpost> ubehandledeJournalposterUtenOppgave, int antall) {
		log.info("Fant totalt {} journalposter med tema {} som er eldre enn {} dag(er) og mangler oppgave. De første 400 journalpostene har journalpostId=" + ubehandledeJournalposterUtenOppgave,
				antall, tema, dagerGamle);
	}

	private static String ubehandledeJournalposterLoggSetning(List<Journalpost> ubehandledeJournalposterUtenOppgave) {
		return ubehandledeJournalposterUtenOppgave.isEmpty() ? "" : "journalpostene har journalpostId=" + ubehandledeJournalposterUtenOppgave;
	}

	private ArrayList<Journalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<Journalpost> ubehandledeJournalpostList) {
		var partitionedIds = journalpostListToPartitionedJournalpostIdList(ubehandledeJournalpostList, JOURNALPOSTER_PARTITION_LIMIT);

		ArrayList<FinnOppgaveResponse> oppgaveResponses = new ArrayList<>();

		for (List<Long> ids : partitionedIds) {
			FinnOppgaveResponse oppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(ids, 0);
			if (oppgaveResponse != null) {
				oppgaveResponses.add(oppgaveResponse);
				int differenceBetweenTotalReponsesAndResponseList = oppgaveResponse.getAntallTreffTotalt() - oppgaveResponse.getOppgaver()
						.size();
				if (differenceBetweenTotalReponsesAndResponseList != 0) {
					int extraPages = differenceBetweenTotalReponsesAndResponseList / JOURNALPOSTER_PARTITION_LIMIT;
					for (int i = 1; i <= extraPages + 1; i++) {
						oppgaveResponses.add(finnOppgaveConsumer.finnOppgaveForJournalposter(ids, i));
					}
				}
			}
		}

		List<String> journalposterMedOppgaver = oppgaveResponses.stream()
				.flatMap(finnOppgaveResponse -> finnOppgaveResponse.getOppgaver().stream())
				.map(Oppgave::getJournalpostId)
				.toList();

		return new ArrayList<>(ubehandledeJournalpostList.stream()
				.filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains(Long.toString(ubehandletJournalpost.getJournalpostId())))
				.toList());
	}

	public static List<List<Long>> journalpostListToPartitionedJournalpostIdList(List<Journalpost> ubehandledeJournalposter, int limit) {
		if (ubehandledeJournalposter == null) {
			return new ArrayList<>();
		}
		List<Long> journalpostIds = ubehandledeJournalposter.stream().map(Journalpost::getJournalpostId).toList();

		return partition(journalpostIds, limit);
	}

}
