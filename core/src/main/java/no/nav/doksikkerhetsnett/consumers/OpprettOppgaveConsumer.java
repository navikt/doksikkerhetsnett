package no.nav.doksikkerhetsnett.consumers;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.constants.MDCConstants;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.FinOppgaveTillaterIkkeTilknyttingFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFinnesIkkeFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.functional.FinnOppgaveFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.FinnOppgaveTechnicalException;
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

import static no.nav.doksikkerhetsnett.constants.DomainConstants.APP_NAME;
import static no.nav.doksikkerhetsnett.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.CORRELATION_HEADER;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_NAV_CONSUMER_ID;
import static no.nav.doksikkerhetsnett.constants.MDCConstants.UUID_HEADER;

@Component
public class OpprettOppgaveConsumer {

    RestTemplate restTemplate;
    StsRestConsumer stsRestConsumer;

    private final String opprettOppgaveUrl;

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
        this.opprettOppgaveUrl = dokSikkerhetsnettProperties.getOpprettoppgaveUrl();
    }

    public void opprettOppgave(Journalpost jp) {
        try {
            String tildeltEnhetsnr = jp.getJournalforendeEnhet() != null ? jp.getJournalforendeEnhet() : "";
            String orgnr = jp.getBruker().getType().equals(Bruker.TYPE_ORGANISASJON) ? jp.getBruker().getId() : "";
            String bnr = jp.getBruker().getType().equals(Bruker.TYPE_PERSON) ? jp.getBruker().getId() : "";
            Oppgave oppgave = Oppgave.builder()
                    .tildeltEnhetsnr(tildeltEnhetsnr)
                    .opprettetAvEnhetsnr("9999")
                    .journalpostId(jp.getJournalStatus())
                    .orgnr(orgnr)
                    .bnr(bnr)
                    .beskrivelse("")
                    .tema(jp.getTema())
                    .behandlingstema(jp.getBehandlingstema())
                    .oppgavetype("JFR")
                    .build();

            HttpHeaders headers = createHeaders();
            HttpEntity<Oppgave> requestEntity = new HttpEntity<>(oppgave, headers);
            restTemplate.exchange(opprettOppgaveUrl, HttpMethod.POST, requestEntity, FinnOppgaveResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new FinnOppgaveFinnesIkkeFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
                        .getStatusCode(), e.getResponseBodyAsString(), opprettOppgaveUrl), e);
            } else if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                throw new FinOppgaveTillaterIkkeTilknyttingFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s", e
                        .getStatusCode(), e.getResponseBodyAsString()), e);
            } else {
                throw new FinnOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
                        .getStatusCode(), e.getResponseBodyAsString(), opprettOppgaveUrl), e);
            }
        } catch (HttpServerErrorException e) {
            throw new FinnOppgaveTechnicalException(String.format("opprettOppgave feilet teknisk med statusKode=%s. Feilmelding=%s", e
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
}
