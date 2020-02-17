package no.nav.doksikkerhetsnett.consumer;

import static no.nav.doksikkerhetsnett.config.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.config.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;

import no.nav.doksikkerhetsnett.config.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.exceptions.functional.AbstractDoksikkerhetsnettFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.AbstractDoksikkerhetsnettTechnicalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnMottatteJournalposterTechnicalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class FinnMottatteJournalposterConsumer {
	private final RestTemplate restTemplate;
	private final String finnMottatteJournalposterUrl;
	private int metricsAntallJournalposter = 0;

	public FinnMottatteJournalposterConsumer(RestTemplateBuilder restTemplateBuilder,
											 DokSikkerhetsnettProperties dokSikkerhetsnettProperties) {
		this.finnMottatteJournalposterUrl = dokSikkerhetsnettProperties.getFinnmottattejournalposterurl();
		this.restTemplate = restTemplateBuilder
				/* TODO: Hvor liberal skal timeouten være?
				 *  Første kallet vil ta lang tid da det kan være 3 år gamle journalposter
				 *  alle kall etter dette burde ta langt mindre tid.
				 */
				.setReadTimeout(Duration.ofSeconds(3600))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(),
						dokSikkerhetsnettProperties.getServiceuser().getPassword())
				.build();
	}

	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "finnMottatteJournalposter"}, percentiles = {0.5, 0.95}, histogram = true, createAntallJournalposterMetric = true)
	public FinnMottatteJournalposterResponse finnMottatteJournalposter(String temaer) {
		List<UbehandletJournalpost> dummyData = new ArrayList<>();
		int randomNumberOfResponses = new Random().nextInt(1000);
		if (randomNumberOfResponses > 975)
			throw new AbstractDoksikkerhetsnettTechnicalException("Dummy teknisk feil");
		if (randomNumberOfResponses > 900)
			throw new AbstractDoksikkerhetsnettFunctionalException("Dummy funksjonell feil");

		metricsAntallJournalposter = randomNumberOfResponses;
		for (int i = 0; i < randomNumberOfResponses; i++)
			dummyData.add(new UbehandletJournalpost());
		return FinnMottatteJournalposterResponse.builder().journalposter(dummyData).build();

		/*
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers = createHeader(headers);
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);

			URI uri = UriComponentsBuilder.fromHttpUrl(finnMottatteJournalposterUrl)
					.path(temaer)
					.build().toUri();
			FinnMottatteJournalposterResponse response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, FinnMottatteJournalposterResponse.class)
					.getBody();
			metricsAntallJournalposter = response.getJournalposter().size();
			return response;

		} catch (HttpClientErrorException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				throw new FinnMottatteJournalposterFinnesIkkeFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getMessage(), finnMottatteJournalposterUrl), e);
			} else if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
				throw new FinnMottatteJournalposterTillaterIkkeTilknyttingFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getMessage()), e);
			} else {
				throw new FinnMottatteJournalposterFinnesIkkeFunctionalException(String.format("finnMottatteJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getMessage(), finnMottatteJournalposterUrl), e);
			}
		} catch (HttpServerErrorException e) {
			throw new FinnMottatteJournalposterTechnicalException(String.format("finnMottatteJournalposter feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
		 */
	}

	public int getMetricsAntallJournalposter() {
		return metricsAntallJournalposter;
	}

	private HttpHeaders createHeader(HttpHeaders headers) {
		if (MDC.get(MDC_NAV_CALL_ID) != null) {
			headers.add(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
		}

		if (MDC.get(MDC_NAV_CONSUMER_ID) != null) {
			headers.add(MDC_NAV_CONSUMER_ID, MDC.get(MDC_NAV_CONSUMER_ID));
		}
		return headers;
	}
}