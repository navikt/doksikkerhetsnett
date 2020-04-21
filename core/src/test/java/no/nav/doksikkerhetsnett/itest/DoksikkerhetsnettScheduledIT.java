package no.nav.doksikkerhetsnett.itest;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.scheduler.DoksikkerhetsnettScheduled;
import no.nav.doksikkerhetsnett.services.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.services.FinnOppgaveService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
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
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class DoksikkerhetsnettScheduledIT {
    private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/PEN";
    private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
    private static final String URL_OPPGAVE_JOURNALPOST_SEARCH = "/api/v1/oppgaver\\?journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";
    private static final String METRIC_TAGS = "UFO;ALTINN;0000";

    private FinnMottatteJournalposterService finnMottatteJournalposterService;
    private FinnOppgaveService finnOppgaveService;
    private OpprettOppgaveService opprettOppgaveService;

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @Autowired
    private StsRestConsumer stsRestConsumer;

    @Autowired
    private MetricsScheduler metricsScheduler;

    @Autowired
    private JiraConsumer jiraConsumer;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        finnOppgaveService = new FinnOppgaveService(new FinnOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer));
        finnMottatteJournalposterService = new FinnMottatteJournalposterService(new FinnMottatteJournalposterConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties));
        opprettOppgaveService = new OpprettOppgaveService(new OpprettOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer), jiraConsumer);
    }

    void setUpStubs() {
        stubFor(get(urlMatching(URL_STSAUTH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("oppgave/stsResponse-happy.json")));
        stubFor(get(urlMatching(URL_OPPGAVE_JOURNALPOST_SEARCH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnmottattejournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
    }

    @Test
    public void Test() {
        DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
                finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService, opprettOppgaveService, metricsScheduler);
        List journalposterUtenOppgaver = doksikkerhetsnettScheduled.finnJournalposterUtenOppgaver(dokSikkerhetsnettProperties.getSkrivTemaer());
        assertEquals(journalposterUtenOppgaver.size(), 4);

        Map<String, Integer> totalMetricsCache = metricsScheduler.getCaches().get(0);
        Map<String, Integer> utenOppgaveMetricsCache = metricsScheduler.getCaches().get(1);
        assertCorrectMetrics(totalMetricsCache, 6, METRIC_TAGS);
        assertCorrectMetrics(utenOppgaveMetricsCache, 4, METRIC_TAGS);
    }

    private void assertCorrectMetrics(Map<String, Integer> metricsCache, int expectedValue, String expectedString) {
        assertEquals(expectedValue, (int) metricsCache.entrySet().iterator().next().getValue());
        assertEquals(expectedString, metricsCache.entrySet().iterator().next().getKey());
    }
}