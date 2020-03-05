package no.nav.doksikkerhetsnett.utils;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.Journalpost;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Utils {

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
}

