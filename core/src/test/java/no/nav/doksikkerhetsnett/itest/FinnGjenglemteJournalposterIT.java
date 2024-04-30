package no.nav.doksikkerhetsnett.itest;

import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.itest.config.DoksikkerhetsnettItest;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService;
import no.nav.doksikkerhetsnett.utils.Tema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class FinnGjenglemteJournalposterIT extends DoksikkerhetsnettItest {

	@Autowired
	private FinnGjenglemteJournalposterService finnGjenglemteJournalposterService;

	@Test
	void shouldFindMottatteJournalposter() {
		List<Journalpost> journalposterUtenOppgaver = new ArrayList<>();
		Arrays.stream(dokSikkerhetsnettProperties.getSkrivTemaer().split(","))
				.forEach(tema -> journalposterUtenOppgaver.addAll(finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, 5)));
		assertEquals(4, journalposterUtenOppgaver.size());

		Map<String, Integer> totalMetricsCache = metricsScheduler.getCaches().get(0);
		Map<String, Integer> utenOppgaveMetricsCache = metricsScheduler.getCaches().get(1);
		assertCorrectMetrics(totalMetricsCache, 6, METRIC_TAGS);
		assertCorrectMetrics(utenOppgaveMetricsCache, 4, METRIC_TAGS);
	}

	@Test
	void shouldFindMottatteJournalposterForAlleTema() {
		List<Journalpost> journalposterUtenOppgaver = new ArrayList<>();
		Tema.getAlleTema()
				.forEach(tema -> journalposterUtenOppgaver.addAll(finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics(tema, 5)));
		assertEquals(244, journalposterUtenOppgaver.size());
	}

	@Test
	void shouldPartitionRequestsToFinnOppgave() {

		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposter51elementer-happy.json")));

		stubFor(get(urlMatching(URL_OPPGAVE + ".*"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-happy.json")));

		var result = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics("UFO", 5);

		assertThat(result).hasSize(51);

		verify(2, getRequestedFor(urlMatching(URL_OPPGAVE + ".*")));

	}

	private void assertCorrectMetrics(Map<String, Integer> metricsCache, int expectedValue, String expectedString) {
		assertEquals(expectedValue, (int) metricsCache.entrySet().iterator().next().getValue());
		assertEquals(expectedString, metricsCache.entrySet().iterator().next().getKey());
	}
}