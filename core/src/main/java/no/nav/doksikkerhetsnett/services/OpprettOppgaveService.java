package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.exceptions.functional.PdlFunctionalException;
import no.nav.doksikkerhetsnett.consumers.pdl.PdlIdentConsumer;
import no.nav.doksikkerhetsnett.consumers.pdl.PersonIkkeFunnetException;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.JiraResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static no.nav.doksikkerhetsnett.entities.Bruker.TYPE_PERSON;
import static no.nav.doksikkerhetsnett.entities.Oppgave.BESKRIVELSE_GJENOPPRETTET;
import static no.nav.doksikkerhetsnett.entities.Oppgave.ENHETSNUMMER_GENERISK;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_FORDELING;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_JOURNALFOERT;
import static no.nav.doksikkerhetsnett.entities.Oppgave.PRIORITET_NORMAL;
import static no.nav.doksikkerhetsnett.entities.Oppgave.TEMA_PENSJON;
import static no.nav.doksikkerhetsnett.mappers.OppgaveTemaMapper.mapJpTemaToOppgaveTema;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Slf4j
@Service
public class OpprettOppgaveService {

	private final JiraConsumer jiraConsumer;
	private final PdlIdentConsumer pdlIdentConsumer;
	private final OpprettOppgaveConsumer opprettOppgaveConsumer;

	public OpprettOppgaveService(JiraConsumer jiraConsumer,
								 PdlIdentConsumer pdlIdentConsumer,
								 OpprettOppgaveConsumer opprettOppgaveConsumer) {
		this.jiraConsumer = jiraConsumer;
		this.pdlIdentConsumer = pdlIdentConsumer;
		this.opprettOppgaveConsumer = opprettOppgaveConsumer;
	}

	public List<OpprettOppgaveResponse> opprettOppgaver(List<Journalpost> journalposts) {
		return journalposts.stream()
				.map(jp -> opprettOppgave(createOppgaveFromJournalpost(jp)))
				.filter(Objects::nonNull)
				.toList();
	}

	public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {
		try {
			log.info("Prøver å opprette en oppgave med journalpostId={}", oppgave.getJournalpostId());
			return opprettOppgaveConsumer.opprettOppgave(oppgave);
		} catch (HttpClientErrorException e) {
			return opprettOppgaveMedLiteMetadata(oppgave);
		} catch (Exception e) {
			log.error("Feil oppstod i opprettOppgave for journalpostId={}", oppgave.getJournalpostId(), e);
			return null;
		}
	}

	public OpprettOppgaveResponse opprettOppgaveMedLiteMetadata(Oppgave oppgave) {
		try {
			log.info(
					"Klarte ikke opprette oppgave med oppgavetype {}. Prøver å opprette oppgave med oppgavetype={} med journalpostId={}",
					oppgave.getOppgavetype(),
					(TEMA_PENSJON.equals(oppgave.getTema()) ? OPPGAVETYPE_JOURNALFOERT : OPPGAVETYPE_FORDELING),
					oppgave.getJournalpostId()
			);
			return opprettOppgaveConsumer.opprettOppgave(createMinimalOppgaveFromJournalpost(oppgave, TEMA_PENSJON.equals(oppgave.getTema()) ? OPPGAVETYPE_JOURNALFOERT : OPPGAVETYPE_FORDELING));
		} catch (HttpClientErrorException e) {
			JiraResponse response = jiraConsumer.opprettJiraIssue(oppgave, e);
			log.info("Doksikkerhetsnett opprettet en jira-issue med kode {}", response.getKey());
			return null;
		}
	}

	private Oppgave createOppgaveFromJournalpost(Journalpost jp) {
		String tildeltEnhetsnr = extractEnhetsNr(jp);
		String tema = mapJpTemaToOppgaveTema(jp);

		return Oppgave.builder()
				.tildeltEnhetsnr(tildeltEnhetsnr)
				.opprettetAvEnhetsnr(ENHETSNUMMER_GENERISK)
				.journalpostId(Long.toString(jp.getJournalpostId()))
				.tema(tema)
				.behandlingstema(jp.getBehandlingstema())
				.oppgavetype(OPPGAVETYPE_JOURNALFOERT)
				.prioritet(PRIORITET_NORMAL)
				.aktivDato(new Date())
				.aktoerId(this.findAktorId(jp))
				.beskrivelse(BESKRIVELSE_GJENOPPRETTET)
				.fristFerdigstillelse(new Date())
				.build();
	}

	private Oppgave createMinimalOppgaveFromJournalpost(Oppgave gammelOppgave, String oppgavetype) {
		return Oppgave.builder()
				.tildeltEnhetsnr(null)
				.opprettetAvEnhetsnr(ENHETSNUMMER_GENERISK)
				.journalpostId(gammelOppgave.getJournalpostId())
				.tema(gammelOppgave.getTema())
				.behandlingstema(null)
				.oppgavetype(oppgavetype)
				.prioritet(PRIORITET_NORMAL)
				.aktivDato(new Date())
				.aktoerId(gammelOppgave.getAktoerId())
				.beskrivelse(gammelOppgave.getBeskrivelse())
				.fristFerdigstillelse(new Date())
				.build();
	}

	private String findAktorId(Journalpost jp) {
		String fnr = jp.getBruker() != null ? jp.getBruker().getId() : null;

		if (isNotBlank(fnr) && TYPE_PERSON.equals(jp.getBruker().getType())) {
			try {
				log.info("Fant en aktorId tilknyttet til journalpost med journalpostId={}", jp.getJournalpostId());
				return pdlIdentConsumer.hentAktoerId(fnr);
			} catch (PersonIkkeFunnetException | PdlFunctionalException | HttpServerErrorException e) {
				log.warn("Det skjedde en feil i kallet til PDL, eller bruker ikke funnet i PDL med journalpostId: {}. Feilmelding: ", jp.getJournalpostId(), e);
			}
		}
		return null;
	}

	private String extractEnhetsNr(Journalpost jp) {
		return ENHETSNUMMER_GENERISK.equals(jp.getJournalforendeEnhet()) ? "" : jp.getJournalforendeEnhet();
	}
}
