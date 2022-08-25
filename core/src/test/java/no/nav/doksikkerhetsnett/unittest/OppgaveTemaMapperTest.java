package no.nav.doksikkerhetsnett.unittest;

import no.nav.doksikkerhetsnett.entities.Journalpost;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static no.nav.doksikkerhetsnett.mappers.OppgaveTemaMapper.mapJpTemaToOppgaveTema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class OppgaveTemaMapperTest {
	@ParameterizedTest
	@CsvSource(value = {
			"FAR, BID", //MMA-6349
			"UKJ, GEN",
			"PEN, PEN",
			"AAP, AAP",
			",GEN"
	})
	void shouldMapTemasCorrectly(String tema, String expectedResult){
		Journalpost jp = Journalpost.builder().tema(tema).build();
		String resultTema = mapJpTemaToOppgaveTema(jp);
		assertThat(resultTema, is(expectedResult));
	}
}
