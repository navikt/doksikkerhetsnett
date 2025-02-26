package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class FinnMottatteJournalposterIT {

	private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/journalpostapi/v1/finnMottatteJournalposter/";
	private static final String TEMA_SINGLE = "UFO";
	private static final String TEMA_MULTI = "UFO,PEN,BAR";
	private static final String TEMA_NONE = "";
	private static final String TEMA_INVALID = "INVALID_TEMA";
	private static final ArrayList<Long> JOURNALPOSTIDS = new ArrayList<>(List.of(111111111L, 222222222L));
	private static final ArrayList<String> PERSONIDS = new ArrayList<>(List.of("33333333333", "44444444444"));
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final String JUST_FOR_PATH = "does_not_matter";

	@Autowired
	private FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer;

	@BeforeEach
	void setup() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	void shouldFindMottatteJournalposterHappyPathTemaSingle() throws ParseException {
		assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_SINGLE, 2,
				"mottatteJournalposterMedTemaSingle-happy.json", TEMA_SINGLE);
	}

	@Test
	void finnMottatteJournalposterHappyPathTemaMulti() throws ParseException {
		assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_MULTI, 2,
				"mottatteJournalposterMedTemaMulti-happy.json", "UFO", "BAR");
	}

	@Test
	void shouldFindMottatteJournalposterInvalidTema() throws ParseException {
		assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_INVALID, 0,
				"mottatteJournalposterMedInvalidTema.json", "");
	}

	private void assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(String temaer, int expectedOutcome, String filename, String... resultTemaer) throws ParseException {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + temaer + "/5"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/" + filename)));

		ArrayList<Date> datoOpprettet = new ArrayList<>();

		datoOpprettet.add(dateFormatter.parse("2014-04-11T14:11:34.622+0000"));
		datoOpprettet.add(dateFormatter.parse("2015-12-28T15:11:05.455+0000"));

		FinnMottatteJournalposterResponse response = finnMottatteJournalposterConsumer.finnMottatteJournalposter(temaer, 5);

		assertEquals(expectedOutcome, response.getJournalposter().size());

		for (int i = 0; i < response.getJournalposter().size(); i++) {
			Journalpost journalpost = response.getJournalposter().get(i);
			assertEquals(JOURNALPOSTIDS.get(i), (Long) journalpost.getJournalpostId());
			assertEquals("M", journalpost.getJournalStatus());
			assertEquals("ALTINN", journalpost.getMottaksKanal());
			assertEquals(PERSONIDS.get(i), journalpost.getBruker().getId());
			assertEquals("PERSON", journalpost.getBruker().getType());
			assertTrue(assertValidTema(journalpost, resultTemaer));
			assertEquals("ab0001", journalpost.getBehandlingstema());
			assertEquals("0000", journalpost.getJournalforendeEnhet());
			assertEquals(datoOpprettet.get(i), journalpost.getDatoOpprettet());
		}
	}

	@Test
	void shouldThrowIllegalArgumentExceptionWhenFindMottatteJournalposterMedTemaNull() {
		assertThrows(IllegalArgumentException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(null, 5));
	}

	@Test
	void shouldFindMottatteJournalposterHappyPathTemaNone() {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + "/5"))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())));
		assertThrows(FinnMottatteJournalposterFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(TEMA_NONE, 5));
	}

	@Test
	void shouldThrowFinnMottatteJournalposterFinnesIkkeFunctionalException() {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH + "/5"))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())));
		assertThrows(FinnMottatteJournalposterFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH, 5));
	}

	@Test
	void shouldThrowFinnOppgaveTillaterIkkeTilknytningFunctionalException() {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH + "/5"))
				.willReturn(aResponse().withStatus(CONFLICT.value())));
		assertThrows(FinnMottatteJournalposterFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH, 5));
	}

	@Test
	void shouldThrowFinnMottatteJournalposterFunctionalException() {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH + "/5"))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())));
		assertThrows(FinnMottatteJournalposterFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH, 5));
	}

	@Test
	void shouldThrowFinnMottatteJournalposterTechnicalException() {
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH + "/5"))
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
		assertThrows(FinnMottatteJournalposterTechnicalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH, 5));
	}

	private boolean assertValidTema(Journalpost journalpost, String... resultTemaer) {
		for (String validTemas : resultTemaer) {
			if (validTemas.equals(journalpost.getTema())) {
				return true;
			}
		}
		return false;
	}
}
