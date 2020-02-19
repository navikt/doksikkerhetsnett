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

	@Test
	public void surr(){
		ArrayList<String> surreliste = new ArrayList<>();
		ArrayList<Long> longliste = new ArrayList<>();
		for(long i = 10; i < 21; i++){
			longliste.add(i);
			if(i % 2 == 0)
				surreliste.add(""+i);
		}

		System.out.println("Før: " + longliste.size());
		longliste.removeIf(longe -> surreliste.contains(""+longe));
		System.out.println("Etter: " + longliste.size());

		
	}

	//@Test
	/*public void shouldFormatJournalpostListToGetRequestFormat() {
		ArrayList<Long> ubehandledeJournalposter = new ArrayList<>();
		long startId = 33333333333L;
		for(int i = 0; i < 150; i++){
		   ubehandledeJournalposter.add(startId++);
		}

		ArrayList<String> retStrings = Utils.formatFinnOppgaveString(ubehandledeJournalposter);
		for (String s : retStrings) {
			System.out.println("length of string: " + s.length() + " " +s);
		}
	}  */
}