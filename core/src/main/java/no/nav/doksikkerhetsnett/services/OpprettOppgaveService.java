package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumers.pdl.IdentConsumer;
import no.nav.doksikkerhetsnett.consumers.pdl.PdlFunctionalException;
import no.nav.doksikkerhetsnett.consumers.pdl.PersonIkkeFunnetException;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.JiraResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.entities.Bruker.TYPE_PERSON;
import static no.nav.doksikkerhetsnett.entities.Oppgave.BESKRIVELSE_GJENOPPRETTET;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

@Slf4j
@Service
public class OpprettOppgaveService {

    private final OpprettOppgaveConsumer opprettOppgaveConsumer;
    private final JiraConsumer jiraConsumer;
    private IdentConsumer identConsumer;

    public OpprettOppgaveService(OpprettOppgaveConsumer opprettOppgaveConsumer, JiraConsumer jiraConsumer, IdentConsumer identConsumer) {
        this.opprettOppgaveConsumer = opprettOppgaveConsumer;
        this.jiraConsumer = jiraConsumer;
        this.identConsumer = identConsumer;
    }

    public List<OpprettOppgaveResponse> opprettOppgaver(List<Journalpost> journalposts) {
        return journalposts.stream()
                .map(jp -> opprettOppgave(createOppgaveFromJournalpost(jp)))
                .collect(Collectors.toList());
    }

    public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {
        try {
            return opprettOppgaveConsumer.opprettOppgave(oppgave);
        } catch (HttpClientErrorException e) {
            return opprettOppgaveMedLiteMetadata(oppgave);
        }
    }

    public OpprettOppgaveResponse opprettOppgaveMedLiteMetadata(Oppgave oppgave) {
        try {
            log.info("Klarte ikke opprette oppgave med oppgavetype {}. Prøver å opprette oppgave med oppgavetype {}", oppgave.getOppgavetype(), Oppgave.OPPGAVETYPE_FORDELING);
            return opprettOppgaveConsumer.opprettOppgave(createFDRFromJFR(oppgave));
        } catch (HttpClientErrorException e) {
            JiraResponse response = jiraConsumer.opprettJiraIssue(oppgave, e);
            log.info("Doksikkerhetsnett opprettet en jira-issue med kode {}", response.getKey());
            throw new OpprettOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s.", e
                    .getStatusCode(), e.getResponseBodyAsString()), e);
        }
    }

    private Oppgave createOppgaveFromJournalpost(Journalpost jp) {
        String tildeltEnhetsnr = extractEnhetsNr(jp);
        String tema = extractTema(jp);

        return Oppgave.builder()
                .tildeltEnhetsnr(tildeltEnhetsnr)
                .opprettetAvEnhetsnr(Oppgave.ENHETSNUMMER_GENERISK)
                .journalpostId(Long.toString(jp.getJournalpostId()))
                .tema(tema)
                .behandlingstema(jp.getBehandlingstema())
                .oppgavetype(Oppgave.OPPGAVETYPE_JOURNALFOERT)
                .prioritet(Oppgave.PRIORITET_NORMAL)
                .aktivDato(new Date())
                .aktoerId(this.findAktorId(jp))
                .beskrivelse(BESKRIVELSE_GJENOPPRETTET)
                .build();
    }

    private Oppgave createFDRFromJFR(Oppgave gammel_oppgave) {
        return Oppgave.builder()
                .tildeltEnhetsnr(null)
                .opprettetAvEnhetsnr(Oppgave.ENHETSNUMMER_GENERISK)
                .journalpostId(gammel_oppgave.getJournalpostId())
                .tema(gammel_oppgave.getTema())
                .behandlingstema(null)
                .oppgavetype(Oppgave.OPPGAVETYPE_FORDELING)
                .prioritet(Oppgave.PRIORITET_NORMAL)
                .aktivDato(new Date())
                .aktoerId(gammel_oppgave.getAktoerId())
                .beskrivelse(gammel_oppgave.getBeskrivelse())
                .build();
    }

    private String findAktorId(Journalpost jp) {
        String fnr = jp.getBruker() != null ? jp.getBruker().getId() : null;

        if (isNotBlank(fnr) && TYPE_PERSON.equals(jp.getBruker().getType())) {
            log.info("Prøver å finne en aktorId tilknyttet til journalPost med journalpostId {}", jp.getJournalpostId());
            try {
                log.info("Fant en aktorId tilknyttet til journalPost med journalpostId {}", jp.getJournalpostId());
                return identConsumer.hentAktoerId(fnr);
            } catch (PersonIkkeFunnetException | PdlFunctionalException | HttpServerErrorException e) {
                log.warn("Kan ikke utføre PDL tilgangskontroll for bruker tilknyttet til journalpost med journalpostId: " + jp.getJournalpostId(), e);
            }
        }
        return null;
    }

    private String extractEnhetsNr(Journalpost jp) {
        return Journalpost.ENHETSNUMMER_GENERISK.equals(jp.getJournalforendeEnhet()) ? "" : jp.getJournalforendeEnhet();
    }

    private String extractTema(Journalpost jp) {
        if (jp.getTema() == null || Oppgave.TEMA_UKJENT.equals(jp.getTema())) {
            return Oppgave.TEMA_GENERELL;
        }
        return jp.getTema();
    }
}
