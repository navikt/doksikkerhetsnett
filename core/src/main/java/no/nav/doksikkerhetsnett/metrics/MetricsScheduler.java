package no.nav.doksikkerhetsnett.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.doksikkerhetsnett.metrics.MetricLabels.CLASS;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.JOURNALFORENDE_ENHET;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.MOTTAKSKANAL;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TEMA;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TOTAL_NAME;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.UTEN_OPPGAVE_NAME;

@Component
public class MetricsScheduler {

    private final MeterRegistry meterRegistry;

    private final List<Gauge> gauges = new ArrayList<>();
    private final Map<String, Integer> totalGaugeCache = new HashMap<>();
    private final Map<String, Integer> utenOppgaveGaugeCache = new HashMap<>();

    MetricsScheduler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementMetrics(List<Journalpost> ubehandledeJournalposter, List<Journalpost> ubehandledeJournalposterUtenOppgave, Class parentClass) {
        Map<String, Integer> newMetricsTotal = extractMetrics(ubehandledeJournalposter);
        Map<String, Integer> newMetricsUtenOppgave = extractMetrics(ubehandledeJournalposterUtenOppgave);
        updateCaches(newMetricsTotal, newMetricsUtenOppgave);

        for (String key : totalGaugeCache.keySet()) {
            String[] tags = key.split(";"); // [tema,mottakskanal,journalforendeEnhet]
            gauges.add(Gauge.builder(TOTAL_NAME, totalGaugeCache, gc -> gc.get(key))
                    .description("Gauge for antall ubehandlede journalposter funnet")
                    .tags(CLASS, new Exception().getStackTrace()[1].getClassName())
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
            int count = metrics.containsKey(key) ? metrics.get(key) : 0;
            metrics.put(key, count + 1);
        }
        return metrics;
    }

    private void updateCaches(Map<String, Integer> newMetricsTotal, Map<String, Integer> newMetricsUtenOppgave) {
        // Vil ikke lage metrikker på gammel data, så må fjerne de eksisterende cachene og meterne
        for (Gauge gauge : gauges) {
            meterRegistry.remove(gauge);
        }
        gauges.clear();
        totalGaugeCache.clear();
        utenOppgaveGaugeCache.clear();

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