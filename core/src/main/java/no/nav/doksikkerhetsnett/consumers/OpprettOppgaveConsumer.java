package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.constants.MDCConstants;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.OpprettOppgaveTechnicalException;
import no.nav.doksikkerhetsnett.jaxws.MDCGenerate;
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

import java.time.Duration;

import static no.nav.doksikkerhetsnett.constants.DomainConstants.BEARER_PREFIX;

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
        this.oppgaveUrl = dokSikkerhetsnettProperties.getOppgaveurl();
    }

    public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Oppgave> requestEntity = new HttpEntity<>(oppgave, headers);
            return restTemplate.exchange(oppgaveUrl, HttpMethod.POST, requestEntity, OpprettOppgaveResponse.class)
                    .getBody();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                throw e;
            }
            throw new OpprettOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
                    .getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
        } catch (HttpServerErrorException e) {
            throw new OpprettOppgaveTechnicalException(String.format("opprettOppgave feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }

    private HttpHeaders createHeaders() {
        MDCGenerate.generateNewCallIdIfThereAreNone();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
        headers.add(CORRELATION_HEADER, MDC.get(MDCConstants.MDC_CALL_ID));
        return headers;
    }
}
