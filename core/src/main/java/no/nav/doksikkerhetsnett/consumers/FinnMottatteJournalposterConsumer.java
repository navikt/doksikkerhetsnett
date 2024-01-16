package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.NavHeaders.NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doksikkerhetsnett.consumers.azure.AzureProperties.CLIENT_REGISTRATION_DOKARKIV;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class FinnMottatteJournalposterConsumer {
	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final WebClient webClient;

	public FinnMottatteJournalposterConsumer(DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
											 WebClient webClient) {
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.webClient = webClient;
	}

	@Retryable(retryFor = FinnMottatteJournalposterTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public FinnMottatteJournalposterResponse finnMottatteJournalposter(String tema, int antallDager) {
		final String callId = MDC.get(MDC_CALL_ID);
		return webClient.get()
				.uri(buildUri(tema, antallDager))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKARKIV))
				.headers(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
					httpHeaders.set(NAV_CALL_ID, callId);
				})
				.retrieve()
				.bodyToMono(FinnMottatteJournalposterResponse.class)
				.doOnError(handleError(tema))
				.block();
	}

	private Consumer<Throwable> handleError(String tema) {
		return error -> {
			if (error instanceof WebClientResponseException webClientResponseException) {
				if (webClientResponseException.getStatusCode().is4xxClientError()) {
					throw new FinnMottatteJournalposterFunctionalException(format("finnMottatteJournalposter feilet funksjonelt for tema=%s med status=%s, feilmelding=%s",
							tema, webClientResponseException.getStatusCode(), webClientResponseException.getResponseBodyAsString()), error);
				} else {
					throw new FinnMottatteJournalposterTechnicalException(format("finnMottatteJournalposter feilet teknisk ved henting av tema=%s med status=%s, feilmelding=%s",
							tema, webClientResponseException.getStatusCode(), error.getMessage()),
							error);
				}
			} else {
				throw new FinnMottatteJournalposterTechnicalException(
						format("finnMottatteJournalposter feilet med ukjent teknisk feil ved henting av tema=%s, feilmelding=%s",
								tema, error.getMessage()),
						error);
			}
		};
	}

	private String buildUri(String tema, int antallDager) {
		return dokSikkerhetsnettProperties.getDokarkiv().getUrl() + validateInput(tema) + "/" + antallDager;
	}

	private String validateInput(String input) {
		if (input == null) {
			return "";
		} else return input;
	}
}