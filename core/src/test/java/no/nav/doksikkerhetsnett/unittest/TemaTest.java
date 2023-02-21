package no.nav.doksikkerhetsnett.unittest;

import no.nav.doksikkerhetsnett.utils.Tema;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemaTest {
	private static final String TEMA_LIST_AS_STRING = "UFO,BAR,PEN";

	@Test
	void shouldFormatListOfTema() {
		List<String> temaer = new ArrayList<>();
		temaer.add("UFO");
		temaer.add("BAR");
		temaer.add("PEN");

		assertEquals(TEMA_LIST_AS_STRING, Tema.formatTemaList(temaer));
	}

	@Test
	void shouldFormatEmptyListOfTema() {
		List<String> temaer = new ArrayList<>();
		assertEquals("", Tema.formatTemaList(temaer));
	}

	@Test
	void shouldFormatTemaListWithTemaNull() {
		assertEquals("", Tema.formatTemaList(null));
	}

	@Test
	void shouldGetAllTemas() {
		assertEquals(62, Tema.getAlleTema().size());
		assertEquals(59, Tema.getAlleTemaExcept(new HashSet<>(Arrays.asList("ERS", "RPO", "SAK"))).size());
		assertEquals(59, Tema.getAlleTemaExcept("ERS,RPO,SAK").size());
	}
}