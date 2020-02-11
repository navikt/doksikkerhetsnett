package no.nav.doksikkerhetsnett.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumer.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.UbehandletJournalpost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import no.nav.doksikkerhetsnett.itest.config.FinnMottateJournalposterTestConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {FinnMottateJournalposterTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class FinnMottatteJournalposterIT {

	private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/";
	private static final String TEMA_SINGLE = "UFO";
	private static final String TEMA_MULTI = "UFO,PEN,BAR";
	private static final String TEMA_NONE = "";
	private static final String TEMA_INVALID = "INVALID_TEMA";
	private static final ArrayList<Long> JOURNALPOSTIDS = new ArrayList<>(List.of(111111111L, 222222222L));
	private static final ArrayList<String> PERSONIDS = new ArrayList<>(List.of("33333333333", "44444444444"));
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Autowired
	private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void finnMottatteJournalposterHappyPathTemaSingle() {
		AssertFinnMottateJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_SINGLE, 2,
				"mottatteJournalposterMedTemaSingle-happy.json", TEMA_SINGLE);
	}

	@Test
	public void finnMottatteJournalposterHappyPathTemaMulti() {
		AssertFinnMottateJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_MULTI, 2,
				"mottatteJournalposterMedTemaMulti-happy.json", "UFO", "BAR");
	}

	@Test
	public void finnMottatteJournalposterHappyPathTemaNone() {
		AssertFinnMottateJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_NONE, 2,
				"mottatteJournalposterMedTemaNone-happy.json", "UFO", "PEN");
	}

	@Test
	public void finnMottatteJournalposterInvalidTema() {
		AssertFinnMottateJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_INVALID, 0,
				"mottatteJournalposterMedinvalidTema.json", "");
	}

	private void AssertFinnMottateJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(String temaer, int expectedOutcome, String filename, String... resultTemaer) {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + temaer))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnMottatteJournalposter/" + filename)));

		ArrayList<Date> datoOpprettet = new ArrayList<>();
		try {
			datoOpprettet.add(dateFormatter.parse("2014-04-11T14:11:34.622+0000"));
			datoOpprettet.add(dateFormatter.parse("2015-12-28T15:11:05.455+0000"));
		} catch (Exception e) {
		}

		FinnMottatteJournalposterConsumer consumer = new FinnMottatteJournalposterConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties);
		FinnMottatteJournalposterResponse response = consumer.finnMottateJournalposter(temaer);

		assertEquals(expectedOutcome, response.getJournalposter().size());

		for (int i = 0; i < response.getJournalposter().size(); i++) {
			UbehandletJournalpost ubehandletJournalpost = response.getJournalposter().get(i);
			assertEquals(JOURNALPOSTIDS.get(i), (Long) ubehandletJournalpost.getJournalpostId());
			assertEquals("M", ubehandletJournalpost.getJournalStatus());
			assertEquals("ALTINN", ubehandletJournalpost.getMottaksKanal());
			assertEquals(PERSONIDS.get(i), ubehandletJournalpost.getBruker().getId());
			assertEquals("PERSON", ubehandletJournalpost.getBruker().getType());
			assertTrue(assertValidTema(ubehandletJournalpost, resultTemaer));
			assertEquals("ab0001", ubehandletJournalpost.getBehandlingstema());
			assertEquals("0000", ubehandletJournalpost.getJournalforendeEnhet());
			assertEquals(datoOpprettet.get(i), ubehandletJournalpost.getDatoOpprettet());
		}
	}

	private boolean assertValidTema(UbehandletJournalpost ubehandletJournalpost, String... resultTemaer) {
		for (String validTemas : resultTemaer) {
			if (validTemas.equals(ubehandletJournalpost.getTema())) {
				return true;
			}
		}
		return false;
	}
}
