package no.nav.doksikkerhetsnett.scheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.sts.StsRestConsumer;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class DoksikkerhetsnettScheduledIT {
    private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/";
    private static final String QUERY_PARAM_AAPNEOPPGAVER = "&statuskategori=AAPEN&sorteringsrekkefolge=ASC&&limit=50";
    private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
    private static final String URL_OPPGAVE = "/api/v1/oppgaver\\?";
    private static final String JOURNALPOST_SEARCH = "journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666";
    private FinnMottatteJournalposterService finnMottatteJournalposterService;
    private FinnOppgaveService finnOppgaveService;

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @Autowired
    private StsRestConsumer stsRestConsumer;

    @BeforeEach
    void setUpConsumer() {
        setUpStubs();
        finnOppgaveService = new FinnOppgaveService(new FinnOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer));
        finnMottatteJournalposterService = new FinnMottatteJournalposterService(new FinnMottatteJournalposterConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties));
    }

    void setUpStubs() {
        stubFor(get(urlMatching(URL_STSAUTH))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/stsResponse-happy.json")));
        stubFor(get(urlMatching(URL_OPPGAVE + JOURNALPOST_SEARCH + QUERY_PARAM_AAPNEOPPGAVER))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnOppgave/finnOppgaverAAPNE-happy.json")));
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnmottattejournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
    }

    @Test
    public void Test() {
        DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
                finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService);
        List journalposterUtenOppgaver = doksikkerhetsnettScheduled.finnjournalposterUtenOppgaver();
        assertEquals(journalposterUtenOppgaver.size(), 4);
    }
}