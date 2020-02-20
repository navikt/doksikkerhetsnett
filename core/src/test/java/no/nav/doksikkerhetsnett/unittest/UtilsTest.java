package no.nav.doksikkerhetsnett.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class UtilsTest {
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
    public void shouldFormatfinnOppgaveString() {
        ArrayList<UbehandletJournalpost> ubehandledeJournalposter = new ArrayList<>();
        long startId = 33333333333L;
        for (int i = 0; i < 5; i++) {
            ubehandledeJournalposter.add(new UbehandletJournalpost().builder().journalpostId(startId++).build());
        }
        ArrayList<String> expectedAns = new ArrayList<>();
        expectedAns.add("journalpostId=33333333333&journalpostId=33333333334&");
        expectedAns.add("journalpostId=33333333335&journalpostId=33333333336&");
        expectedAns.add("journalpostId=33333333337&");

        ArrayList<String> strings = Utils.journalpostListToJournalpostIdListQueryString(ubehandledeJournalposter, 2);
        assertEquals(expectedAns, strings);
    }

    @Test
    public void shouldFormatEmptyListOfTema() {
        List<String> temaer = new ArrayList<>();
        assertEquals("", Utils.formatTemaList(temaer));
    }

    @Test
    public void shouldFormatTemaListWithTemaNull() {
        assertEquals("", Utils.formatTemaList(null));
    }

}