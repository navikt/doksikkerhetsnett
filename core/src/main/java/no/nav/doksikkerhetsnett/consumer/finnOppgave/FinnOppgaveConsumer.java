package no.nav.doksikkerhetsnett.consumer.finnOppgave;

import static no.nav.doksikkerhetsnett.constants.DomainConstants.APP_NAME;
import static no.nav.doksikkerhetsnett.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;

import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.exceptions.functional.AbstractDoksikkerhetsnettFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.AbstractDoksikkerhetsnettTechnicalException;
import no.nav.doksikkerhetsnett.jaxws.MDCGenerate;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.constants.MDCConstants;
import no.nav.doksikkerhetsnett.consumer.sts.StsRestConsumer;
import no.nav.doksikkerhetsnett.exceptions.functional.FinOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
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
public class FinnOppgaveConsumer {

	private static final String CORRELATION_HEADER = "X-Correlation-Id";
	private static final String UUID_HEADER = "X-Uuid";
	private final String finnOppgaverUrl;
	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;
	private int metricsAntallOppgaver;

	public FinnOppgaveConsumer(RestTemplateBuilder restTemplateBuilder,
							   DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
							   StsRestConsumer stsRestConsumer) {
		this.finnOppgaverUrl = dokSikkerhetsnettProperties.getFinnoppgaverurl();
		this.stsRestConsumer = stsRestConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(250))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(),
						dokSikkerhetsnettProperties.getServiceuser().getPassword())
				.build();
	}

	@Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "finnOppgaveForJournalposter"}, percentiles = {0.5, 0.95}, histogram = true)
	public FinnOppgaveResponse finnOppgaveForJournalposter(String journalpostIds) {
		try {
			List<OppgaveJson> dummyData = new ArrayList<>();
			int randomNumberOfResponses = new Random().nextInt(20);

			for (int i = 0; i < randomNumberOfResponses; i++)
				dummyData.add(new OppgaveJson());
			return FinnOppgaveResponse.builder().oppgaver(dummyData).build();

		/* TODO: Dette er gjort for testing av metrikker for å unngå kall mot oppgave. Husk å fiks før merging!
		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);

			URI uri = UriComponentsBuilder.fromHttpUrl(finnOppgaverUrl)
					.queryParam(journalpostIds)
					.build().toUri();
			return restTemplate.exchange(uri, HttpMethod.GET, requestEntity, FinnOppgaveResponse.class)
					.getBody();
*/
		} catch (HttpClientErrorException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				throw new FinnOppgaveFinnesIkkeFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getMessage(), finnOppgaverUrl), e);
			} else if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
				throw new FinOppgaveTillaterIkkeTilknyttingFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getMessage()), e);
			} else {
				throw new FinnOppgaveFinnesIkkeFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getMessage(), finnOppgaverUrl), e);
			}
		} catch (HttpServerErrorException e) {
			throw new FinnOppgaveTechnicalException(String.format("finnOppgaveForJournalposter feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		MDCGenerate.generateNewCallIdIfThereAreNone();
		HttpHeaders headers = new HttpHeaders();
		
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(CORRELATION_HEADER, MDC.get(MDCConstants.MDC_CALL_ID));
		headers.add(UUID_HEADER, MDC.get(MDCConstants.MDC_CALL_ID));
		headers.add(MDC_NAV_CONSUMER_ID, APP_NAME);
		headers.add(MDC_NAV_CALL_ID, MDC.get(MDC_NAV_CALL_ID));
		return headers;
	}

	public int getMetricsAntallOppgaver() {
		return metricsAntallOppgaver;
	}
}
