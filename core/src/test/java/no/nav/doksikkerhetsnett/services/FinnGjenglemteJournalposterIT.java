package no.nav.doksikkerhetsnett.services;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.services.config.DoksikkerhetsnettItest;
import no.nav.doksikkerhetsnett.services.config.TestConfig;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
		assertEquals(260, journalposterUtenOppgaver.size());
	}

	@Test
	void shouldPartitionRequestsToFinnOppgave() {

		stubFor(get(urlPathEqualTo(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposter51elementer-happy.json")));

		stubFor(get(urlMatching(URL_OPPGAVE + ".*"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-happy.json")));

		var result = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics("UFO", 5);

		assertThat(result).hasSize(51);

		var request = WireMock.findAll(getRequestedFor(urlMatching(URL_OPPGAVE + ".*")));

		var antallJournalpostIdRequestParamsPrRequest = request.stream()
				.map(LoggedRequest::getQueryParams)
				.map(queryParameters -> queryParameters.get("journalpostId").values().size())
				.toList();

		assertThat(antallJournalpostIdRequestParamsPrRequest).containsExactly(50, 1);
	}

	@Test
	void shouldFinnOppgaveWhen50DuplikateOppgaver() {
		stubFor(get(urlPathEqualTo(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposterMedTemaSingle-happy.json")));

		stubFor(get(urlMatching(URL_OPPGAVE + "\\?journalpostId=111111111&journalpostId=222222222&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&limit=50"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-50-duplikater-offset0.json")));
		stubFor(get(urlMatching(URL_OPPGAVE + "\\?journalpostId=111111111&journalpostId=222222222&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&limit=50&offset=50"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-50-duplikater-offset50.json")));

		var result = finnGjenglemteJournalposterService.finnJournalposterUtenOppgaveUpdateMetrics("UFO", 5);

		assertThat(result).hasSize(0);
	}

	private void assertCorrectMetrics(Map<String, Integer> metricsCache, int expectedValue, String expectedString) {
		assertEquals(expectedValue, (int) metricsCache.entrySet().iterator().next().getValue());
		assertEquals(expectedString, metricsCache.entrySet().iterator().next().getKey());
	}
}