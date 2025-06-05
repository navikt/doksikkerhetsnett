package no.nav.doksikkerhetsnett.itest;

import no.nav.doksikkerhetsnett.OpprettOppgaverScheduled;
import no.nav.doksikkerhetsnett.itest.config.DoksikkerhetsnettItest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class OpprettOppgaverScheduledIT extends DoksikkerhetsnettItest {

	@Autowired
	OpprettOppgaverScheduled opprettOppgaverScheduled;

	@Test
	void shouldCreateJiraIssueWhenOpprettOppgaveFails() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
		stubFor(post(urlMatching(URL_JIRA))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));

		opprettOppgaverScheduled.opprettOppgaverForGjenglemteJournalposter();

		verify(exactly(4), postRequestedFor(urlMatching(URL_JIRA)));
	}

}