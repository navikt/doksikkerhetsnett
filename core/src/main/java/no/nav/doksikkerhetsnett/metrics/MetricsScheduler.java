package no.nav.doksikkerhetsnett.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.doksikkerhetsnett.metrics.MetricLabels.CLASS;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.JOURNALFORENDE_ENHET;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.MOTTAKSKANAL;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TEMA;

@Component
public class MetricsScheduler {

    private final MeterRegistry meterRegistry;

    private final Map<String, Integer> totalGaugeCache = new HashMap<>();
    private final Map<String, Integer> utenOppgaveGaugeCache = new HashMap<>();
    private final String TOTAL_NAME = DOK_METRIC + ".antall.mottatte.journalposter";
    private final String UTEN_OPPGAVE_NAME = DOK_METRIC + ".antall.uten.oppgave";

    MetricsScheduler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementMetrics(List<UbehandletJournalpost> ubehandledeJournalposter, List<UbehandletJournalpost> ubehandledeJournalposterUtenOppgave, Class parentClass) {

        Map<String, Integer> newMetricsTotal = extractMetrics(ubehandledeJournalposter);
        Map<String, Integer> newMetricsUtenOppgave = extractMetrics(ubehandledeJournalposterUtenOppgave);
        updateCaches(newMetricsTotal, newMetricsUtenOppgave);

        for (String key : totalGaugeCache.keySet()) {
            String[] tags = key.split(";");

            Gauge.builder(TOTAL_NAME, totalGaugeCache, gc -> gc.get(key))
                    .description("Gauge for antall ubehandlede journalposter funnet")
                    .tags(CLASS, parentClass.getCanonicalName())
                    .tag(TEMA, tags[0])
                    .tags(MOTTAKSKANAL, tags[1])
                    .tags(JOURNALFORENDE_ENHET, tags[2])
                    .register(meterRegistry);
        }
        for (String key : utenOppgaveGaugeCache.keySet()) {
            String[] tags = key.split(";");

            Gauge.builder(UTEN_OPPGAVE_NAME, utenOppgaveGaugeCache, gc -> gc.get(key))
                    .description("Gauge for antall ubehandlede journalposter funnet som ikke har en åpen oppgave")
                    .tags(CLASS, parentClass.getCanonicalName())
                    .tag(TEMA, tags[0])
                    .tags(MOTTAKSKANAL, tags[1])
                    .tags(JOURNALFORENDE_ENHET, tags[2])
                    .register(meterRegistry);
        }
    }

    private Map<String, Integer> extractMetrics(List<UbehandletJournalpost> journalposts) {
        Map<String, Integer> metrics = new HashMap<>();
        for (UbehandletJournalpost jp : journalposts) {
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
        // Resetter cachen, men vil beholde alle nøklene
        for (String key : totalGaugeCache.keySet()) {
            totalGaugeCache.put(key, 0);
        }
        for (String key : utenOppgaveGaugeCache.keySet()) {
            utenOppgaveGaugeCache.put(key, 0);
        }
        // Setter de nye verdiene
        for (String newKey : newMetricsTotal.keySet()) {
            totalGaugeCache.put(newKey, newMetricsTotal.get(newKey));
        }
        for (String newKey : newMetricsUtenOppgave.keySet()) {
            utenOppgaveGaugeCache.put(newKey, newMetricsTotal.get(newKey));
        }
    }
}