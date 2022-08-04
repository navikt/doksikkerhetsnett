package no.nav.doksikkerhetsnett.consumers;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureTokenConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import no.nav.doksikkerhetsnett.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class FinnMottatteJournalposterConsumer {
	private final RestTemplate restTemplate;
	private final DokSikkerhetsnettProperties.AzureEndpoint finnMottatteJournalposter;
	private final AzureTokenConsumer azureTokenConsumer;

	public FinnMottatteJournalposterConsumer(RestTemplateBuilder restTemplateBuilder,
											 DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
											 AzureTokenConsumer azureTokenConsumer) {
		this.finnMottatteJournalposter = dokSikkerhetsnettProperties.getEndpoints().getDokarkiv();
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(150))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.azureTokenConsumer = azureTokenConsumer;
	}

	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "finnMottatteJournalposter"}, percentiles = {0.5, 0.95}, histogram = true)
	public FinnMottatteJournalposterResponse finnMottatteJournalposter(String tema, int antallDager) {
		try {
			HttpEntity<?> requestEntity = new HttpEntity<>(createHeaders());

			URI uri = UriComponentsBuilder.fromHttpUrl(finnMottatteJournalposter.getUrl())
					.path(validateInput(tema))
					.path("/" + antallDager)
					.build().toUri();

			return restTemplate.exchange(uri, GET, requestEntity, FinnMottatteJournalposterResponse.class)
					.getBody();

		} catch (HttpClientErrorException e) {
			if (NOT_FOUND.equals(e.getStatusCode())) {
				throw new FinnMottatteJournalposterFinnesIkkeFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), finnMottatteJournalposter.getUrl()), e);
			} else if (CONFLICT.equals(e.getStatusCode())) {
				throw new FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			} else {
				throw new FinnMottatteJournalposterFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), finnMottatteJournalposter.getUrl()), e);
			}
		} catch (HttpServerErrorException e) {
			throw new FinnMottatteJournalposterTechnicalException(String.format("finnMottatteJournalposter feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private String validateInput(String input) {
		if (input == null) {
			return "";
		} else return input;
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(azureTokenConsumer.getClientCredentialToken(finnMottatteJournalposter.getScope()).getAccess_token());

		if (MDC.get(MDC_NAV_CALL_ID) != null) {
			headers.add(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
		}
		if (MDC.get(MDC_NAV_CONSUMER_ID) != null) {
			headers.add(MDC_NAV_CONSUMER_ID, MDC.get(MDC_NAV_CONSUMER_ID));
		}
		return headers;
	}
}