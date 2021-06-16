package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.services.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.services.FinnOppgaveService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {
	private final FinnMottatteJournalposterService finnMottatteJournalposterService;
	private final FinnOppgaveService finnOppgaveService;
	private final OpprettOppgaveService opprettOppgaveService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final MetricsScheduler metricsScheduler;

	private static final String TEMA_ALLE = "ALLE";
	private static final int EN_DAG = 1;
	private static final int TO_DAGER = 2;
	private static final int FEM_DAGER = 5;


	public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
									  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									  FinnOppgaveService finnOppgaveService,
									  OpprettOppgaveService opprettOppgaveService,
									  MetricsScheduler metricsScheduler) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnOppgaveService = finnOppgaveService;
		this.opprettOppgaveService = opprettOppgaveService;
		this.metricsScheduler = metricsScheduler;
	}

    // Satt til å kjøre klokken 07:00 på man - fre
    @Scheduled(cron = "0 0 7 * * MON-FRI")
    public void triggerOppdatering() {
		log.info("Mandag/onsdag/fredag kjøring av doksikkerhetsnett skrive-modus er startet");

        //Fjerner gammel data fra cachen
        metricsScheduler.clearOldMetrics();

        //Kjører read-only temaene
        runDoksikkerhetsnettInReadOnlyMode();

        //Oppretter oppgaver for write-temaene
        runDokSikkerhetsnettInReadWriteMode();
		log.info("Mandag/onsdag/fredag kjøring av doksikkerhetsnett skrive-modus er ferdig");
    }

    public void runDoksikkerhetsnettInReadOnlyMode() {
        if (dokSikkerhetsnettProperties.getLesTemaer() != null && dokSikkerhetsnettProperties.getLesTemaer().length() > 0) {
            Set<String> temaer = TEMA_ALLE.equals(dokSikkerhetsnettProperties.getLesTemaer()) ?
                    Utils.getAlleTemaExcept(dokSikkerhetsnettProperties.getSkrivTemaer()) : // Hent alle tema. Ignorer de som skal skrives, da de behandles neste steg
                    temaerStringToSet(dokSikkerhetsnettProperties.getLesTemaer());

            temaer.forEach(this::finnJournalposterUtenOppgave);

        } else {
            log.info("Det er ikke spesifisert noen temaer for read-only");
        }
    }

    public void runDokSikkerhetsnettInReadWriteMode() {
        if (dokSikkerhetsnettProperties.getSkrivTemaer() != null && dokSikkerhetsnettProperties.getSkrivTemaer().length() > 0) {
            Set<String> temaer = TEMA_ALLE.equals(dokSikkerhetsnettProperties.getSkrivTemaer()) ?
                    Utils.getAlleTema() : temaerStringToSet(dokSikkerhetsnettProperties.getSkrivTemaer());

            temaer.forEach(this::lagOppgaverForGlemteJournalposter);

        } else {
            log.info("Det er ikke spesifisert noen temaer å opprette oppgaver for");
        }
    }

    /*
    *  Satt til å kjøre klokken 6:30, mon-fri
    *  Lager grafana metrics på journalposter som er ubehandlede, uten oppgave og har ligget i minst x dager
    */
	@Scheduled(cron = "0 30 6 * * MON-FRI")
	public void updateDailyGrafanaMetrics() {
		log.info("Daglig kjøring av doksikkerhetsnett les-modus er startet");

		metricsScheduler.clearOneDayOldCache();
		metricsScheduler.clearTwoDaysOldCache();

		for (String tema : Utils.getAlleTema()) {
			List<Journalpost> ubehandledeJournalposterUtenOppgave = finnUbehandledeJournalposterUtenOppgaveForDagligeMetrics(tema, EN_DAG);
			metricsScheduler.incrementOneDayOldMetrics(ubehandledeJournalposterUtenOppgave);
		}

		for (String tema : Utils.getAlleTema()) {
			List<Journalpost> ubehandledeJournalposterUtenOppgave = finnUbehandledeJournalposterUtenOppgaveForDagligeMetrics(tema, TO_DAGER);
			metricsScheduler.incrementTwoDaysOldMetrics(ubehandledeJournalposterUtenOppgave);
		}

		log.info("Daglig kjøring av doksikkerhetsnett les-modus er ferdig");
	}

	private List<Journalpost> finnUbehandledeJournalposterUtenOppgaveForDagligeMetrics(String tema, int eldreEnn){
		List<Journalpost> ubehandledeJournalposter;
		log.info("Henter alle ubehandlede journalposter {} som er eldre enn {}.",
				Utils.logWithTema(tema),
				Utils.logWithDager(eldreEnn));

		ubehandledeJournalposter = finnUbehandledeJournalposter(tema, eldreEnn);
		return findUbehandledeJournalposterUtenOppgave(tema, ubehandledeJournalposter, eldreEnn);
	}

	public void lagOppgaverForGlemteJournalposter(String tema) {
		List<Journalpost> ubehandletJournalpostsUtenOppgave = finnJournalposterUtenOppgave(tema);
		try {
			List<OpprettOppgaveResponse> opprettedeOppgaver = opprettOppgaveService.opprettOppgaver(ubehandletJournalpostsUtenOppgave);
			if (!opprettedeOppgaver.isEmpty()) {
				log.info("Doksikkerhetsnett har opprettet {} oppgaver {} med ID'ene: {}", opprettedeOppgaver.size(), Utils.logWithTema(tema),
						opprettedeOppgaver.stream().map(OpprettOppgaveResponse::getId).collect(Collectors.toList()));
			}
		} catch (Exception e) {
			log.error("Doksikkerhetsnett feilet under oppretting av oppgaver {}", Utils.logWithTema(tema), e);
		}
	}

	public List<Journalpost> finnJournalposterUtenOppgave(String tema) {
		List<Journalpost> ubehandledeJournalposter;
		List<Journalpost> ubehandledeJournalposterUtenOppgave;
		log.info("Doksikkerhetsnett henter alle ubehandlede journalposter eldre enn 5 dager fra tema: {}", tema);

		ubehandledeJournalposter = finnUbehandledeJournalposter(tema, FEM_DAGER);
		ubehandledeJournalposterUtenOppgave = findUbehandledeJournalposterUtenOppgave(tema, ubehandledeJournalposter, FEM_DAGER);

		metricsScheduler.incrementMetrics(ubehandledeJournalposter, ubehandledeJournalposterUtenOppgave);
		return ubehandledeJournalposterUtenOppgave;
	}

	private List<Journalpost> findUbehandledeJournalposterUtenOppgave(String tema, List<Journalpost> ubehandledeJournalposter, int dagerGamle) {
		List<Journalpost> ubehandledeJournalposterUtenOppgave;
		ubehandledeJournalposterUtenOppgave = finnEksisterendeOppgaverFraUbehandledeJournalpostList(ubehandledeJournalposter);
		log.info("Fant {} journalposter {} som er eldre enn {} dag(er) og mangler oppgave. {}",
				ubehandledeJournalposterUtenOppgave.size(),
				Utils.logWithTema(tema), //Legger på teksten: med tema {tema}
				dagerGamle,
				ubehandledeJournalposterUtenOppgave.isEmpty() ? "" :
						"Journalpostene hadde ID'ene:" + ubehandledeJournalposterUtenOppgave + ".");
		return ubehandledeJournalposterUtenOppgave;
	}

	private List<Journalpost> finnUbehandledeJournalposter(String tema, int eldreEnn) {
		try {
			return finnMottatteJournalposterService.finnMottatteJournalPoster(tema, eldreEnn).getJournalposter();
		} catch (Exception e) {
			log.error("Doksikkerhetsnett feilet under hentingen av alle journalposter {}: " + e.getMessage(), Utils.logWithTema(tema), e);
			return Collections.emptyList();
		}
	}

	private ArrayList<Journalpost> finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<Journalpost> ubehandledeJournalpostList) {
		return fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList);
	}

	private ArrayList<Journalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<Journalpost> ubehandledeJournalpostList) {
		FinnOppgaveResponse oppgaveResponse = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList);

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

	private Set<String> temaerStringToSet(String temaer) {
		return new HashSet(Arrays.asList(temaer.split(",")).stream()
				.map(String::trim)
				.collect(Collectors.toList()));
	}
}