package no.nav.doksikkerhetsnett.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import no.nav.doksikkerhetsnett.CoreConfig;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import no.nav.doksikkerhetsnett.consumers.pdl.IdentConsumer;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.itest.config.TestConfig;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;
import static no.nav.doksikkerhetsnett.entities.Bruker.TYPE_PERSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class, CoreConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class OpprettOppgaveIT {

	private static final String URL_OPPGAVE = "/api/v1/oppgaver";
	private static final String URL_JIRA = "/rest/api/2/issue";
	private static final String URL_PDL = "/pdl";
	private static final String URL_STSAUTH = "/rest/v1/sts/token\\?grant_type=client_credentials&scope=openid";

	private OpprettOppgaveService opprettOppgaveService;

	@Autowired
	private DokSikkerhetsnettProperties dokSikkerhetsnettProperties;

	@Autowired
	private StsRestConsumer stsRestConsumer;

	@Autowired
	private JiraConsumer jiraConsumer;

	@Autowired
	private IdentConsumer identConsumer;

	@BeforeEach
	void setup() {
		setupSts();
		happyAktoerIdStub();
		OpprettOppgaveConsumer opprettOppgaveConsumer = new OpprettOppgaveConsumer(new RestTemplateBuilder(), dokSikkerhetsnettProperties, stsRestConsumer);
		opprettOppgaveService = new OpprettOppgaveService(opprettOppgaveConsumer, jiraConsumer, identConsumer);
	}

	@AfterEach
	void tearDown() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
		WireMock.resetAllScenarios();
	}

	@Test
	void shouldOpprettOppgaveMapping() {
		stubOppgaveWithHappy333Response();
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"444\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happy444.json")));
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"555\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
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
	void shouldOpprettOppgaveMedUgyldigEnhet() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tildeltEnhetsnr\": \"FEIL\", \"oppgavetype\": \"JFR\"}", true, true))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-feilEnhet.json")));
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tildeltEnhetsnr\": null, \"oppgavetype\": \"FDR\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happyFDR.json")));
		List<Journalpost> jp = Collections.singletonList(Journalpost.builder()
				.journalpostId(555)
				.journalforendeEnhet("FEIL")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).get(0);
		assertEquals("555", response.getJournalpostId());
		assertEquals("FDR", response.getOppgavetype());
		assertNull(response.getTildeltEnhetsnr());
	}

	@Test
	void shoulOpprettOppgaveWithUgyldigAnsvarligEnhet() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"JFR\"}", true, true))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ugyldigAnsvarligEnhet.json")));
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tema\": \"AAR\", \"oppgavetype\": \"FDR\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet.json")));
		List<Journalpost> jp = Collections.singletonList(Journalpost.builder()
				.journalpostId(555)
				.tema("AAR")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).get(0);
		assertEquals("555", response.getJournalpostId());
		assertEquals("AAR", response.getTema());
		assertEquals("FDR", response.getOppgavetype());
	}

	@Test
	void shoulOpprettOppgaveWithUgyldigAnsvarligEnhetAndTemaPEN() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tema\": \"PEN\", \"oppgavetype\": \"JFR\"}", true, true))
				.inScenario("oppgave")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ugyldigAnsvarligEnhet.json"))
				.willSetStateTo("second"));
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"tema\": \"PEN\", \"oppgavetype\": \"JFR\"}", true, true))
				.inScenario("oppgave")
				.whenScenarioStateIs("second")
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet-PEN.json"))
				.willSetStateTo("done"));
		List<Journalpost> jp = Collections.singletonList(Journalpost.builder()
				.journalpostId(555)
				.tema("PEN")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).get(0);
		assertEquals("555", response.getJournalpostId());
		assertEquals("PEN", response.getTema());
		assertEquals("JFR", response.getOppgavetype());
	}

	@Test
	void shouldmakeJiraIssueIfOppgaveFails() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("opprettOppgave/opprettOppgave-ukjentFeil.json")));
		stubFor(post(urlMatching(URL_JIRA))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));

		Oppgave jp = Oppgave.builder()
				.journalpostId("555")
				.tema("AAR")
				.build();

		assertNull(opprettOppgaveService.opprettOppgave(jp));
		WireMock.verify(exactly(1), postRequestedFor(urlMatching(URL_JIRA)));
	}

	@Test
		//feiler
	void shouldGetAktorIdWhenBrukerWithIdAndTypeOfPerson() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"222\", \"aktoerId\": \"1234567890123\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happyWithAktorId222.json")));

		Journalpost jp =
				Journalpost.builder()
						.journalpostId(222)
						.bruker(Bruker.builder().id("1").type(TYPE_PERSON).build())
						.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(Collections.singletonList(jp));
		assertEquals("222", opprettOppgaverResponses.get(0).getJournalpostId());
		assertEquals("1234567890123", opprettOppgaverResponses.get(0).getAktoerId());
	}

	@Test
	void shouldNotSettAktorIdWhenBrukerHaveWrongType() {
		stubOppgaveWithHappy333Response();

		Journalpost jp =
				Journalpost.builder()
						.journalpostId(333)
						.bruker(Bruker.builder().id("1").type("IKKE_PERSON").build())
						.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(asList(jp));
		assertNull(opprettOppgaverResponses.get(0).getAktoerId());
		assertEquals("333", opprettOppgaverResponses.get(0).getJournalpostId());
	}

	@Test
	void shouldNotSettAktorIdWhenAPICallToIdentConsumerFails() {
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-ident-notfound.json")));
		stubOppgaveWithHappy333Response();

		Journalpost jp = Journalpost.builder()
				.journalpostId(333)
				.bruker(Bruker.builder().id("1").type(TYPE_PERSON).build())
				.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(asList(jp));
		assertNull(opprettOppgaverResponses.get(0).getAktoerId());
		assertEquals("333", opprettOppgaverResponses.get(0).getJournalpostId());
	}

	private void stubOppgaveWithHappy333Response() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"333\", \"aktoerId\": null}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happy333.json")));
	}

	private void setupSts() {
		stubFor(get(urlMatching(URL_STSAUTH))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("oppgave/stsResponse-happy.json")));
	}

	void happyAktoerIdStub() {
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-aktoerid-happy.json")));
	}
}
