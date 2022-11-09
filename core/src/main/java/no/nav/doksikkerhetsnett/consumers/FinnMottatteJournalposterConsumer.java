package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureProperties;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import no.nav.doksikkerhetsnett.metrics.Metrics;
import org.slf4j.MDC;
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
import java.util.function.Consumer;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class FinnMottatteJournalposterConsumer {

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

	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "finnMottatteJournalposter"}, percentiles = {0.5, 0.95}, histogram = true)
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
				log.error("finnMottatteJournalposter feilet funksjonelt med statuscode={} ved henting av tema={} , feilmelding={}", thrownError.getStatusCode(), tema, thrownError.getMessage());
				throw new FinnMottatteJournalposterFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s",
						thrownError.getRawStatusCode(), thrownError.getResponseBodyAsString()), error);

			} else {
				log.error("finnMottatteJournalposter feilet teknisk ved henting av tema={}, feilmelding={}", tema, error.getMessage());
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

		if (MDC.get(MDC_NAV_CALL_ID) != null) {
			httpHeaders.set(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
		}
		if (MDC.get(MDC_NAV_CONSUMER_ID) != null) {
			httpHeaders.set(MDC_NAV_CONSUMER_ID, MDC.get(MDC_NAV_CONSUMER_ID));
		}
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