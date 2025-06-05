package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.metrics.MetricsService;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.services.SlackService;
import no.nav.doksikkerhetsnett.utils.Tema;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;

@Slf4j
@Component
public class OppdaterMetricsScheduled {

	private final SlackService slackService;
	private final MetricsService metricsService;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int EN_DAG = 1;
	private static final int TO_DAGER = 2;

	public OppdaterMetricsScheduled(SlackService slackService,
									MetricsService metricsService,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.slackService = slackService;
		this.metricsService = metricsService;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	/*
	 *  Satt til å kjøre klokken 6:30, mon-fri
	 *  Lager grafana metrics på journalposter som er ubehandlede, uten oppgave og har ligget i minst x dager
	 */
	@Scheduled(cron = "0 30 6 * * MON-FRI")
	public void updateDailyGrafanaMetrics() {
		try {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
			log.info("Daglig kjøring av doksikkerhetsnett les-modus er startet");

			metricsService.clearOldMetrics();

			AtomicInteger antallFeiledeOppdateringerFor1Dagsmetrikk = new AtomicInteger(0);
			for (String tema : Tema.getAlleTema()) {
				try {
					int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, EN_DAG).size();
					log.info("Fant {} journalposter uten oppgave som var {} dag eller eldre.", antall, EN_DAG);
				} catch (Exception e) {
					log.error("Klarte ikke å oppdatere 1-dagsmetrikk for tema={}", tema, e);

					antallFeiledeOppdateringerFor1Dagsmetrikk.getAndIncrement();
				}
			}

			AtomicInteger antallFeiledeOppdateringerFor2Dagsmetrikk = new AtomicInteger(0);
			for (String tema : Tema.getAlleTema()) {
				try {
					int antall = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, TO_DAGER).size();
					log.info("Fant {} journalposter uten oppgave som var {} dager eller eldre.", antall, TO_DAGER);
				} catch (Exception e) {
					log.error("Klarte ikke å oppdatere 2-dagsmetrikk for tema={}", tema, e);

					antallFeiledeOppdateringerFor2Dagsmetrikk.getAndIncrement();
				}
			}

			if (antallFeiledeOppdateringerFor1Dagsmetrikk.get() > 0 || antallFeiledeOppdateringerFor2Dagsmetrikk.get() > 0) {
				var feilmelding = "OppdaterMetrikker cron-jobb feilet med å oppdatere 1-dagsmetrikk for %s tema og 2-dagsmetrikk for %s tema."
						.formatted(antallFeiledeOppdateringerFor1Dagsmetrikk.get(), antallFeiledeOppdateringerFor2Dagsmetrikk.get());
				log.error(feilmelding);
				slackService.sendMelding(feilmelding);
			}

			log.info("Daglig kjøring av doksikkerhetsnett les-modus er ferdig");
		} finally {
			MDC.clear();
		}
	}

}