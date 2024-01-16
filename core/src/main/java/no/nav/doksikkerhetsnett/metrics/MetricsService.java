package no.nav.doksikkerhetsnett.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.doksikkerhetsnett.metrics.MetricLabels.ANTALL_DAGER;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.CLASS;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.JOURNALFORENDE_ENHET;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.MOTTAKSKANAL;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TEMA;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TOTAL_NAME;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.UTEN_OPPGAVE_NAME;

@Slf4j
@Component
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final List<Gauge> gauges = new ArrayList<>();
    private final Map<String, Integer> totalGaugeCache = new HashMap<>();
    private final Map<String, Integer> utenOppgaveGaugeCache = new HashMap<>();

    MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void updateGauges(List<Journalpost> ubehandledeJournalposter, List<Journalpost> ubehandledeJournalposterUtenOppgave, int antallDager) {
        Map<String, Integer> newMetricsTotal = extractMetrics(ubehandledeJournalposter);
        Map<String, Integer> newMetricsUtenOppgave = extractMetrics(ubehandledeJournalposterUtenOppgave);
        updateCaches(newMetricsTotal, newMetricsUtenOppgave);

        for (String key : totalGaugeCache.keySet()) {
            String[] tags = key.split(";"); // [tema,mottakskanal,journalforendeEnhet]
            gauges.add(Gauge.builder(TOTAL_NAME, totalGaugeCache, gc -> gc.get(key))
                    .description("Gauge for antall ubehandlede journalposter funnet")
                    .tags(CLASS, new Exception().getStackTrace()[1].getClassName())
                    .tag(ANTALL_DAGER, ""+antallDager)
                    .tag(TEMA, tags[0])
                    .tags(MOTTAKSKANAL, tags[1])
                    .tags(JOURNALFORENDE_ENHET, tags[2])
                    .register(meterRegistry));
        }
        for (String key : utenOppgaveGaugeCache.keySet()) {
            String[] tags = key.split(";"); // [tema,mottakskanal,journalforendeEnhet]
            gauges.add(Gauge.builder(UTEN_OPPGAVE_NAME, utenOppgaveGaugeCache, gc -> gc.get(key))
                    .description("Gauge for antall ubehandlede journalposter funnet som ikke har en åpen oppgave")
                    .tags(CLASS, new Exception().getStackTrace()[1].getClassName())
                    .tags(ANTALL_DAGER, ""+antallDager)
                    .tag(TEMA, tags[0])
                    .tags(MOTTAKSKANAL, tags[1])
                    .tags(JOURNALFORENDE_ENHET, tags[2])
                    .register(meterRegistry));
        }
    }

    private Map<String, Integer> extractMetrics(List<Journalpost> journalposts) {
        Map<String, Integer> metrics = new HashMap<>();
        for (Journalpost jp : journalposts) {
            String tema = jp.getTema() != null ? jp.getTema() : "mangler tema";
            String mottakskanal = jp.getMottaksKanal() != null ? jp.getMottaksKanal() : "mangler mottakskanal";
            String journalforendeEnhet = jp.getJournalforendeEnhet() != null ? jp.getJournalforendeEnhet() : "mangler journalførende enhet";

            String key = tema + ";" + mottakskanal + ";" + journalforendeEnhet;
            int count = metrics.getOrDefault(key, 0);
            metrics.put(key, count + 1);
        }

        return metrics;
    }

    // vi ønsker ikke å lage metrikker på gamle data så vi må fjerne de eksisterende cachene og meterne
    public void clearOldMetrics(){
        for (Gauge gauge : gauges) {
            meterRegistry.remove(gauge);
        }
        gauges.clear();
        totalGaugeCache.clear();
        utenOppgaveGaugeCache.clear();
    }

    private void updateCaches(Map<String, Integer> newMetricsTotal, Map<String, Integer> newMetricsUtenOppgave) {
        // Populerer cachene med de nye verdiene
        for (String newKey : newMetricsTotal.keySet()) {
            totalGaugeCache.put(newKey, newMetricsTotal.get(newKey));
        }
        for (String newKey : newMetricsUtenOppgave.keySet()) {
            utenOppgaveGaugeCache.put(newKey, newMetricsUtenOppgave.get(newKey));
        }
    }

    public List<Map<String, Integer>> getCaches() {
        return Arrays.asList(totalGaugeCache, utenOppgaveGaugeCache);
    }
}