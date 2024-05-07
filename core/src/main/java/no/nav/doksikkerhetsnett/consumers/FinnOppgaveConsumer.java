package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClientRequest;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.NavHeaders.X_CORRELATION_ID;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doksikkerhetsnett.consumers.azure.AzureProperties.CLIENT_REGISTRATION_OPPGAVE;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_FORDELING;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_JOURNALFOERT;
import static no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService.JOURNALPOSTER_PARTITION_LIMIT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class FinnOppgaveConsumer {

	private final WebClient webClient;

	private static final String OPPGAVER_URI_PATH = "/api/v1/oppgaver";

	private static final String PARAM_NAME_JOURNALPOSTID = "journalpostId";
	private static final String PARAM_NAME_OPPGAVETYPE = "oppgavetype";
	private static final String PARAM_NAME_STATUSKATEGORI = "statuskategori";
	private static final String PARAM_VALUE_STATUSKATEGORI = "AAPEN";
	private static final String PARAM_NAME_LIMIT = "limit";
	private static final String PARAM_NAME_OFFSET = "offset";

	public FinnOppgaveConsumer(DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
							   WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokSikkerhetsnettProperties.getEndpoints().getOppgave().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(APPLICATION_JSON))
				.build();
	}

	@Retryable(retryFor = FinnOppgaveTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public FinnOppgaveResponse finnOppgaveForJournalposter(List<Long> ubehandledeJournalposter, int offset) {
		if (ubehandledeJournalposter == null || ubehandledeJournalposter.isEmpty()) {
			return null;
		}

		String journalpostParams = mapJournalpostIdListToQueryParams(ubehandledeJournalposter);

		return webClient.get()
				.uri(uriBuilder -> {
					uriBuilder.path(OPPGAVER_URI_PATH)
							.query(journalpostParams)
							.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_JOURNALFOERT)
							.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_FORDELING)
							.queryParam(PARAM_NAME_STATUSKATEGORI, PARAM_VALUE_STATUSKATEGORI)
							.queryParam(PARAM_NAME_LIMIT, JOURNALPOSTER_PARTITION_LIMIT);
					if (offset > 0) {
						uriBuilder.queryParam(PARAM_NAME_OFFSET, offset);
					}
					return uriBuilder.build();
				})
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_OPPGAVE))
				.headers(httpHeaders -> httpHeaders.add(X_CORRELATION_ID, MDC.get(MDC_CALL_ID)))
				.httpRequest(httpRequest -> {
					HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
					reactorRequest.responseTimeout(ofSeconds(250));
				})
				.retrieve()
				.bodyToMono(FinnOppgaveResponse.class)
				.doOnError(handleError())
				.block();
	}

	private String mapJournalpostIdListToQueryParams(List<Long> values) {
		return values.stream()
				.map(value -> PARAM_NAME_JOURNALPOSTID + "=" + value)
				.collect(Collectors.joining("&"));
	}

	private Consumer<Throwable> handleError() {
		return error -> {
			if (error instanceof WebClientResponseException exception) {
				if (exception.getStatusCode().is4xxClientError()) {
					throw new FinnOppgaveFunctionalException("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s, feilmelding=%s".formatted(
							exception.getStatusCode(),
							exception.getResponseBodyAsString()),
							error);
				} else {
					throw new FinnOppgaveTechnicalException("finnOppgaveForJournalposter feilet teknisk med statusKode=%s, feilmelding=%s".formatted(
							exception.getStatusCode(),
							error.getMessage()),
							error);
				}
			} else {
				throw new FinnOppgaveTechnicalException("finnOppgaveForJournalposter feilet med ukjent teknisk feil, feilmelding=%s".formatted(
						error.getMessage()),
						error);
			}
		};
	}

}
