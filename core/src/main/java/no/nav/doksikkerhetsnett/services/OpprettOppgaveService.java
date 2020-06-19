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
            return opprettOppgaveConsumer.opprettOppgave(oppgave);
        } catch (HttpClientErrorException e) {
            try{
                log.info("Klarte ikke opprette oppgave med oppgavetype=%s. Prøver å opprette oppgave med oppgavetype=%s", oppgave.getOppgavetype(), Oppgave.OPPGAVETYPE_FORDELING);
                return opprettOppgaveConsumer.opprettOppgave(createFDRFromJFR(oppgave));
            }
            catch(HttpClientErrorException ee) {
                return handterFeilsituasjon(ee, oppgave);
            }
        }
    }

    private OpprettOppgaveResponse handterFeilsituasjon(HttpClientErrorException e, Oppgave oppgave) {
        log.error("Klarte ikke å opprette oppgave, fikk feilmelding fra oppgave: {}", e.getResponseBodyAsString(), e);
        if (e.getResponseBodyAsString().contains("Enheten med nummeret '" + oppgave.getTildeltEnhetsnr() + "' eksisterer ikke")) {
            log.info("Enheten med nummeret '{}' eksisterer ikke, så prøver på nytt uten enhetsnr", oppgave.getTildeltEnhetsnr());
            Oppgave oppgaveUtenTildeltEnhetsnr = new Oppgave(oppgave);
            oppgaveUtenTildeltEnhetsnr.setTildeltEnhetsnr(null);
            return opprettOppgave(oppgaveUtenTildeltEnhetsnr);
        }
        JiraResponse response = jiraConsumer.opprettJiraIssue(oppgave, e);
        log.info("Doksikkerhetsnett opprettet en jira-issue med kode {}", response.getKey());
        throw new OpprettOppgaveFunctionalException(String.format("opprettOppgave feilet funksjonelt med statusKode=%s. Feilmelding=%s.", e
                .getStatusCode(), e.getResponseBodyAsString()), e);
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
                .build();
    }

    private Oppgave createFDRFromJFR(Oppgave o) {
        return Oppgave.builder()
                .tildeltEnhetsnr("")
                .opprettetAvEnhetsnr(Oppgave.ENHETSNUMMER_GENERISK)
                .journalpostId(o.getJournalpostId())
                .tema(o.getTema())
                .behandlingstema("")
                .oppgavetype(Oppgave.OPPGAVETYPE_FORDELING)
                .prioritet(Oppgave.PRIORITET_NORMAL)
                .aktivDato(new Date())
                .build();
    }

    private String extractEnhetsNr(Journalpost jp){
        return Journalpost.ENHETSNUMMER_GENERISK.equals(jp.getJournalforendeEnhet()) ? "" : jp.getJournalforendeEnhet();
    }

    private String extractTema(Journalpost jp) {
        if (jp.getTema() == null || Oppgave.TEMA_UKJENT.equals(jp.getTema())) {
            return Oppgave.TEMA_GENERELL;
        }
        return jp.getTema();
    }
}
