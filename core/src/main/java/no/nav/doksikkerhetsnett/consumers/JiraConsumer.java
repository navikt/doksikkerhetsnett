package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.dok.jiraapi.JiraRequest;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.dok.jiracore.exception.JiraClientException;
import no.nav.dok.jiracore.interndomain.Issue;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Slf4j
@Component
public class JiraConsumer {

	private static final String PROJECT_KEY = "MMA";
	private static final String ISSUETYPE_NAME = "Oppgave";
	private static final List<String> LABELS = asList("morgenvakt", "doksikkerhetsnett");

	private final String opprettJiraIssueUrl;
	private final JiraClient jiraClient;

	public JiraConsumer(DokSikkerhetsnettProperties dokSikkerhetsnettProperties, JiraClient jiraClient) {
		this.jiraClient = jiraClient;
		this.opprettJiraIssueUrl = dokSikkerhetsnettProperties.getEndpoints().getJira();
	}

	public String opprettJiraIssue(Oppgave oppgave, WebClientResponseException exception) {
		try {
			JiraRequest jiraRequest = createIssue(oppgave, exception);
			log.info("doksikkerhetsnett prøver å lage en jira-issue i prosjekt {} med tittel \"{}\"", PROJECT_KEY, jiraRequest.summary());

			Issue issue = jiraClient.opprettJira(jiraRequest, PROJECT_KEY, issueType -> ISSUETYPE_NAME.equalsIgnoreCase(issueType.name()), Issue.class, Stream.empty());

			return issue.key();
		} catch (JiraClientException e) {
			throw new FinnOppgaveFinnesIkkeFunctionalException(format("OpprettJiraIssue feilet funksjonelt med feilmelding=%s. Url=%s",
					e.getMessage(), opprettJiraIssueUrl), e);
		}
	}

	private JiraRequest createIssue(Oppgave oppgave, WebClientResponseException e) {
		return JiraRequest.builder()
				.labels(LABELS)
				.summary("Doksikkerhetsnett feilet i å opprette oppgave")
				.description(formatDescription(oppgave, e))
				.build();
	}

	private String formatDescription(Oppgave oppgave, WebClientResponseException e) {
		return """
				Doksikkerhetsnett prøvde å lage en oppgave for den ubehandlede journalposten med id %s og tema %s.
				Forsøkt opprettet oppgave så slik ut:
				%s
				
				Oppgave-api kastet denne feilmeldingen:
				%s
				""".formatted(
				oppgave.getJournalpostId(),
				oppgave.getTema(),
				prettifyOppgave(oppgave),
				e.getResponseBodyAsString());
	}

	private String prettifyOppgave(Oppgave oppgave) {
		return "tildeltEnhetsnr: " + oppgave.getTildeltEnhetsnr()
			   + "\nopprettetAvEnhetsnr: " + oppgave.getOpprettetAvEnhetsnr()
			   + "\njournalpostId: " + oppgave.getJournalpostId()
			   + "\norgnr: " + oppgave.getOrgnr()
			   + "\nbnr: " + oppgave.getBnr()
			   + "\ntema: " + oppgave.getTema()
			   + "\nbehandlingstema: " + oppgave.getBehandlingstema()
			   + "\noppgavetype: " + oppgave.getOppgavetype()
			   + "\nprioritet: " + oppgave.getPrioritet()
			   + "\naktivDato: " + oppgave.getAktivDato();
	}
}
