package no.nav.doksikkerhetsnett.utils;

import no.nav.doksikkerhetsnett.consumer.finnMottateJournalposter.UbehandletJournalpost;

import java.util.ArrayList;
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

	public static ArrayList<String> formatFinnOppgaveString(List<Long> ubehandledeJournalposter){
		ArrayList<String> retList = new ArrayList<>();
		String arbeidsString = "";

		for (int i = 0; i < ubehandledeJournalposter.size(); i++) {
			arbeidsString += "journalpostId=" + ubehandledeJournalposter.get(i);
			if((i > 0 && i % 500 == 0) || i == ubehandledeJournalposter.size() -1) {
				retList.add(arbeidsString);
				System.out.println("Lengde på journalpostIDstrin= " + arbeidsString.length());
				arbeidsString = "";
			}
			else{
				arbeidsString += "&";
			}
		}
		System.out.println("Lengde på journalpostIDstrin= " + arbeidsString.length());
		return retList;
	}

	public static ArrayList<String> journalpostListToJournalpostIdList(List<UbehandletJournalpost> ubehandledeJournalposter){
		ArrayList<Long> retList = new ArrayList<>();
		for(UbehandletJournalpost ubehandletJP : ubehandledeJournalposter){
			retList.add(ubehandletJP.getJournalpostId());
		}
		return formatFinnOppgaveString(retList);
	}
}
