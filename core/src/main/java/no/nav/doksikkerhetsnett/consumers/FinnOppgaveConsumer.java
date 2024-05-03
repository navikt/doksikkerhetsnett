package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
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
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.NavHeaders.X_CORRELATION_ID;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_FORDELING;
import static no.nav.doksikkerhetsnett.entities.Oppgave.OPPGAVETYPE_JOURNALFOERT;
import static no.nav.doksikkerhetsnett.services.FinnGjenglemteJournalposterService.JOURNALPOSTER_PARTITION_LIMIT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class FinnOppgaveConsumer {

	private final String oppgaveUrl;
	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;

	private static final String PARAM_NAME_JOURNALPOSTID = "journalpostId";
	private static final String PARAM_NAME_OPPGAVETYPE = "oppgavetype";
	private static final String PARAM_NAME_STATUSKATEGORI = "statuskategori";
	private static final String PARAM_NAME_SORTERINGSREKKEFOLGE = "sorteringsrekkefolge";
	private static final String PARAM_NAME_LIMIT = "limit";
	private static final String PARAM_NAME_OFFSET = "offset";

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

	public FinnOppgaveResponse finnOppgaveForJournalposter(List<Long> ubehandledeJournalposter, int offset) {
		if (ubehandledeJournalposter == null || ubehandledeJournalposter.isEmpty()) {
			return null;
		}

		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);

			String journalpostParams = mapJournalpostIdListToQueryParams(ubehandledeJournalposter);

			URI uri = UriComponentsBuilder.fromHttpUrl(oppgaveUrl)
					.query(journalpostParams)
					.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_JOURNALFOERT)
					.queryParam(PARAM_NAME_OPPGAVETYPE, OPPGAVETYPE_FORDELING)
					.queryParam(PARAM_NAME_STATUSKATEGORI, "AAPEN")
					.queryParam(PARAM_NAME_SORTERINGSREKKEFOLGE, "ASC")
					.queryParam(PARAM_NAME_LIMIT, JOURNALPOSTER_PARTITION_LIMIT)
					.build().toUri();
			if (offset > 0) {
				uri = appendQuery(uri, Integer.toString(offset * JOURNALPOSTER_PARTITION_LIMIT));
			}

			return restTemplate.exchange(uri, GET, requestEntity, FinnOppgaveResponse.class).getBody();

		} catch (HttpClientErrorException e) {
			if (NOT_FOUND.equals(e.getStatusCode())) {
				throw new FinnOppgaveFinnesIkkeFunctionalException(format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
			} else if (CONFLICT.equals(e.getStatusCode())) {
				throw new FinnOppgaveTillaterIkkeTilknyttingFunctionalException(format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString()), e);
			} else {
				throw new FinnOppgaveFunctionalException(format("finnOppgaveForJournalposter feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
			}
		} catch (HttpServerErrorException e) {
			throw new FinnOppgaveTechnicalException(format("finnOppgaveForJournalposter feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private URI appendQuery(URI oldUri, String value) {
		String appendQuery = PARAM_NAME_OFFSET + "=" + value;
		String newQuery = oldUri.getQuery();
		if (newQuery == null) {
			newQuery = appendQuery;
		} else {
			newQuery += "&" + appendQuery;
		}
		try {
			return new URI(oldUri.getScheme(), oldUri.getAuthority(),
					oldUri.getPath(), newQuery, oldUri.getFragment());
		} catch (URISyntaxException e) {
			log.error("Append query feilet å legge til {} til uri'en {}", appendQuery, oldUri, e);
			return null;
		}

	}

	private String mapJournalpostIdListToQueryParams(List<Long> values) {
		return values.stream()
				.map(value -> PARAM_NAME_JOURNALPOSTID + "=" + value)
				.collect(Collectors.joining("&"));
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(stsRestConsumer.getOidcToken());
		headers.add(X_CORRELATION_ID, MDC.get(MDC_CALL_ID));
		return headers;
	}
}
