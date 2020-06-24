package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
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

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class OpprettOppgaveIT {

    private static final String URL_OPPGAVE = "/api/v1/oppgaver";
    private static final String URL_JIRA = "/rest/api/2/issue";
    private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";

    private OpprettOppgaveService opprettOppgaveService;
    private OpprettOppgaveConsumer opprettOppgaveConsumer;

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @Autowired
    private StsRestConsumer stsRestConsumer;

    @Autowired
    private JiraConsumer jiraConsumer;

    @BeforeEach
    void setup() {
        setupSts();
        opprettOppgaveConsumer = new OpprettOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer);
        opprettOppgaveService = new OpprettOppgaveService(opprettOppgaveConsumer, jiraConsumer);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void testOpprettOppgaveMapping() {
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"journalpostId\": \"333\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happy333.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"journalpostId\": \"444\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happy444.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"journalpostId\": \"555\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happy555.json")));
        List<Journalpost> jps = asList(Journalpost.builder().journalpostId(333).build(),
                Journalpost.builder().journalpostId(444).build());
        Oppgave oppgave = Oppgave.builder().journalpostId("555").build();

        OpprettOppgaveResponse opprettOppgaveResponse = opprettOppgaveService.opprettOppgave(oppgave);
        List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(jps);
        assertEquals("333", opprettOppgaverResponses.get(0).getJournalpostId());
        assertEquals("444", opprettOppgaverResponses.get(1).getJournalpostId());
        assertEquals("555", opprettOppgaveResponse.getJournalpostId());
    }

    @Test
    public void testOpprettOppgaveMedUgyldigEnhet() {
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tildeltEnhetsnr\": \"FEIL\", \"oppgavetype\": \"JFR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-feilEnhet.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tildeltEnhetsnr\": null, \"oppgavetype\": \"FDR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happyFDR.json")));
        List<Journalpost> jp = asList(Journalpost.builder()
                .journalpostId(555)
                .journalforendeEnhet("FEIL")
                .build());

        OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).get(0);
        assertEquals("555", response.getJournalpostId());
        assertEquals("FDR", response.getOppgavetype());
        assertEquals(null, response.getTildeltEnhetsnr());
    }

    @Test
    public void testUgyldigAnsvarligEnhet() {
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"JFR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-ugyldigAnsvarligEnhet.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"FDR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet.json")));
        List<Journalpost> jp = asList(Journalpost.builder()
                .journalpostId(555)
                .tema("AAR")
                .build());

        OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).get(0);
        assertEquals("555", response.getJournalpostId());
        assertEquals("AAR", response.getTema());
        assertEquals("FDR", response.getOppgavetype());
    }

    @Test
    public void testShouldmakeJiraIssueIfAllFails() {
        Oppgave jp = Oppgave.builder()
                .journalpostId("555")
                .tema("AAR")
                .build();
        stubFor(post(urlMatching(URL_OPPGAVE))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
        stubFor(post(urlMatching(URL_JIRA))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/jiraResponse-ok.json")));

        assertThrows(OpprettOppgaveFunctionalException.class,
                () -> opprettOppgaveService.opprettOppgave(jp));
        WireMock.verify(exactly(1), postRequestedFor(urlMatching(URL_JIRA)));
    }

    private void setupSts() {
        stubFor(get(urlMatching(URL_STSAUTH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("oppgave/stsResponse-happy.json")));
    }

}
