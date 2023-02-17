package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureProperties;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class FinnMottatteJournalposterConsumer {
	public static final String NAV_CALL_ID = "Nav-Callid";

	private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
	private final WebClient webClient;

	public FinnMottatteJournalposterConsumer(DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
											 ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
											 WebClient webClient) {
		this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.webClient = webClient;
	}

	@Retryable(include = FinnMottatteJournalposterTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MAX_ATTEMPTS_SHORT))
	public FinnMottatteJournalposterResponse finnMottatteJournalposter(String tema, int antallDager) {
		return webClient.get()
				.uri(buildUri(tema, antallDager))
				.attributes(getOAuth2AuthorizedClient())
				.headers(this::createHeaders)
				.retrieve()
				.bodyToMono(FinnMottatteJournalposterResponse.class)
				.doOnError(handleError(tema))
				.block();
	}

	private Consumer<Throwable> handleError(String tema) {
		return error -> {
			if (error instanceof WebClientResponseException && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				WebClientResponseException thrownError = (WebClientResponseException) error;
				throw new FinnMottatteJournalposterFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt for tema=%S med statusKode=%s. Feilmelding=%s",
						tema, thrownError.getRawStatusCode(), thrownError.getResponseBodyAsString()), error);

			} else {
				throw new FinnMottatteJournalposterTechnicalException(
						String.format("finnMottatteJournalposter feilet teknisk ved henting av tema={} ,feilmelding=%s",
								tema,
								error.getMessage()),
						error);
			}
		};
	}

	private void createHeaders(HttpHeaders httpHeaders) {
		httpHeaders.setContentType(APPLICATION_JSON);
		httpHeaders.set(NAV_CALL_ID, UUID.randomUUID().toString());
	}

	private String buildUri(String tema, int antallDager) {
		return dokSikkerhetsnettProperties.getDokarkiv().getUrl() + validateInput(tema) + "/" + antallDager;
	}

	private String validateInput(String input) {
		if (input == null) {
			return "";
		} else return input;
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(AzureProperties.getOAuth2AuthorizeRequestForAzure(AzureProperties.CLIENT_REGISTRATION_DOKARKIV));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}

}