package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.OpprettOppgaveTechnicalException;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClientRequest;

import java.util.function.Consumer;

import static java.time.Duration.ofSeconds;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.NavHeaders.X_CORRELATION_ID;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doksikkerhetsnett.consumers.azure.AzureProperties.CLIENT_REGISTRATION_OPPGAVE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class OpprettOppgaveConsumer {

	private final WebClient webClient;

	private static final String OPPGAVER_URI_PATH = "/api/v1/oppgaver";

	public OpprettOppgaveConsumer(DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
								  WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokSikkerhetsnettProperties.getEndpoints().getOppgave().getUrl())
				.defaultHeaders(httpHeaders -> httpHeaders.setContentType(APPLICATION_JSON))
				.build();
	}

	@Retryable(retryFor = OpprettOppgaveTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {

		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path(OPPGAVER_URI_PATH).build())
				.bodyValue(oppgave)
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_OPPGAVE))
				.headers(httpHeaders -> httpHeaders.add(X_CORRELATION_ID, MDC.get(MDC_CALL_ID)))
				.httpRequest(httpRequest -> {
					HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
					reactorRequest.responseTimeout(ofSeconds(250));
				})
				.retrieve()
				.bodyToMono(OpprettOppgaveResponse.class)
				.doOnError(handleError())
				.block();
	}

	private Consumer<Throwable> handleError() {
		return error -> {
			if (error instanceof WebClientResponseException exception) {
				if (exception.getStatusCode().is4xxClientError()) {
					//Denne blir spesialhåndtert i OpprettOppgaveService::opprettOppgave
					if (exception.getStatusCode().isSameCodeAs(BAD_REQUEST))
						throw exception;

					throw new OpprettOppgaveFunctionalException("opprettOppgave feilet funksjonelt med statusKode=%s, feilmelding=%s".formatted(
							exception.getStatusCode(),
							exception.getResponseBodyAsString()),
							error);
				} else {
					throw new OpprettOppgaveTechnicalException("opprettOppgave feilet teknisk med statusKode=%s, feilmelding=%s".formatted(
							exception.getStatusCode(),
							error.getMessage()),
							error);
				}
			} else {
				throw new OpprettOppgaveTechnicalException("opprettOppgave feilet med ukjent teknisk feil, feilmelding=%s".formatted(
						error.getMessage()),
						error);
			}
		};
	}

}
