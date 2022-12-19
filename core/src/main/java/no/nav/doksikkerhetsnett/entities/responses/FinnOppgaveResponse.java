package no.nav.doksikkerhetsnett.entities.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.doksikkerhetsnett.entities.Oppgave;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinnOppgaveResponse {

	@NotNull(message = "Feilet ved henting av oppgaver")
	private int antallTreffTotalt;
	private List<Oppgave> oppgaver;
}