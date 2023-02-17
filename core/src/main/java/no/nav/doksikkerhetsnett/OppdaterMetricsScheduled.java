package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.metrics.MetricsUtil;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OppdaterMetricsScheduled {

	private final MetricsUtil metricsScheduler;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int EN_DAG = 1;
	private static final int TO_DAGER = 2;


	public OppdaterMetricsScheduled(MetricsUtil metricsScheduler,
									DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.metricsScheduler = metricsScheduler;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	/*
	 *  Satt til å kjøre klokken 6:30, mon-fri
	 *  Lager grafana metrics på journalposter som er ubehandlede, uten oppgave og har ligget i minst x dager
	 */
	@Scheduled(cron = "0 30 6 * * MON-FRI")
	public void doUpdateDailyGrafanaMetrics() {
		log.info("Daglig kjøring av doksikkerhetsnett les-modus er startet");

		metricsScheduler.clearOldMetrics();


		for (String tema : Utils.getAlleTema()) {
			try {
				int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgave(tema, EN_DAG).size();
				log.info("Fant {} journalposter uten oppgave som var èn dag eller eldre.", antall);
			} catch (Exception e){
				log.error("Klarte ikke å oppdatere daglige metrics for tema={}", tema, e);
			}
		}

		for (String tema : Utils.getAlleTema()) {
			try {
				int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgave(tema, TO_DAGER).size();
				log.info("Fant {} journalposter uten oppgave som var 2 dager eller eldre.", antall);
			} catch (Exception e){
				log.error("Klarte ikke å oppdatere 2-dagers metrics for tema={}", tema, e);
			}
		}

		log.info("Daglig kjøring av doksikkerhetsnett les-modus er ferdig");
	}

}