package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.consumers.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class FinnOppgaveService {

    private final FinnOppgaveConsumer finnOppgaveConsumer;

    @Inject
    public FinnOppgaveService(FinnOppgaveConsumer finnOppgaveConsumer) {
        this.finnOppgaveConsumer = finnOppgaveConsumer;
    }

    public FinnOppgaveResponse finnOppgaver(List<Journalpost> ubehandledeJournalposter) {
        return finnOppgaveConsumer.finnOppgaveForJournalposter(ubehandledeJournalposter);
    }
}

