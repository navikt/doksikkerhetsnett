package no.nav.doksikkerhetsnett.unittest;

import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {
	private static final String TEMA_LIST_AS_STRING = "UFO,BAR,PEN";
	private static final String QUERY_PARAMS = "name=1&name=2&name=3&name=4&name=5";

	@Test
	void shouldFormatListOfTema() {
		List<String> temaer = new ArrayList<>();
		temaer.add("UFO");
		temaer.add("BAR");
		temaer.add("PEN");

		assertEquals(TEMA_LIST_AS_STRING, Utils.formatTemaList(temaer));
	}

	@Test
	void shouldFormatAndPartitionJournalpostIds() {
		List<Journalpost> ubehandledeJournalposter = new ArrayList<>();
		long startId = 33333333333L;
		for (int i = 0; i < 5; i++) {
			ubehandledeJournalposter.add(new Journalpost().builder().journalpostId(startId++).build());
		}
		List<List<Long>> expectedAns = asList(
				asList(33333333333L, 33333333334L),
				asList(33333333335L, 33333333336L),
				Collections.singletonList(33333333337L));

		List<List<Long>> strings = Utils.journalpostListToPartitionedJournalpostIdList(ubehandledeJournalposter, 2);
		assertEquals(expectedAns, strings);
	}

	@Test
	void shouldFormatListOfLongsToQueryParams() {
		List<Long> values = asList(1L, 2L, 3L, 4L, 5L);
		String queryParams = Utils.listOfLongsToQueryParams(values, "name");
		assertEquals(QUERY_PARAMS, queryParams);
	}

	@Test
	void shouldFormatEmptyListOfTema() {
		List<String> temaer = new ArrayList<>();
		assertEquals("", Utils.formatTemaList(temaer));
	}

	@Test
	void shouldAppendQueryToUri() throws URISyntaxException {
		URI uriNoParams = new URI("http://url.com");
		URI uriAppendedParamsOnce = Utils.appendQuery(uriNoParams, "appendedName", "appendedValue");
		URI uriAppendedParamsTwice = Utils.appendQuery(uriAppendedParamsOnce, "appendedName", "appendedValue");
		assertEquals(new URI("http://url.com?appendedName=appendedValue"), uriAppendedParamsOnce);
		assertEquals(new URI("http://url.com?appendedName=appendedValue&appendedName=appendedValue"), uriAppendedParamsTwice);
	}

	@Test
	void shouldFormatTemaListWithTemaNull() {
		assertEquals("", Utils.formatTemaList(null));
	}

	@Test
	void shouldGetAllTemas() {
		assertEquals(55, Utils.getAlleTema().size());
		assertEquals(52, Utils.getAlleTemaExcept(new HashSet<>(Arrays.asList("ERS", "RPO", "SAK"))).size());
		assertEquals(52, Utils.getAlleTemaExcept("ERS,RPO,SAK").size());
	}
}