package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.contract.wiremock.restdocs.WireMockWebTestClient.verify;
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
        opprettOppgaveConsumer = new OpprettOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer, jiraConsumer);
    }

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void testOpprettOppgaveMapping() {
        Journalpost jp = Journalpost.builder().journalpostId(555).build();
        stubFor(post(urlMatching(URL_OPPGAVE))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happy.json")));

        OpprettOppgaveResponse opprettOppgaveResponse = opprettOppgaveConsumer.opprettOppgave(jp);
        assertEquals("555", opprettOppgaveResponse.getJournalpostId());
    }

    @Test
    public void testOpprettOppgaveMedUgyldigEnhet() {
        Journalpost jp = Journalpost.builder()
                .journalpostId(555)
                .journalforendeEnhet("FEIL")
                .build();
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tildeltEnhetsnr\": \"FEIL\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-feilEnhet.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tildeltEnhetsnr\": null}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgave-happy.json")));

        OpprettOppgaveResponse response = opprettOppgaveConsumer.opprettOppgave(jp);
        assertEquals("555", response.getJournalpostId());
        assertEquals(null, response.getTildeltEnhetsnr());
    }

    @Test
    public void testUgyldigAnsvarligEnhet() {
        Journalpost jp = Journalpost.builder()
                .journalpostId(555)
                .tema("AAR")
                .build();
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"JFR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-ugyldigAnsvarligEnhet.json")));
        stubFor(post(urlMatching(URL_OPPGAVE))
                .withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"FDR\"}", true, true))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet.json")));

        OpprettOppgaveResponse response = opprettOppgaveConsumer.opprettOppgave(jp);
        assertEquals("555", response.getJournalpostId());
        assertEquals("AAR", response.getTema());
        assertEquals("FDR", response.getOppgavetype());
    }

    @Test
    public void testShouldmakeJiraIssueIfAllFails() {
        Journalpost jp = Journalpost.builder()
                .journalpostId(555)
                .tema("AAR")
                .build();
        stubFor(post(urlMatching(URL_OPPGAVE))
                .willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
                        .withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
        stubFor(post(urlMatching(URL_JIRA))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("opprettOppgave/jiraResponse-ok.json")));

        assertThrows(FinnOppgaveFunctionalException.class,
                () -> opprettOppgaveConsumer.opprettOppgave(jp));
        WireMock.verify(exactly(1), postRequestedFor(urlMatching(URL_JIRA)));
    }

    private void setupSts() {
        stubFor(get(urlMatching(URL_STSAUTH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("oppgave/stsResponse-happy.json")));
    }

}
