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
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.TEMA;

@Component
public class MetricsScheduler {

    private final MeterRegistry meterRegistry;

    MetricsScheduler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementMetrics(List<UbehandletJournalpost> ubehandledeJournalposter, List<UbehandletJournalpost> ubehandledeJournalposterUtenOppgave, Class parentClass) {
        counterBuilder(DOK_METRIC + ".antall.mottatte.journalposter", "Gauge for antall ubehandlede journalposter funnet", ubehandledeJournalposter, parentClass);
        counterBuilder(DOK_METRIC + ".antall.uten.oppgave", "Gauge for antall ubehandlede journalposter funnet som ikke har en åpen oppgave", ubehandledeJournalposterUtenOppgave, parentClass);
    }

    private void counterBuilder(String name, String description, List<UbehandletJournalpost> journalposts, Class parentClass) {
        Map<String, Integer> metrics = extractMetrics(journalposts);

        for (String key : metrics.keySet()) {
            String[] tags = key.split(";");

            Gauge.builder(name, metrics, m -> m.get(key))
                    .description(description)
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
}
