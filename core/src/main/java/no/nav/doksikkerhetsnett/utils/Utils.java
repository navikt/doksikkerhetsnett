package no.nav.doksikkerhetsnett.utils;

import no.nav.doksikkerhetsnett.consumer.finnMottatteJournalposter.UbehandletJournalpost;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

	public static String formatTemaList(List<String> temaer) {
		if (temaer == null) {
			return "";
		}
		return temaer.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}

	public static ArrayList<String> formatFinnOppgaveString(List<Long> ubehandledeJournalposter) {
		ArrayList<String> retList = new ArrayList<>();
		String arbeidsString = "";

		int ii = 0;
		for (int i = 0; i < ubehandledeJournalposter.size(); i++) {
			arbeidsString += "journalpostId=" + ubehandledeJournalposter.get(i);
			if (ii == 19 || i == ubehandledeJournalposter.size() - 1) {
				ii = 0;
				retList.add(arbeidsString);
				arbeidsString = "";
			} else {
				arbeidsString += "&";
				ii++;
			}
		}
		return retList;
	}

	public static ArrayList<String> journalpostListToJournalpostIdList(List<UbehandletJournalpost> ubehandledeJournalposter) {
		ArrayList<Long> retList = new ArrayList<>();
		for (UbehandletJournalpost ubehandletJP : ubehandledeJournalposter) {
			retList.add(ubehandletJP.getJournalpostId());
		}
		return formatFinnOppgaveString(retList);
	}
}

