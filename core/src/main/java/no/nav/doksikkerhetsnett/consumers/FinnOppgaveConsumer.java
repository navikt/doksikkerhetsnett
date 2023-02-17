package no.nav.doksikkerhetsnett.consumers;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.constants.MDCConstants;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
import no.nav.doksikkerhetsnett.utils.Utils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_FORDELING;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_JOURNALFOERT;
import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class FinnOppgaveConsumer {

	private final String oppgaveUrl;
	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;

	public static final String CORRELATION_HEADER = "X-Correlation-Id";
	private static final String PARAM_NAME_JOURNALPOSTID = "journalpostId";
	private static final String PARAM_NAME_OPPGAVETYPE = "oppgavetype";
	private static final String PARAM_NAME_STATUSKATEGORI = "statuskategori";
	private static final String PARAM_NAME_SORTERINGSREKKEFOLGE = "sorteringsrekkefolge";
	private static final String PARAM_NAME_LIMIT = "limit";
	private static final String PARAM_NAME_OFFSET = "offset";
	private static final int LIMIT = 50;

	public FinnOppgaveConsumer(RestTemplateBuilder restTemplateBuilder,
							   DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
							   StsRestConsumer stsRestConsumer) {
		this.oppgaveUrl = dokSikkerhetsnettProperties.getEndpoints().getOppgave();
		this.stsRestConsumer = stsRestConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(250))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(),
						dokSikkerhetsnettProperties.getServiceuser().getPassword())
				.build();
	}

	public FinnOppgaveResponse finnOppgaveForJournalposter(List<Journalpost> ubehandledeJournalposter) {
		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);
			List<List<Long>> partitionedIds = Utils.journalpostListToPartitionedJournalpostIdList(ubehandledeJournalposter, LIMIT);
			ArrayList<FinnOppgaveResponse> oppgaveResponses = new ArrayList<>();

			for (List<Long> ids : partitionedIds) {
				FinnOppgaveResponse oppgaveResponse = executeGetRequest(ids, requestEntity, 0);
				oppgaveResponses.add(oppgaveResponse);
				if (oppgaveResponse != null) {
					int differenceBetweenTotalReponsesAndResponseList = oppgaveResponse.getAntallTreffTotalt() - oppgaveResponse.getOppgaver()
							.size();
					if (differenceBetweenTotalReponsesAndResponseList != 0) {
						int extraPages = differenceBetweenTotalReponsesAndResponseList / LIMIT;
						for (int i = 1; i <= extraPages + 1; i++) {
							oppgaveResponses.add(executeGetRequest(ids, requestEntity, i));
						}
					}
				}
			}
			List<Oppgave> allOppgaveResponses = oppgaveResponses.stream()
					.flatMap(finnOppgaveResponse -> finnOppgaveResponse != null ? finnOppgaveResponse.getOppgaver().stream() : null)
					.collect(Collectors.toList());

			return FinnOppgaveResponse.builder()
					.oppgaver(allOppgaveResponses)
					.build();

		} catch (HttpClientErrorException e) {
			if (NOT_FOUND.equals(e.getStatusCode())) {
				throw new FinnOppgaveFinnesIkkeFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
			} else if (CONFLICT.equals(e.getStatusCode())) {
				throw new FinnOppgaveTillaterIkkeTilknyttingFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			} else {
				throw new FinnOppgaveFunctionalException(String.format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
			}
		} catch (HttpServerErrorException e) {
			throw new FinnOppgaveTechnicalException(String.format("finnOppgaveForJournalposter feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private FinnOppgaveResponse executeGetRequest(List<Long> ids, HttpEntity<?> requestEntity, int offset) {
		String journalpostParams = Utils.listOfLongsToQueryParams(ids, PARAM_NAME_JOURNALPOSTID);
		URI uri = UriComponentsBuilder.fromHttpUrl(oppgaveUrl)
				.query(journalpostParams)
				.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_JOURNALFOERT)
				.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_FORDELING)
				.queryParam(PARAM_NAME_STATUSKATEGORI, "AAPEN")
				.queryParam(PARAM_NAME_SORTERINGSREKKEFOLGE, "ASC")
				.queryParam(PARAM_NAME_LIMIT, LIMIT)
				.build().toUri();
		if (offset > 0) {
			uri = Utils.appendQuery(uri, PARAM_NAME_OFFSET, Integer.toString(offset * LIMIT));
		}
		return restTemplate.exchange(uri, GET, requestEntity, FinnOppgaveResponse.class)
				.getBody();
	}


	private HttpHeaders createHeaders() {

		if (MDC.get(MDC_CALL_ID) == null) {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
		}

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(APPLICATION_JSON);
		headers.set(AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(CORRELATION_HEADER, MDC.get(MDCConstants.MDC_CALL_ID));
		return headers;
	}
}
