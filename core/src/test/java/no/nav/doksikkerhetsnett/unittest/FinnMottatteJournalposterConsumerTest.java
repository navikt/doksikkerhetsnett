package no.nav.doksikkerhetsnett.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.doksikkerhetsnett.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class FinnMottatteJournalposterConsumerTest {
	private static final String TEMA_LIST_AS_STRING = "UFO,BAR,PEN";

	@Test
	public void shouldFormatListOfTema() {
		List<String> temaer = new ArrayList<>();
		temaer.add("UFO");
		temaer.add("BAR");
		temaer.add("PEN");

		assertEquals(TEMA_LIST_AS_STRING, Utils.formatTemaList(temaer));
	}

	@Test
	public void shouldFormatEmptyListOfTema() {
		List<String> temaer = new ArrayList<>();

		assertEquals("", Utils.formatTemaList(temaer));
	}

	@Test
	public void shouldFormatNull() {
		List<String> temaer = null;

		assertEquals("", Utils.formatTemaList(temaer));
	}
}