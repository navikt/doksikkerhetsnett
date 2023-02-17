package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.OpprettOppgaverScheduled;
import no.nav.doksikkerhetsnett.itest.config.DoksikkerhetsnettItest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


class OpprettOppgaverScheduledItest extends DoksikkerhetsnettItest {


	@Autowired
	OpprettOppgaverScheduled opprettOppgaverScheduled;

	@Test
	void shouldCreateJiraIssueWhenOpprettOppgaveFails() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
		stubFor(post(urlMatching(URL_JIRA))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));

		opprettOppgaverScheduled.lagOppgaverForGlemteJournalposter(dokSikkerhetsnettProperties.getSkrivTemaer());

		WireMock.verify(exactly(4), postRequestedFor(urlMatching(URL_JIRA)));
	}

}