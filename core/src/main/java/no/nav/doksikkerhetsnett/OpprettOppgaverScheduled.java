package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.utils.Tema.temaerStringToSet;

@Slf4j
@Component
public class OpprettOppgaverScheduled {

	private final OpprettOppgaveService opprettOppgaveService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int FEM_DAGER = 5;

	public OpprettOppgaverScheduled(OpprettOppgaveService opprettOppgaveService,
									DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.opprettOppgaveService = opprettOppgaveService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	// Satt til å kjøre klokken 07:00 man - fre
	@Scheduled(cron = "0 10 10 0 * MON-FRI")
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
				log.info("Doksikkerhetsnett har opprettet {} oppgaver for tema {} med ID'ene: {}", opprettedeOppgaver.size(), tema,
						opprettedeOppgaver.stream().map(OpprettOppgaveResponse::getId).toList());
			}
		} catch (Exception e) {
			log.error("Doksikkerhetsnett feilet under oppretting av oppgaver for tema={}", tema, e);
		}
	}

}
