package no.nav.doksikkerhetsnett.utils;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.Journalpost;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Utils {

    private static String[] alleTema = {"AAP", "AAR", "AGR", "BAR", "BID", "BIL", "DAG", "ENF", "ERS", "FAR", "FEI", "FOR", "FOS", "FUL", "FRI", "GEN", "GRA", "GRU", "HEL", "HJE", "IAR", "IND", "KON", "KTR", "MED", "MOB", "OMS", "OPA", "OPP", "PEN", "PER", "REH", "REK", "RPO", "RVE", "SAA", "SAK", "SAP", "SER", "SIK", "STO", "SUP", "SYK", "SYM", "TIL", "TRK", "TRY", "TSO", "TSR", "UFM", "UFO", "UKJ", "VEN", "YRA", "YRK"};

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
        return Lists.partition(journalpostIds, limit);
    }

    public static String listOfLongsToQueryParams(List<Long> values, String paramName) {
        String result = "";
        for (Long value : values) {
            result += paramName + "=" + value + "&";
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

    // Henter alle temaer, kan velge å ikke få med spesifikke temaer
    public static String[] getAlleTema() {
        return getAlleTema(null);
    }

    public static String[] getAlleTema(String excluded) {
        if (excluded == null || excluded.isEmpty()) {
            return alleTema;
        }
        return Arrays.stream(alleTema).filter(tema -> !excluded.contains(tema)).collect(Collectors.toList()).toArray(new String[0]);
    }
}

