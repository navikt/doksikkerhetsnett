package no.nav.doksikkerhetsnett.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.UbehandletJournalpost;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LagOppgaveService {

    public void lagOppgave(List<UbehandletJournalpost> journalposts) {
        for (UbehandletJournalpost jp : journalposts) {
            lagOppgave(jp);
        }
    }

    public void lagOppgave(UbehandletJournalpost jp) {

    }
}
