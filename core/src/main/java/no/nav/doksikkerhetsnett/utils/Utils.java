package no.nav.doksikkerhetsnett.utils;


import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.Journalpost;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.partition;

@Slf4j
public class Utils {

	private Utils() {
	}

	// Alle temaer fra https://confluence.adeo.no/display/BOA/Enum%3A+Tema. Oppdatert 15.03.2022
	private static final Set<String> alleTema = new HashSet(Arrays.asList("AAP", "AAR", "AGR", "ARP", "ARS", "BAR", "BID", "BIL", "DAG", "ENF", "ERS", "EYB", "EYO", "FAR", "FEI", "FIP", "FOR", "FOS", "FUL", "FRI", "GEN", "GRA", "GRU", "HEL", "HJE", "IAR", "IND", "KLL", "KON", "KTA", "KTR", "MED", "MOB", "OMS", "OPA", "OPP", "PEN", "PER", "REH", "REK", "RPO", "RVE", "SAA", "SAK", "SAP", "SER", "SIK", "STO", "SUP", "SYK", "SYM", "TIL", "TRK", "TRY", "TSO", "TSR", "UFM", "UFO", "UKJ", "VEN", "YRA", "YRK"));

	public static String formatTemaList(List<String> temaer) {
		if (temaer == null) {
			return "";
		}
		return temaer.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}

	public static List<List<Long>> journalpostListToPartitionedJournalpostIdList(List<Journalpost> ubehandledeJournalposter, int limit) {
		if (ubehandledeJournalposter == null) {
			return new ArrayList<>();
		}
		List<Long> journalpostIds = ubehandledeJournalposter.stream().map(Journalpost::getJournalpostId).collect(Collectors.toList());

		return partition(journalpostIds, limit);
	}

	public static String listOfLongsToQueryParams(List<Long> values, String paramName) {
		StringBuilder result = new StringBuilder();
		for (Long value : values) {
			result.append(paramName).append("=").append(value).append("&");
		}
		return result.substring(0, result.length() - 1);
	}

	public static URI appendQuery(URI oldUri, String name, String value) {
		String appendQuery = name + "=" + value;
		String newQuery = oldUri.getQuery();
		if (newQuery == null) {
			newQuery = appendQuery;
		} else {
			newQuery += "&" + appendQuery;
		}
		try {
			return new URI(oldUri.getScheme(), oldUri.getAuthority(),
					oldUri.getPath(), newQuery, oldUri.getFragment());
		} catch (URISyntaxException e) {
			log.error("Append query feilet å legge til {} til uri'en {}", appendQuery, oldUri, e);
			return null;
		}

	}

	public static String logWithDager(int dagerGamle) {
		return dagerGamle == 1 ? "1 dag" : dagerGamle + " dager";
	}

	// Hjelpefunksjon for finere logging ut ifra hvor mange temaer som er i kallet
	public static String logWithTema(String temaer) {
		if (temaer == null || temaer.isEmpty()) {
			return "for alle temaer";
		}
		if (temaer.contains(",")) {
			return "med tema blandt " + temaer;
		}
		return "med tema " + temaer;
	}

	public static Set<String> getAlleTema() {
		return alleTema;
	}

	public static Set<String> getAlleTemaExcept(String excluded) {
		return getAlleTemaExcept(new HashSet<>(Arrays.asList(excluded.split(","))));
	}

	public static Set<String> getAlleTemaExcept(Set<String> excluded) {
		if (excluded == null || excluded.isEmpty()) {
			return getAlleTema();
		}
		Set<String> alleTemaCopy = new HashSet(alleTema);
		alleTemaCopy.removeAll(excluded);
		return alleTemaCopy;
	}

}

