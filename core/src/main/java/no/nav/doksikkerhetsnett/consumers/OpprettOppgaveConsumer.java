package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.OpprettOppgaveTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class OpprettOppgaveConsumer {

	public static final String CORRELATION_HEADER = "X-Correlation-Id";

	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;
	private final String oppgaveUrl;

	public OpprettOppgaveConsumer(RestTemplateBuilder restTemplateBuilder,
								  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
								  StsRestConsumer stsRestConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(250))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(),
						dokSikkerhetsnettProperties.getServiceuser().getPassword())
				.build();
		this.stsRestConsumer = stsRestConsumer;
		this.oppgaveUrl = dokSikkerhetsnettProperties.getEndpoints().getOppgave();
	}

	public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {
		try {
			HttpHeaders headers = createHeaders();
			HttpEntity<Oppgave> requestEntity = new HttpEntity<>(oppgave, headers);

			return restTemplate.exchange(oppgaveUrl, POST, requestEntity, OpprettOppgaveResponse.class).getBody();
		} catch (HttpClientErrorException e) {
			if (BAD_REQUEST.equals(e.getStatusCode())) {
				throw e;
			}
			throw new OpprettOppgaveFunctionalException(format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s",
					e.getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
		} catch (HttpServerErrorException e) {
			throw new OpprettOppgaveTechnicalException(format("opprettOppgave feilet teknisk med statusKode=%s. Feilmelding=%s",
					e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(stsRestConsumer.getOidcToken());
		headers.add(CORRELATION_HEADER, MDC.get(MDC_CALL_ID));

		return headers;
	}
}
