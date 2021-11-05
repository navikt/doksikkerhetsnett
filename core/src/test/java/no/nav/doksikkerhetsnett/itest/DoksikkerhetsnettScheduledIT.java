package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.doksikkerhetsnett.DoksikkerhetsnettScheduled;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.FinnMottatteJournalposterConsumer;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.consumers.pdl.IdentConsumer;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.services.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.services.FinnOppgaveService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;
import static no.nav.doksikkerhetsnett.entities.Bruker.TYPE_PERSON;
import static no.nav.doksikkerhetsnett.utils.Utils.EESSI;
import static no.nav.doksikkerhetsnett.utils.Utils.ENHET_4530;
import static no.nav.doksikkerhetsnett.utils.Utils.TEMA_MED;
import static no.nav.doksikkerhetsnett.utils.Utils.TEMA_UFM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.hamcrest.core.Is.is;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class DoksikkerhetsnettScheduledIT {
	private static final String URL_FINNMOTTATTEJOURNALPOSTER = "/rest/intern/journalpostapi/v1/finnMottatteJournalposter/.{3}/.{1}";
	private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";
	private static final String URL_OPPGAVE_JOURNALPOST_SEARCH = "/api/v1/oppgaver\\?journalpostId=111111111&journalpostId=222222222&journalpostId=333333333&journalpostId=444444444&journalpostId=555555555&journalpostId=666666666&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&sorteringsrekkefolge=ASC&limit=50";
	private static final String URL_OPPGAVE = "/api/v1/oppgaver";
	private static final String URL_JIRA = "/rest/api/2/issue";
	private static final String URL_PDL = "/pdl";
	private static final String METRIC_TAGS = "UFO;ALTINN;0000";
	private static final String IKKE_EESSI ="IKKE_ESSI";

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

	@Autowired
	private IdentConsumer identConsumer;

	@BeforeEach
	void setUpConsumer() {
		setUpStubs();
		finnOppgaveService = new FinnOppgaveService(new FinnOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer));
		finnMottatteJournalposterService = new FinnMottatteJournalposterService(new FinnMottatteJournalposterConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties));
		opprettOppgaveService = new OpprettOppgaveService(new OpprettOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer), jiraConsumer, identConsumer);
	}

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
	}

	@Test
	void shouldFindMottatteJournalposter() {
		DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
				finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService, opprettOppgaveService, metricsScheduler);
		List<Journalpost> journalposterUtenOppgaver = new ArrayList();
		Arrays.stream(dokSikkerhetsnettProperties.getSkrivTemaer().split(","))
				.forEach(tema -> journalposterUtenOppgaver.addAll(doksikkerhetsnettScheduled.finnJournalposterUtenOppgave(tema)));
		assertEquals(4, journalposterUtenOppgaver.size());

		Map<String, Integer> totalMetricsCache = metricsScheduler.getCaches().get(0);
		Map<String, Integer> utenOppgaveMetricsCache = metricsScheduler.getCaches().get(1);
		assertCorrectMetrics(totalMetricsCache, 6, METRIC_TAGS);
		assertCorrectMetrics(utenOppgaveMetricsCache, 4, METRIC_TAGS);
	}

	@Test
	void shouldFindMottatteJournalposterForAlleTema() {
		DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
				finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService, opprettOppgaveService, metricsScheduler);
		List<Journalpost> journalposterUtenOppgaver = new ArrayList();
		Utils.getAlleTema()
				.forEach(tema -> journalposterUtenOppgaver.addAll(doksikkerhetsnettScheduled.finnJournalposterUtenOppgave(tema)));
		assertEquals(220, journalposterUtenOppgaver.size());
	}

	@Test
	void shouldCreateJiraIssueWhenOpprettOppgaveFails() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
		stubFor(post(urlMatching(URL_JIRA))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));

		DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
				finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService, opprettOppgaveService, metricsScheduler);

		doksikkerhetsnettScheduled.lagOppgaverForGlemteJournalposter(dokSikkerhetsnettProperties.getSkrivTemaer());

		WireMock.verify(exactly(4), postRequestedFor(urlMatching(URL_JIRA)));
	}

	@Test
	void shouldFilterJournalposter(){
		DoksikkerhetsnettScheduled doksikkerhetsnettScheduled = new DoksikkerhetsnettScheduled(
				finnMottatteJournalposterService, dokSikkerhetsnettProperties, finnOppgaveService, opprettOppgaveService, metricsScheduler);

		Journalpost post1 = createJournalpostWithOutBruker(TEMA_UFM, ENHET_4530, EESSI);
		Journalpost post2 = createJournalpostWithOutBruker(TEMA_MED, ENHET_4530, EESSI);
		Journalpost post3 = createJournalpostWithOutBruker(TEMA_MED, ENHET_4530, IKKE_EESSI);
		Journalpost post11 = createJournalpostWithBruker(TEMA_UFM, ENHET_4530, EESSI);
		Journalpost post22 = createJournalpostWithBruker(TEMA_MED, ENHET_4530, EESSI);

		List<Journalpost> journalpostList = doksikkerhetsnettScheduled.filtererUonskedeJournalposter(asList(post1, post2, post11, post22, post3));
		assertEquals(3, journalpostList.size());
		assertTrue(journalpostList.stream().anyMatch(jp -> TEMA_UFM.equals(jp.getTema()) && jp.getBruker() != null && jp.getJournalforendeEnhet() == ENHET_4530));
		assertTrue(journalpostList.stream().anyMatch(jp -> TEMA_MED.equals(jp.getTema()) && jp.getBruker() != null && jp.getJournalforendeEnhet() == ENHET_4530));
		assertTrue(journalpostList.stream().anyMatch(jp -> TEMA_MED.equals(jp.getTema()) && jp.getMottaksKanal().equals(IKKE_EESSI)));
	}

	private Journalpost createJournalpostWithBruker(String tema, String enhet, String mottaksKanal){
		return Journalpost.builder()
				.journalpostId(333)
				.bruker(Bruker.builder().id("1").type(TYPE_PERSON).build())
				.tema(tema)
				.mottaksKanal(mottaksKanal)
				.journalforendeEnhet(enhet)
				.build();
	}

	private Journalpost createJournalpostWithOutBruker(String tema, String enhet, String mottaksKanal){
		return Journalpost.builder()
				.journalpostId(333)
				.bruker(null)
				.tema(tema)
				.journalforendeEnhet(enhet)
				.mottaksKanal(mottaksKanal)
				.build();
	}

	private void assertCorrectMetrics(Map<String, Integer> metricsCache, int expectedValue, String expectedString) {
		assertEquals(expectedValue, (int) metricsCache.entrySet().iterator().next().getValue());
		assertEquals(expectedString, metricsCache.entrySet().iterator().next().getKey());
	}
}