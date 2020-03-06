package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.JiraConsumer;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.JiraResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.OpprettOppgaveFunctionalException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpprettOppgaveService {

    private final OpprettOppgaveConsumer opprettOppgaveConsumer;
    private final JiraConsumer jiraConsumer;

    public OpprettOppgaveService(OpprettOppgaveConsumer opprettOppgaveConsumer, JiraConsumer jiraConsumer) {
        this.opprettOppgaveConsumer = opprettOppgaveConsumer;
        this.jiraConsumer = jiraConsumer;
    }

    public List<OpprettOppgaveResponse> opprettOppgaver(List<Journalpost> journalposts) {
        return journalposts.stream()
                .map(jp -> opprettOppgave(createOppgaveFromJournalpost(jp)))
                .collect(Collectors.toList());
    }

    public OpprettOppgaveResponse opprettOppgave(Oppgave oppgave) {
        try {
            OpprettOppgaveResponse response = opprettOppgaveConsumer.opprettOppgave(oppgave);
            return response;
        } catch (HttpClientErrorException e) {
            return handterFeilsituasjon(e, oppgave);
        }
    }

    private OpprettOppgaveResponse handterFeilsituasjon(HttpClientErrorException e, Oppgave oppgave) {
        if (e.getResponseBodyAsString().contains("Enheten med nummeret '" + oppgave.getTildeltEnhetsnr() + "' eksisterer ikke")) {
            log.info("Enheten med nummeret '{}' eksisterer ikke, så prøver på nytt uten enhetsnr", oppgave.getTildeltEnhetsnr());
            Oppgave oppgaveUtenTildeltEnhetsnr = new Oppgave(oppgave);
            oppgaveUtenTildeltEnhetsnr.setTildeltEnhetsnr(null);
            return opprettOppgave(oppgaveUtenTildeltEnhetsnr);
        }
        if (e.getResponseBodyAsString().contains("Fant ingen gyldig arbeidsfordeling for oppgave")) {
            log.info("Fant ikke ansvarlig enhet for oppgavetype {}, prøver på nytt med oppgavetype {}", oppgave.getOppgavetype(), Oppgave.OPPGAVETYPE_FORDELING);
            Oppgave oppgaveMedTypeFordeling = new Oppgave(oppgave);
            oppgaveMedTypeFordeling.setOppgavetype(Oppgave.OPPGAVETYPE_FORDELING);
            return opprettOppgave(oppgaveMedTypeFordeling);
        }

        JiraResponse response = jiraConsumer.OpprettJiraIssue(oppgave, e);
        log.info("Doksikkerhetsnett opprettet en jira-issue med kode {}", response.getKey());
        throw new OpprettOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s.", e
                .getStatusCode(), e.getResponseBodyAsString()), e);
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
}
