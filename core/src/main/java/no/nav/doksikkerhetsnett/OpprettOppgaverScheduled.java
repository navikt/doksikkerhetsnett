package no.nav.doksikkerhetsnett;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.utils.Utils.temaerStringToSet;

@Slf4j
@Component
public class OpprettOppgaverScheduled {

	private final OpprettOppgaveService opprettOppgaveService;
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	private static final int FEM_DAGER = 5;
	private static final String TEMA_ALLE = "ALLE";

	public OpprettOppgaverScheduled(OpprettOppgaveService opprettOppgaveService,
									DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
									FinnGjenglemteJournalposterService finnGjenglemteJournalposterService) {
		this.opprettOppgaveService = opprettOppgaveService;
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.finnGjenglemteJournalposterService = finnGjenglemteJournalposterService;
	}

	// Satt til ? k?jre klokken 07:00 man - fre
	@Scheduled(cron = "0 0 7 * * MON-FRI")
	public void doOpprettOppgaverForGjenglemteJournalposter() {
		log.info("Starter den daglige skriv-kj?ringen (man-fre)");
		if (dokSikkerhetsnettProperties.getSkrivTemaer() != null && dokSikkerhetsnettProperties.getSkrivTemaer().length() > 0) {
			Set<String> temaer = TEMA_ALLE.equals(dokSikkerhetsnettProperties.getSkrivTemaer()) ?
					Utils.getAlleTema() : temaerStringToSet(dokSikkerhetsnettProperties.getSkrivTemaer());

			temaer.forEach(this::lagOppgaverForGlemteJournalposter);

		} else {
			log.info("Det er ikke spesifisert noen temaer ? opprette oppgaver for");
		}
		log.info("Avslutter den daglige skriv-kj?ringen (man-fre)");
	}

	public void lagOppgaverForGlemteJournalposter(String tema) {
		try {
			List<Journalpost> ubehandletJournalpostsUtenOppgave = finnGjenglemteJournalposterService.finnJournalposterUtenOppgave(tema, FEM_DAGER);
			List<OpprettOppgaveResponse> opprettedeOppgaver = opprettOppgaveService.opprettOppgaver(ubehandletJournalpostsUtenOppgave);
			if (!opprettedeOppgaver.isEmpty()) {
				log.info("Doksikkerhetsnett har opprettet {} oppgaver for tema {} med ID'ene: {}", opprettedeOppgaver.size(), tema,
						opprettedeOppgaver.stream().map(OpprettOppgaveResponse::getId).collect(Collectors.toList()));
			}
		} catch (Exception e) {
			log.error("Doksikkerhetsnett feilet under oppretting av oppgaver for tema={}", tema, e);
		}
	}

}
