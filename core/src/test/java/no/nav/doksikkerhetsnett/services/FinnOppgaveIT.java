package no.nav.doksikkerhetsnett.services;

import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import no.nav.doksikkerhetsnett.services.config.DoksikkerhetsnettItest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class FinnOppgaveIT extends DoksikkerhetsnettItest {
	private static final Long JOURNALPOSTID = 111111111L;
	private static final Long JOURNALPOSTID_2 = 222222222L;

	private static final String OPPGAVER_QUERY = "\\?%s&oppgavetype=JFR&oppgavetype=FDR&statuskategori=AAPEN&limit=50";

	@Autowired
	private FinnOppgaveConsumer finnOppgaveConsumer;

	@Test
	void shouldFinnOppgave() {
		stubFor(get(urlMatching(buildUrl(JOURNALPOSTID)))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-happy.json")));

		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(List.of(JOURNALPOSTID), 0);

		assertThat(finnOppgaveResponse.getOppgaver())
				.isNotNull()
				.extracting("journalpostId")
				.contains(String.valueOf(JOURNALPOSTID));
	}

	@Test
	void shouldFinnOppgaveForMultipleJournalposts() {
		stubFor(get(urlMatching(buildUrl(JOURNALPOSTID, JOURNALPOSTID_2)))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("finnoppgave/finnOppgaver-happy.json")));

		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(List.of(JOURNALPOSTID, JOURNALPOSTID_2), 0);

		assertThat(finnOppgaveResponse.getOppgaver())
				.hasSize(2)
				.extracting("journalpostId")
				.containsExactly(String.valueOf(JOURNALPOSTID), String.valueOf(JOURNALPOSTID_2));
	}

	@ParameterizedTest
	@MethodSource
	@NullSource
	void shouldReturnEmptyResponseWithEmptyOrNullInput(List<Long> journalpostIds) {
		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(journalpostIds, 0);
		assertThat(finnOppgaveResponse).isNull();
	}

	static Stream<Arguments> shouldReturnEmptyResponseWithEmptyOrNullInput() {
		return Stream.of(
				Arguments.of(List.of())
		);
	}

	@Test
	void shouldReturnEmptyResponseWithInputNull() {
		FinnOppgaveResponse finnOppgaveResponse = finnOppgaveConsumer.finnOppgaveForJournalposter(null, 0);
		assertThat(finnOppgaveResponse).isNull();
	}

	@Test
	void shouldThrowFinnOppgaveFunctionalExceptionWhenBadRequest() {
		stubFor(get(urlMatching(buildUrl(JOURNALPOSTID)))
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withBodyFile("finnoppgave/finnOppgaver-badRequest.json")));

		assertThatExceptionOfType(FinnOppgaveFunctionalException.class)
				.isThrownBy(() -> finnOppgaveConsumer.finnOppgaveForJournalposter(List.of(JOURNALPOSTID), 0))
				.withMessageContainingAll(
						"finnOppgaveForJournalposter feilet funksjonelt med statusKode=400 BAD_REQUEST",
						"\"feilmelding\": \"statuskategori må være en av [AAPEN, AVSLUTTET]\"");
	}

	@Test
	void shouldThrowFinnOppgaveTechnicalExceptionWhenInternalServerError() {
		stubFor(get(urlMatching(buildUrl(JOURNALPOSTID)))
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

		assertThrows(FinnOppgaveTechnicalException.class, () -> finnOppgaveConsumer.finnOppgaveForJournalposter(List.of(JOURNALPOSTID), 0));
	}

	@Test
	void shouldThrowFinnOppgaveFunctionalExceptionWhenUnauthorized() {
		stubFor(get(urlMatching(buildUrl(JOURNALPOSTID)))
				.willReturn(aResponse().withStatus(401)
						.withBodyFile("finnoppgave/finnOppgaver-unauthorized.json")));

		assertThatExceptionOfType(FinnOppgaveFunctionalException.class)
				.isThrownBy(() -> finnOppgaveConsumer.finnOppgaveForJournalposter(List.of(JOURNALPOSTID), 0))
				.withMessageContainingAll(
						"finnOppgaveForJournalposter feilet funksjonelt med statusKode=401 UNAUTHORIZED",
						"\"feilmelding\": \"Autentisering feilet\"");
	}

	private String buildUrl(Long... journalpostIds) {
		var journalpostParams = Arrays.stream(journalpostIds)
				.map(id -> "journalpostId=" + id)
				.collect(Collectors.joining("&"));

		return URL_OPPGAVE + OPPGAVER_QUERY.formatted(journalpostParams);
	}
}
