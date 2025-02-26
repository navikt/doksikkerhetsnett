package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.metrics.MetricsService;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.utils.Tema;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;

@Slf4j
@Component
public class OppdaterMetricsScheduled {

	private final MetricsService metricsService;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int EN_DAG = 1;
	private static final int TO_DAGER = 2;


	public OppdaterMetricsScheduled(MetricsService metricsService,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.metricsService = metricsService;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	/*
	 *  Satt til å kjøre klokken 6:30, mon-fri
	 *  Lager grafana metrics på journalposter som er ubehandlede, uten oppgave og har ligget i minst x dager
	 */
	@Scheduled(cron = "0 25 10 * * MON-FRI")
	public void updateDailyGrafanaMetrics() {
		try {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
			log.info("Daglig kjøring av doksikkerhetsnett les-modus er startet");

			metricsService.clearOldMetrics();


			for (String tema : Tema.getAlleTema()) {
				try {
					int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, EN_DAG).size();
					log.info("Fant {} journalposter uten oppgave som var {} dag eller eldre.", antall, EN_DAG);
				} catch (Exception e) {
					log.error("Klarte ikke å oppdatere daglige metrics for tema={}", tema, e);
				}
			}

			for (String tema : Tema.getAlleTema()) {
				try {
					int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, TO_DAGER).size();
					log.info("Fant {} journalposter uten oppgave som var {} dager eller eldre.", antall, TO_DAGER);
				} catch (Exception e) {
					log.error("Klarte ikke å oppdatere 2-dagers metrics for tema={}", tema, e);
				}
			}
			log.info("Daglig kjøring av doksikkerhetsnett les-modus er ferdig");
		} finally {
			MDC.clear();
		}
	}

}