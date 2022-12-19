package no.nav.doksikkerhetsnett.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {
	public static final String ENHETSNUMMER_GENERISK = "9999";

	@NotNull(message = "JournalpostId mangler")
	private long journalpostId;

	@NotNull(message = "journalStatusCode mangler")
	private String journalStatus;
	private String mottaksKanal;
	private Bruker bruker;
	private String tema;
	private String behandlingstema;
	private String journalforendeEnhet;

	@NotNull(message = "datoOpprettet mangler for journalpost")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	private Date datoOpprettet;

	@Override
	public String toString() {
		return "" + journalpostId;
	}
}
