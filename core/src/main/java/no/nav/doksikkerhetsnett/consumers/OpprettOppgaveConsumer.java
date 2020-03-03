package no.nav.doksikkerhetsnett.consumers;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.constants.MDCConstants;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.JiraResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
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
import java.util.Date;

import static no.nav.doksikkerhetsnett.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.doksikkerhetsnett.constants.DomainConstants.CORRELATION_HEADER;

@Slf4j
@Component
public class OpprettOppgaveConsumer {

    private final RestTemplate restTemplate;
    private final StsRestConsumer stsRestConsumer;
    private final JiraConsumer jiraConsumer;

    private final String oppgaveUrl;

    public OpprettOppgaveConsumer(RestTemplateBuilder restTemplateBuilder,
                                  DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
                                  StsRestConsumer stsRestConsumer,
                                  JiraConsumer jiraConsumer) {
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(250))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(),
                        dokSikkerhetsnettProperties.getServiceuser().getPassword())
                .build();
        this.stsRestConsumer = stsRestConsumer;
        this.jiraConsumer = jiraConsumer;
        this.oppgaveUrl = dokSikkerhetsnettProperties.getOppgaveurl();
    }

    public OpprettOppgaveResponse opprettOppgave(Journalpost jp) {
        Oppgave oppgave = createOppgaveFromJournalpost(jp);
        return postOpprettOppgave(oppgave);
    }

    private OpprettOppgaveResponse postOpprettOppgave(Oppgave oppgave) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Oppgave> requestEntity = new HttpEntity<>(oppgave, headers);
             return restTemplate.exchange(oppgaveUrl, HttpMethod.POST, requestEntity, OpprettOppgaveResponse.class)
                    .getBody();

        } catch (HttpClientErrorException e) {
            if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                return handterFeilsituasjon(e, oppgave);
            }
            throw new FinnOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
                    .getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
        } catch (HttpServerErrorException e) {
            throw new FinnOppgaveTechnicalException(String.format("opprettOppgave feilet teknisk med statusKode=%s. Feilmelding=%s", e
                    .getStatusCode(), e.getMessage()), e);
        }
    }

    private OpprettOppgaveResponse handterFeilsituasjon(HttpClientErrorException e, Oppgave oppgave) {
        if (e.getResponseBodyAsString().contains("Enheten med nummeret '" + oppgave.getTildeltEnhetsnr() + "' eksisterer ikke")) {
            log.info("Enheten med nummeret '" + oppgave.getTildeltEnhetsnr() + "' eksisterer ikke, så prøver på nytt uten enhetsnr");
            oppgave.setTildeltEnhetsnr(null);
            return postOpprettOppgave(oppgave);
        }
        // TODO: Gjør en sjekk på "Finner ikke ansvarlig enhet" og behandle som beskrevet i dok
        else {
            JiraResponse response = jiraConsumer.OpprettJiraIssue(oppgave, e);
            log.info("Doksikkerhetsnett opprettet en jira-issue med kode {}", response.getKey());
            throw new FinnOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s. Url=%s", e
                    .getStatusCode(), e.getResponseBodyAsString(), oppgaveUrl), e);
        }
    }

    private Oppgave createOppgaveFromJournalpost(Journalpost jp) {
        String tildeltEnhetsnr = jp.getJournalforendeEnhet();
        String orgnr = extractOrgnr(jp);
        String bnr = extractBnr(jp);
        String tema = extractTema(jp);

        return Oppgave.builder()
                .tildeltEnhetsnr(tildeltEnhetsnr)
                .opprettetAvEnhetsnr(Oppgave.ENHETSNUMMER_GENERISK)
                .journalpostId(jp.getJournalpostId() + "")
                .orgnr(orgnr)
                .bnr(bnr)
                .beskrivelse("")
                .tema(tema)
                .behandlingstema(jp.getBehandlingstema())
                .oppgavetype(Oppgave.OPPGAVETYPE_JOURNALFOERT)
                .prioritet(Oppgave.PRIORITET_NORMAL)
                .aktivDato(new Date())
                .build();
    }

    private String extractOrgnr(Journalpost jp) {
        if (jp.getBruker() != null && jp.getBruker().getType().equals(Bruker.TYPE_ORGANISASJON)) {
            return jp.getBruker().getId();
        }
        return null;
    }

    private String extractBnr(Journalpost jp) {
        if (jp.getBruker() != null && jp.getBruker().getType().equals(Bruker.TYPE_PERSON)) {
            return jp.getBruker().getId();
        }
        return null;
    }

    private String extractTema(Journalpost jp) {
        if (jp.getTema() == null || jp.getTema().equals(Oppgave.TEMA_UKJENT)) {
            return Oppgave.TEMA_GENERELL;
        }
        return jp.getTema();
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
