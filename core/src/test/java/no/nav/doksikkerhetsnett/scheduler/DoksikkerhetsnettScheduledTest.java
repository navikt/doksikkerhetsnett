package no.nav.doksikkerhetsnett.scheduler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.sts.StsRestConsumer;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class DoksikkerhetsnettScheduledTest {
    private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/";
    private static final String QUERY_PARAM_AAPNEOPPGAVER = "&statuskategori=AAPEN&sorteringsrekkefolge=ASC";
    private static final String QUERY_PARAM_AVSLUTTETOPPGAVER = "&statuskategori=AAVSLUTTET&sorteringsrekkefolge=ASC";
    private static final String URL_OPPGAVE = "https://oppgave.nais.preprod.local/api/v1/oppgaver?";
    private static final String OPPGAVE_ID_STRING_AAPEN = "journalpostId=11111111";
    private static final String OPPGAVE_ID_STRING_AVSLUTTET = "journalpostId=222222222&journalpostId=333333333";
    private static final String STS_STORE = "https://security-token-service.nais.preprod.local/rest/v1/sts/token?grant_type=client_credentials&scope=openid";

	/*@Autowired
	private FinnOppgaveConsumer finnOppgaveConsumer;
	@Autowired
	private FinnMottatteJournalposterConsumer finnMottatteJournalposterConsumer;
	@Autowired
	private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	@Autowired
	private StsRestConsumer  stsRestConsumer;
	@Autowired
	private DoksikkerhetsnettScheduled doksikkerhetsnettScheduled;
	@Autowired
	private FinnMottatteJournalposterService finnMottatteJournalposterService;
	@Autowired
	private FinnOppgaveService finnOppgaveService;


	@Before
	public void setUp() {
		stubFor(get(urlMatching(STS_STORE))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnOppgave/stsResponse-happy.json")));
		stubFor(get(urlMatching(URL_OPPGAVE))
				.withQueryParam(QUERY_PARAM_AAPNEOPPGAVER, equalTo(OPPGAVE_ID_STRING_AAPEN + QUERY_PARAM_AAPNEOPPGAVER))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnOppgave/finnOppgaverAAPNE-happy.json")));
		stubFor(get(urlMatching(URL_OPPGAVE))
				.withQueryParam(QUERY_PARAM_AVSLUTTETOPPGAVER, equalTo(OPPGAVE_ID_STRING_AVSLUTTET + QUERY_PARAM_AAPNEOPPGAVER))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnOppgave/finnOppgaverAVSLUTTET-happy.json")));
		stubFor(get(urlMatching(URL_FINNMOTTATTEJOURNALPOSTER))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnMottatteJournalposter/mottatteJournalposterForSchedulerTest-happy.json")));
	}

	@Test
	public void Test() {
		doksikkerhetsnettScheduled.lagOppgaverForGlemteJournalposter();
	}   */

}