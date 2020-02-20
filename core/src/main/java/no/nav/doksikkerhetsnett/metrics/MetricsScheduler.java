package no.nav.doksikkerhetsnett.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    public void incrementMetrics(List<UbehandletJournalpost> ubehandledeJournalposter, List<UbehandletJournalpost> ubehandledeJournalposterUtenOppgave) {
        counterBuilder(DOK_METRIC + ".antall.mottatte.journalposter", ubehandledeJournalposter);
        counterBuilder(DOK_METRIC + ".antall.uten.oppgave", ubehandledeJournalposterUtenOppgave);
    }

    private void counterBuilder(String name, List<UbehandletJournalpost> journalposts) {
        for (UbehandletJournalpost jp: journalposts) {
            String tema = jp.getTema() != null ? jp.getTema() : "mangler tema";
            String mottakskanal = jp.getMottaksKanal() != null ? jp.getMottaksKanal() : "mangler mottakskanal";
            String journalforendeEnhet = jp.getJournalforendeEnhet() != null ? jp.getJournalforendeEnhet() : "mangler journalførende enhet";

            Counter.builder(name)
                    .description("Counter for antall ubehandlede journalposter funnet")
                    .tags(CLASS, this.getClass().getCanonicalName())
                    .tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
                    .tags(TEMA, tema)
                    .tags(MOTTAKSKANAL, mottakskanal)
                    .tags(JOURNALFORENDE_ENHET, journalforendeEnhet)
                    .register(meterRegistry)
                    .increment();
        }
    }
}
