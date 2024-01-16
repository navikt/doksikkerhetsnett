package no.nav.doksikkerhetsnett.utils;


import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class Tema {

	// Alle temaer fra https://confluence.adeo.no/display/BOA/Enum%3A+Tema. Oppdatert 24.10.2023
	@Getter
	private static final Set<String> alleTema = Set.of("AAP", "AAR", "AGR", "ARP", "ARS", "BAR", "BID", "BIL", "DAG", "ENF", "ERS", "EYB", "EYO", "FAR", "FEI", "FIP", "FOR", "FOS", "FUL", "FRI", "GEN", "GRA", "GRU", "HEL", "HJE", "IAR", "IND", "KLL", "KON", "KTA", "KTR", "MED", "MOB", "OMS", "OPA", "OPP", "PEN", "PER", "REH", "REK", "RPO", "RVE", "SAA", "SAK", "SAP", "SER", "STO", "SUP", "SYK", "SYM", "TIL", "TRK", "TRY", "TSO", "TSR", "UFM", "UFO", "UKJ", "VEN", "YRA", "YRK");

	public static String formatTemaList(List<String> temaer) {
		if (temaer == null) {
			return "";
		}
		return temaer.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}

	public static Set<String> getAlleTemaExcept(String excluded) {
		return getAlleTemaExcept(new HashSet<>(asList(excluded.split(","))));
	}

	public static Set<String> getAlleTemaExcept(Set<String> excluded) {
		if (excluded == null || excluded.isEmpty()) {
			return getAlleTema();
		}

		Set<String> alleTemaCopy = new HashSet<>(alleTema);
		alleTemaCopy.removeAll(excluded);

		return alleTemaCopy;
	}

	public static Set<String> temaerStringToSet(String temaer) {
		return Arrays.stream(temaer.split(","))
				.map(String::trim)
				.collect(Collectors.toSet());
	}
}

