package no.nav.doksikkerhetsnett.services.config;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.metrics.MetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class},
		webEnvironment = RANDOM_PORT
)
@EnableWireMock
@ActiveProfiles("itest")
public abstract class DoksikkerhetsnettItest {

	protected static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/journalpostapi/v1/finnMottatteJournalposter";
	protected static final String URL_OPPGAVE_JOURNALPOST_SEARCH = "/api/v1/oppgaver\\?journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&limit=50";
	protected static final String URL_OPPGAVE = "/api/v1/oppgaver";
	protected static final String URL_JIRA = "/rest/api/2/issue";
	protected static final String JIRA_PROJECT_URL = "/rest/api/2/project/MMA";
	protected static final String URL_PDL = "/pdl/graphql";
	protected static final String METRIC_TAGS = "UFO;ALTINN;0000";

	@BeforeEach
	void setup(){
		setUpStubs();
		MDC.put(MDC_CALL_ID, "itest-callId");
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
		stubFor(get(urlMatching(URL_OPPGAVE_JOURNALPOST_SEARCH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-happy.json")));
		stubFor(get(urlPathEqualTo(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnmottattejournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-aktoerid-happy.json")));
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
		stubFor(get(urlMatching(JIRA_PROJECT_URL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jira-project.json")));
	}

}
