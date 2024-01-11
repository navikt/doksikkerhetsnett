package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.jira.Fields;
import no.nav.doksikkerhetsnett.entities.jira.Issue;
import no.nav.doksikkerhetsnett.entities.jira.Issuetype;
import no.nav.doksikkerhetsnett.entities.jira.Project;
import no.nav.doksikkerhetsnett.entities.responses.JiraResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class JiraConsumer {

	private static final String APP_NAME = "doksikkerhetsnett";
	public static final String CORRELATION_HEADER = "X-Correlation-Id";
	public static final String UUID_HEADER = "X-Uuid";
	private static final String PROJECT_KEY = "ADMKDL";
	private static final String ISSUETYPE_NAME = "Avvik";
	private static final List<String> LABELS = asList("morgenvakt", "doksikkerhetsnett");

	private final RestTemplate restTemplate;
	private final String opprettJiraIssueUrl;

	public JiraConsumer(RestTemplateBuilder restTemplateBuilder, DokSikkerhetsnettProperties dokSikkerhetsnettProperties) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(250))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(), dokSikkerhetsnettProperties.getServiceuser().getPassword())
				.build();
		this.opprettJiraIssueUrl = dokSikkerhetsnettProperties.getEndpoints().getOpprettjiraissue();
	}

	public JiraResponse opprettJiraIssue(Oppgave oppgave, HttpClientErrorException exception) {
		try {
			HttpHeaders headers = createHeaders();
			Issue issue = createIssue(oppgave, exception);
			HttpEntity<Issue> requestEntity = new HttpEntity<>(issue, headers);
			log.info("doksikkerhetsnett prøver å lage en jira-issue i prosjekt {} med tittel \"{}\"", PROJECT_KEY, issue.getFields().getSummary());
			return restTemplate.exchange(opprettJiraIssueUrl, POST, requestEntity, JiraResponse.class)
					.getBody();
		} catch (HttpClientErrorException e) {
			throw new FinnOppgaveFinnesIkkeFunctionalException(format("OpprettJiraIssue feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s",
					e.getStatusCode(), e.getResponseBodyAsString(), opprettJiraIssueUrl), e);
		} catch (HttpServerErrorException e) {
			throw new FinnOppgaveTechnicalException(format("OpprettJiraIssue feilet teknisk med statusKode=%s. Feilmelding=%s",
					e.getStatusCode(), e.getMessage()), e);
		}
	}

	private Issue createIssue(Oppgave oppgave, HttpClientErrorException e) {
		return Issue.builder()
				.fields(Fields.builder()
						.project(Project.builder()
								.key(PROJECT_KEY)
								.build())
						.issuetype(Issuetype.builder()
								.name(ISSUETYPE_NAME)
								.build())
						.labels(LABELS)
						.summary("Doksikkerhetsnett feilet med å opprette oppgave")
						.description("Doksikkerhetsnett prøvde å lage en oppgave for den ubehandlede journalposten med id " + oppgave.getJournalpostId() + " og tema " + oppgave.getTema() + ".\n"
									 + "Forsøkt opprettet oppgave så slik ut:\n"
									 + prettifyOppgave(oppgave) + "\n\n"
									 + "Oppgave-api kastet denne feilmeldingen:\n"
									 + e.getResponseBodyAsString())
						.build())
				.build();
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

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(APPLICATION_JSON);
		headers.add(CORRELATION_HEADER, MDC.get(MDC_CALL_ID));
		headers.add(UUID_HEADER, MDC.get(MDC_CALL_ID));
		headers.add(MDC_NAV_CONSUMER_ID, APP_NAME);
		headers.add(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
		return headers;
	}
}
