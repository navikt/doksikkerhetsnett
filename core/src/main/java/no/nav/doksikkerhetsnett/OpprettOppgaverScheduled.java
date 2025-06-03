package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import no.nav.doksikkerhetsnett.services.SlackService;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.utils.Tema.temaerStringToSet;

@Slf4j
@Component
public class OpprettOppgaverScheduled {

	private final SlackService slackService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final OpprettOppgaveService opprettOppgaveService;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int FEM_DAGER = 5;

	public OpprettOppgaverScheduled(SlackService slackService,
									DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									OpprettOppgaveService opprettOppgaveService,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.slackService = slackService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.opprettOppgaveService = opprettOppgaveService;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	// Satt til å kjøre klokken 07:00 man - fre
	@Scheduled(cron = "0 0 7 * * MON-FRI")
	public void opprettOppgaverForGjenglemteJournalposter() {
		try {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
			log.info("Starter den daglige skriv-kjøringen (man-fre)");

			temaerStringToSet(dokSikkerhetsnettProperties.getSkrivTemaer())
					.forEach(this::lagOppgaverForGlemteJournalposter);

			log.info("Avslutter den daglige skriv-kjøringen (man-fre)");
		} finally {
			MDC.clear();
		}
	}

	public void lagOppgaverForGlemteJournalposter(String tema) {
		try {
			List<Journalpost> ubehandletJournalpostsUtenOppgave = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, FEM_DAGER);
			List<OpprettOppgaveResponse> opprettedeOppgaver = opprettOppgaveService.opprettOppgaver(ubehandletJournalpostsUtenOppgave);

			if (!opprettedeOppgaver.isEmpty()) {
				log.info("Doksikkerhetsnett har opprettet {} oppgaver for tema {} med ID'ene: {}",
						opprettedeOppgaver.size(), tema, opprettedeOppgaver.stream().map(OpprettOppgaveResponse::getId).toList());
			}
		} catch (Exception e) {
			var feilmelding = "OpprettOppgave cron-jobb feilet for tema=%s med feilmelding=%s".formatted(tema, e.getMessage());

			log.error(feilmelding, e);
			slackService.sendMelding(feilmelding);
		}
	}

}
