package no.nav.doksikkerhetsnett.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Service
public class FinnOppgaveService {

    FinnOppgaveConsumer finnOppgaveConsumer;

    @Inject
    public FinnOppgaveService(FinnOppgaveConsumer finnOppgaveConsumer) {
        this.finnOppgaveConsumer = finnOppgaveConsumer;
    }

    public FinnOppgaveResponse finnOppgaver(List<UbehandletJournalpost> ubehandledeJournalposter) {

        return finnOppgaveConsumer.finnOppgaveForJournalposter(ubehandledeJournalposter);
    }

}

