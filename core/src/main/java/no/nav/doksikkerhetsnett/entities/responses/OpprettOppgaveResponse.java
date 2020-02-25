package no.nav.doksikkerhetsnett.entities.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.doksikkerhetsnett.entities.Oppgave;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OpprettOppgaveResponse extends Oppgave {

    @NotNull
    private String id;
}
