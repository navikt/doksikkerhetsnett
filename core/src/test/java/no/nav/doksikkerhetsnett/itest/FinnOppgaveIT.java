package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
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
class FinnOppgaveIT {
	private static final String URL_OPPGAVER = "/api/v1/oppgaver";
	private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
	private static final String OPPGAVER_HAPPY_PATH = "\\?journalpostId=111111111&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";
	private static final int HAPPY_INT = 111111111;
	private static final int BAD_INT = 222;
	private static final String OPPGAVER_BAD_REQUEST = "\\?journalpostId=222&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";

	@Autowired
	private FinnOppgaveConsumer finnOppgaveConsumer;

	@BeforeEach
	void setUpConsumer() {
		setupSts();
	}

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	void shouldFinnOppgaveMapping() {
		stubFor(get(urlMatching(URL_OPPGAVER + OPPGAVER_HAPPY_PATH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));

		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(HAPPY_INT));
		assertEquals("111111111", finnOppgaveResponse.getOppgaver().get(0).getJournalpostId());
		assertEquals(2, finnOppgaveResponse.getOppgaver().size());
	}

	@Test
	void shouldReturnEmptyResponseWithEmptyInput() {
		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(new ArrayList<>());
		assertEquals(0, finnOppgaveResponse.getOppgaver().size());
	}

	@Test
	void shouldReturnEmptyResponseWithInputNull() {
		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(null);
		assertEquals(0, finnOppgaveResponse.getOppgaver().size());
	}

	@Test
	void shouldThrowFinnOppgaveFinnesIkkeFunctionalException() {
		stubFor(get(urlMatching(URL_OPPGAVER + OPPGAVER_BAD_REQUEST))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())));
		assertThrows(FinnOppgaveFinnesIkkeFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
	}

	@Test
	void shouldThrowFinnOppgaveTillaterIkkeTilknytningFunctionalException() {
		stubFor(get(urlMatching(URL_OPPGAVER + OPPGAVER_BAD_REQUEST))
				.willReturn(aResponse().withStatus(CONFLICT.value())));
		assertThrows(FinnOppgaveTillaterIkkeTilknyttingFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
	}

	@Test
	void shouldThrowFinnOppgaveFunctionalException() {
		stubFor(get(urlMatching(URL_OPPGAVER + OPPGAVER_BAD_REQUEST))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())));
		assertThrows(FinnOppgaveFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
	}

	@Test
	void shouldThrowFinnOppgaveTechnicalException() {
		stubFor(get(urlMatching(URL_OPPGAVER + OPPGAVER_BAD_REQUEST))
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
		assertThrows(FinnOppgaveTechnicalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
	}

	private void setupSts() {
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
	}

	private ArrayList<Journalpost> getJournalpostList(int... journalpostIds) {
		ArrayList<Journalpost> ubehandledeJournalpostList = new ArrayList<>();
		for (int journalpostId : journalpostIds) {
			ubehandledeJournalpostList.add(new Journalpost().builder().journalpostId(journalpostId).build());
		}
		return ubehandledeJournalpostList;
	}
}
