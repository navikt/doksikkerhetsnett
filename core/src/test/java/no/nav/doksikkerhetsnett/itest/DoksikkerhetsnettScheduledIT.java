package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.DoksikkerhetsnettScheduled;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class DoksikkerhetsnettScheduledIT {
	private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/.{3}/.{1}";
	private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
	private static final String URL_OPPGAVE_JOURNALPOST_SEARCH = "/api/v1/oppgaver\\?journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";
	private static final String URL_OPPGAVE = "/api/v1/oppgaver";
	private static final String URL_JIRA = "/rest/api/2/issue";
	private static final String URL_PDL = "/pdl";
	private static final String METRIC_TAGS = "UFO;ALTINN;0000";

	@Autowired
	private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

	@Autowired
	private MetricsScheduler metricsScheduler;

	@Autowired
	private DoksikkerhetsnettScheduled doksikkerhetsnettScheduled;

	@BeforeEach
	void setup(){
		setUpStubs();
	}

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	void setUpStubs() {
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
		stubFor(get(urlMatching(URL_OPPGAVE_JOURNALPOST_SEARCH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-aktoerid-happy.json")));
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@Test
	void shouldFindMottatteJournalposter() {
		List<Journalpost> journalposterUtenOppgaver = new ArrayList();
		Arrays.stream(dokSikkerhetsnettProperties.getSkrivTemaer().split(","))
				.forEach(tema -> journalposterUtenOppgaver.addAll(doksikkerhetsnettScheduled.finnJournalposterUtenOppgave(tema)));
		assertEquals(4, journalposterUtenOppgaver.size());

		Map<String, Integer> totalMetricsCache = metricsScheduler.getCaches().get(0);
		Map<String, Integer> utenOppgaveMetricsCache = metricsScheduler.getCaches().get(1);
		assertCorrectMetrics(totalMetricsCache, 6, METRIC_TAGS);
		assertCorrectMetrics(utenOppgaveMetricsCache, 4, METRIC_TAGS);
	}

	@Test
	void shouldFindMottatteJournalposterForAlleTema() {
		List<Journalpost> journalposterUtenOppgaver = new ArrayList();
		Utils.getAlleTema()
				.forEach(tema -> journalposterUtenOppgaver.addAll(doksikkerhetsnettScheduled.finnJournalposterUtenOppgave(tema)));
		assertEquals(228, journalposterUtenOppgaver.size());
	}

	@Test
	void shouldCreateJiraIssueWhenOpprettOppgaveFails() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
		stubFor(post(urlMatching(URL_JIRA))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));

		doksikkerhetsnettScheduled.lagOppgaverForGlemteJournalposter(dokSikkerhetsnettProperties.getSkrivTemaer());

		WireMock.verify(exactly(4), postRequestedFor(urlMatching(URL_JIRA)));
	}

	private void assertCorrectMetrics(Map<String, Integer> metricsCache, int expectedValue, String expectedString) {
		assertEquals(expectedValue, (int) metricsCache.entrySet().iterator().next().getValue());
		assertEquals(expectedString, metricsCache.entrySet().iterator().next().getKey());
	}
}