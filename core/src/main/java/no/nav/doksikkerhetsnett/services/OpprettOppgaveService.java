package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumers.OpprettOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OpprettOppgaveService {

    private final OpprettOppgaveConsumer opprettOppgaveConsumer;

    public OpprettOppgaveService(OpprettOppgaveConsumer opprettOppgaveConsumer) {
        this.opprettOppgaveConsumer = opprettOppgaveConsumer;
    }

    public void opprettOppgave(List<Journalpost> journalposts) {
        for (Journalpost jp : journalposts) {
            opprettOppgave(jp);
        }
    }

    public void opprettOppgave(Journalpost jp) {
        opprettOppgaveConsumer.opprettOppgave(jp);
    }
}
