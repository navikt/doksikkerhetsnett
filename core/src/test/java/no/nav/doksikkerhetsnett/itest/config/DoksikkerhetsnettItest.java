package no.nav.doksikkerhetsnett.itest.config;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.metrics.MetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public abstract class DoksikkerhetsnettItest {

	protected static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/internal/journalpostapi/v1/finnMottatteJournalposter/.{3}/.{1}";
	protected static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
	protected static final String URL_OPPGAVE_JOURNALPOST_SEARCH = "/api/v1/oppgaver\\?journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";
	protected static final String URL_OPPGAVE = "/api/v1/oppgaver";
	protected static final String URL_JIRA = "/rest/api/2/issue";
	protected static final String URL_PDL = "/pdl";
	protected static final String METRIC_TAGS = "UFO;ALTINN;0000";

	@BeforeEach
	void setup(){
		setUpStubs();
	}

	@Autowired
	protected DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

	@Autowired
	protected MetricsService metricsScheduler;

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	void setUpStubs() {
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
		stubFor(get(urlMatching(URL_OPPGAVE_JOURNALPOST_SEARCH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaverAAPNE-happy.json")));
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-aktoerid-happy.json")));
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

}
