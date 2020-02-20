package no.nav.doksikkerhetsnett.utils;

import com.google.common.collect.Lists;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;

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


    private static ArrayList<String> formatFinnOppgaveStringAsIdQueryString(List<Long> ubehandledeJournalposter, int limit) {
        ArrayList<String> journalpostListAsQueryString = new ArrayList<>();

        for (List<Long> partition : Lists.partition(ubehandledeJournalposter, limit)) {
            String arbeidsString = "";

            for (Long journalpostId : partition) {
                arbeidsString += "journalpostId=" + journalpostId + "&";
            }
            journalpostListAsQueryString.add(arbeidsString);
        }

        return journalpostListAsQueryString;
    }

    public static ArrayList<String> journalpostListToJournalpostIdListQueryString(List<UbehandletJournalpost> ubehandledeJournalposter, int limit) {
        if (ubehandledeJournalposter == null) {
            return new ArrayList<>();
        }
        List<Long> retList = ubehandledeJournalposter.stream()
                .map(UbehandletJournalpost::getJournalpostId)
                .collect(Collectors.toList());

        return formatFinnOppgaveStringAsIdQueryString(retList, limit);
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
}

