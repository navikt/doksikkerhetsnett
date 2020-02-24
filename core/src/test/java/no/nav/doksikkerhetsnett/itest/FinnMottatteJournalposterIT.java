package no.nav.doksikkerhetsnett.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import no.nav.doksikkerhetsnett.itest.config.TestConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
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
    private static final String JUST_FOR_PATH = "does_not_matter";
    private FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer;

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @BeforeEach
    void setUp() {
        finnMottatteJournalposterConsumer = new FinnMottatteJournalposterConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void finnMottatteJournalposterHappyPathTemaSingle() {
        assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_SINGLE, 2,
                "mottatteJournalposterMedTemaSingle-happy.json", TEMA_SINGLE);
    }

    @Test
    public void finnMottatteJournalposterHappyPathTemaMulti() {
        assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_MULTI, 2,
                "mottatteJournalposterMedTemaMulti-happy.json", "UFO", "BAR");
    }

    @Test
    public void finnMottatteJournalposterHappyPathTemaNone() {
        assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_NONE, 2,
                "mottatteJournalposterMedTemaNone-happy.json", "UFO", "PEN");
    }

    @Test
    public void finnMottatteJournalposterInvalidTema() {
        assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(TEMA_INVALID, 0,
                "mottatteJournalposterMedInvalidTema.json", "");
    }

    @Test
    public void finnMottatteJournalposterMedTemaNull() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + ""))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnmottattejournalposter/mottatteJournalposterMedTemaMulti-happy.json")));
        assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(null, 2,
                "mottatteJournalposterMedTemaMulti-happy.json", "UFO", "BAR");
    }

    private void assertFinnMottatteJournalPosterConsumerGetsExpectedNumberofJournalpostsAndCorrectValues(String temaer, int expectedOutcome, String filename, String... resultTemaer) {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + temaer))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnmottattejournalposter/" + filename)));

        ArrayList<Date> datoOpprettet = new ArrayList<>();
        try {
            datoOpprettet.add(dateFormatter.parse("2014-04-11T14:11:34.622+0000"));
            datoOpprettet.add(dateFormatter.parse("2015-12-28T15:11:05.455+0000"));
        } catch (Exception e) {
        }

        FinnMottatteJournalposterResponse response = finnMottatteJournalposterConsumer.finnMottatteJournalposter(temaer);

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
    public void shouldThrowFinnMottatteJournalposterFinnesIkkeFunctionalException() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
        assertThrows(FinnMottatteJournalposterFinnesIkkeFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH));
    }

    @Test
    public void shouldThrowFinnOppgaveTillaterIkkeTilknytningFunctionalException() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value())));
        assertThrows(FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH));
    }

    @Test
    public void shouldThrowFinnMottatteJournalposterFunctionalException() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
        assertThrows(FinnMottatteJournalposterFunctionalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH));
    }

    @Test
    public void shouldThrowFinnMottatteJournalposterTechnicalException() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER + JUST_FOR_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        assertThrows(FinnMottatteJournalposterTechnicalException.class, () -> finnMottatteJournalposterConsumer.finnMottatteJournalposter(JUST_FOR_PATH));
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
