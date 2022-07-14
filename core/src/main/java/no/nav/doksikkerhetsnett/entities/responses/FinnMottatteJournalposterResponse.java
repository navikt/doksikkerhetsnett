package no.nav.doksikkerhetsnett.entities.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.doksikkerhetsnett.entities.Journalpost;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinnMottatteJournalposterResponse {

	@NotNull(message = "FinnMottatteJournalposterResponse mangler Journalposter")
	private List<Journalpost> journalposter;
}