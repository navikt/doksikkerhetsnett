package no.nav.doksikkerhetsnett.utils;

import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveConsumer;

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

	private static ArrayList<String> formatFinnOppgaveString(List<Long> ubehandledeJournalposter, int limit ) {
		ArrayList<String> retList = new ArrayList<>();
		String arbeidsString = "";
		int ii = 0;
		for (int i = 0; i < ubehandledeJournalposter.size(); i++) {
			arbeidsString += "journalpostId=" + ubehandledeJournalposter.get(i);
			if (ii == limit-1 || i == ubehandledeJournalposter.size() - 1) {
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

	public static ArrayList<String> journalpostListToJournalpostIdList(List<UbehandletJournalpost> ubehandledeJournalposter, int limit) {
		List<Long> retList = ubehandledeJournalposter.stream()
				.map(UbehandletJournalpost::getJournalpostId)
				.collect(Collectors.toList());

		return formatFinnOppgaveString(retList, limit);
	}
}

