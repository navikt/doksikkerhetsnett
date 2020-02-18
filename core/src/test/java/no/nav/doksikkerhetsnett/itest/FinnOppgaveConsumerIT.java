package no.nav.doksikkerhetsnett.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.sts.StsRestConsumer;
import no.nav.doksikkerhetsnett.itest.config.FinnMottatteJournalposterTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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
@SpringBootTest(classes = {FinnMottatteJournalposterTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@Disabled
public class FinnOppgaveConsumerIT {
    private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/api/v1/oppgaver?";

    @Autowired
    private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void testFinnOppgaveMapping() {
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));
        stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));

        StsRestConsumer stsRestConsumer = new StsRestConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties);
        FinnOppgaveConsumer finnOppgaveConsumer = new FinnOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer);
        ArrayList<UbehandletJournalpost> ubehandledeJournalpostList = new ArrayList<>();
        ubehandledeJournalpostList.add(new UbehandletJournalpost().builder().journalpostId(111111111).build());

        FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(ubehandledeJournalpostList);
    }
}
