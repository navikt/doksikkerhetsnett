package no.nav.doksikkerhetsnett.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.exceptions.functional.FinOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
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

import java.util.ArrayList;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class FinnOppgaveConsumerIT {
    private static final String URL_FINNOPPGAVER = "/api/v1/oppgaver\\?";
    private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
    private static final String OPPGAVER_HAPPY_PATH = "journalpostId=111111111&statuskategori=AAPEN&sorteringsrekkefolge=ASC&&limit=50";
    private static int HAPPY_INT = 111111111;
    private static int BAD_INT = 222;
    private static final String OPPGAVER_BAD_REQUEST = "journalpostId=222&statuskategori=AAPEN&sorteringsrekkefolge=ASC&&limit=50";
    private FinnOppgaveConsumer finnOppgaveConsumer;

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @Autowired
    private StsRestConsumer stsRestConsumer;


    @BeforeEach
    void setUpConsumer() {
        setupSts();
        finnOppgaveConsumer = new FinnOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void testFinnOppgaveMapping() {
        stubFor(get(urlMatching(URL_FINNOPPGAVER + OPPGAVER_HAPPY_PATH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));

        FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(HAPPY_INT));
        assertEquals("111111111", finnOppgaveResponse.getOppgaver().get(0).getJournalpostId());
        assertEquals(2, finnOppgaveResponse.getOppgaver().size());
    }

    @Test
    public void shouldReturnEmptyResponseWithEmptyInput() {
        FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(new ArrayList<>());
        assertEquals(0, finnOppgaveResponse.getOppgaver().size());
    }

    @Test
    public void shouldReturnEmptyResponseWithInputNull() {
        FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(null);
        assertEquals(0, finnOppgaveResponse.getOppgaver().size());
    }

    @Test
    public void shouldThrowFinnOppgaveFinnesIkkeFunctionalException() {
        stubFor(get(urlMatching(URL_FINNOPPGAVER + OPPGAVER_BAD_REQUEST))
                .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
        assertThrows(FinnOppgaveFinnesIkkeFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
    }

    @Test
    public void shouldThrowFinnOppgaveTillaterIkkeTilknytningFunctionalException() {
        stubFor(get(urlMatching(URL_FINNOPPGAVER + OPPGAVER_BAD_REQUEST))
                .willReturn(aResponse().withStatus(HttpStatus.CONFLICT.value())));
        assertThrows(FinOppgaveTillaterIkkeTilknyttingFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
    }

    @Test
    public void shouldThrowFinnOppgaveFunctionalException() {
        stubFor(get(urlMatching(URL_FINNOPPGAVER + OPPGAVER_BAD_REQUEST))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
        assertThrows(FinnOppgaveFunctionalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
    }

    @Test
    public void shouldThrowFinnOppgaveTechnicalException() {
        stubFor(get(urlMatching(URL_FINNOPPGAVER + OPPGAVER_BAD_REQUEST))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        assertThrows(FinnOppgaveTechnicalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(getJournalpostList(BAD_INT)));
    }

    private void setupSts() {
        stubFor(get(urlMatching(URL_STSAUTH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/stsResponse-happy.json")));
    }

    private ArrayList<UbehandletJournalpost> getJournalpostList(int... journalpostIds) {
        ArrayList<UbehandletJournalpost> ubehandledeJournalpostList = new ArrayList<>();
        for (int journalpostId : journalpostIds) {
            ubehandledeJournalpostList.add(new UbehandletJournalpost().builder().journalpostId(journalpostId).build());
        }
        return ubehandledeJournalpostList;
    }
}
