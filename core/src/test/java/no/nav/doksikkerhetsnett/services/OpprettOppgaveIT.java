package no.nav.doksikkerhetsnett.services;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import no.nav.doksikkerhetsnett.CoreConfig;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.services.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

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
import static java.util.Collections.singletonList;
import static no.nav.doksikkerhetsnett.entities.Bruker.TYPE_PERSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {TestConfig.class, CoreConfig.class},
		webEnvironment = RANDOM_PORT
)
@EnableWireMock
@ActiveProfiles("itest")
class OpprettOppgaveIT {

	private static final String JIRA_PROJECT_URL = "/rest/api/2/project/MMA";
	private static final String URL_OPPGAVE = "/api/v1/oppgaver";
	private static final String URL_JIRA = "/rest/api/2/issue";
	private static final String URL_PDL = "/pdl/graphql";

	@Autowired
	private OpprettOppgaveService opprettOppgaveService;

	@BeforeEach
	void setup() {
		setUpTokenServices();
		happyAktoerIdStub();
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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happy444.json")));
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"555\"}", true, true))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happyFDR.json")));
		List<Journalpost> jp = singletonList(Journalpost.builder()
				.journalpostId(555)
				.journalforendeEnhet("FEIL")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).getFirst();
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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet.json")));
		List<Journalpost> jp = singletonList(Journalpost.builder()
				.journalpostId(555)
				.tema("AAR")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).getFirst();
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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgaveResponse-NyAnsvarligEnhet-PEN.json"))
				.willSetStateTo("done"));
		List<Journalpost> jp = singletonList(Journalpost.builder()
				.journalpostId(555)
				.tema("PEN")
				.build());

		OpprettOppgaveResponse response = opprettOppgaveService.opprettOppgaver(jp).getFirst();
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
				.willReturn(aResponse().withStatus(CREATED.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jiraResponse-ok.json")));
		stubFor(get(urlMatching(JIRA_PROJECT_URL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/jira-project.json")));

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
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happyWithAktorId222.json")));

		Journalpost jp =
				Journalpost.builder()
						.journalpostId(222)
						.bruker(Bruker.builder().id("1").type(TYPE_PERSON).build())
						.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(singletonList(jp));
		assertEquals("222", opprettOppgaverResponses.getFirst().getJournalpostId());
		assertEquals("1234567890123", opprettOppgaverResponses.getFirst().getAktoerId());
	}

	@Test
	void shouldNotSettAktorIdWhenBrukerHaveWrongType() {
		stubOppgaveWithHappy333Response();

		Journalpost jp =
				Journalpost.builder()
						.journalpostId(333)
						.bruker(Bruker.builder().id("1").type("IKKE_PERSON").build())
						.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(singletonList(jp));
		assertNull(opprettOppgaverResponses.getFirst().getAktoerId());
		assertEquals("333", opprettOppgaverResponses.getFirst().getJournalpostId());
	}

	@Test
	void shouldNotSettAktorIdWhenAPICallToIdentConsumerFails() {
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-ident-notfound.json")));
		stubOppgaveWithHappy333Response();

		Journalpost jp = Journalpost.builder()
				.journalpostId(333)
				.bruker(Bruker.builder().id("1").type(TYPE_PERSON).build())
				.build();

		List<OpprettOppgaveResponse> opprettOppgaverResponses = opprettOppgaveService.opprettOppgaver(singletonList(jp));
		assertNull(opprettOppgaverResponses.getFirst().getAktoerId());
		assertEquals("333", opprettOppgaverResponses.getFirst().getJournalpostId());
	}

	private void stubOppgaveWithHappy333Response() {
		stubFor(post(urlMatching(URL_OPPGAVE))
				.withRequestBody(equalToJson("{\"journalpostId\": \"333\", \"aktoerId\": null}", true, true))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("opprettOppgave/opprettOppgave-happy333.json")));
	}

	private void setUpTokenServices() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	void happyAktoerIdStub() {
		stubFor(post(urlEqualTo(URL_PDL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-aktoerid-happy.json")));
	}
}
