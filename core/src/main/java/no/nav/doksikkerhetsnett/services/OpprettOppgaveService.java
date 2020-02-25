package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OpprettOppgaveService {

    private final OpprettOppgaveConsumer opprettOppgaveConsumer;

    public OpprettOppgaveService(OpprettOppgaveConsumer opprettOppgaveConsumer) {
        this.opprettOppgaveConsumer = opprettOppgaveConsumer;
    }

    public List<OpprettOppgaveResponse> opprettOppgaver(List<Journalpost> journalposts) {
        List<OpprettOppgaveResponse> responses = new ArrayList<>();
        for (Journalpost jp : journalposts) {
            responses.add(opprettOppgave(jp));
        }
        return responses;
    }

    public OpprettOppgaveResponse opprettOppgave(Journalpost jp) {
        OpprettOppgaveResponse response = opprettOppgaveConsumer.opprettOppgave(jp);
        return response;
    }
}
