package no.nav.doksikkerhetsnett.utils;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

	public static String formatTemaList(List<String> temaer) {
		if(temaer == null){
			return "";
		}
		return temaer.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}
}
