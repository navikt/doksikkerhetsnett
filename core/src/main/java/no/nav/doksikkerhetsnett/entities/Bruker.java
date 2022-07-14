package no.nav.doksikkerhetsnett.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Bruker {

	public static final String TYPE_ORGANISASJON = "ORGANISASJON";
	public static final String TYPE_PERSON = "PERSON";

	private String id;
	private String type;
}
