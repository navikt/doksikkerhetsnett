package no.nav.doksikkerhetsnett.scheduler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.OppgaveJson;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.doksikkerhetsnett.metrics.MetricLabels.CLASS;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.DOK_METRIC;
import static no.nav.doksikkerhetsnett.metrics.MetricLabels.PROCESS_NAME;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {

    private final FinnMottatteJournalposterService finnMottatteJournalposterService;
    private final FinnOppgaveService finnOppgaveService;
    private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
    private final MeterRegistry meterRegistry;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
                                      DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
                                      FinnOppgaveService finnOppgaveService,
                                      MeterRegistry meterRegistry) {
        this.finnMottatteJournalposterService = finnMottatteJournalposterService;
        this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
        this.finnOppgaveService = finnOppgaveService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(initialDelay = 1000, fixedDelay = 24 * HOUR)
    public void triggerOppdatering() {
        lagOppgaverForGlemteJournalposter();
    }

    public void lagOppgaverForGlemteJournalposter() {
        FinnMottatteJournalposterResponse finnMottatteJournalposterResponse;
        ArrayList<UbehandletJournalpost> ubehandletJournalposts;

        log.info("doksikkerhetsnett henter alle ubehandlede journalposter eldre enn 1 uke {}", Utils.logWithTema(dokSikkerhetsnettProperties
                .getTemaer()));
        try {
            finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(dokSikkerhetsnettProperties
                    .getTemaer());
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under hentingen av alle journalposter {}: " + e.getMessage(), Utils.logWithTema(dokSikkerhetsnettProperties.getTemaer()), e);
            return;
        }

        try {
            ubehandletJournalposts = finnEksisterendeOppgaverFraUbehandledeJournalpostList(finnMottatteJournalposterResponse
                    .getJournalposter());
            log.info("doksikkerhetsnett fant {} journalposter uten oppgave {}",
                    ubehandletJournalposts.size(), Utils.logWithTema(dokSikkerhetsnettProperties.getTemaer()));
            if (ubehandletJournalposts.size() > 0) {
                log.info("journalpostene hadde ID: {}", ubehandletJournalposts);
            }
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under hentingen av alle oppgaver");
            return;
        }

        int antallMottatteJournalposter = finnMottatteJournalposterResponse.getJournalposter().size();
        int antallJournalposterUtenOppgave = ubehandletJournalposts.size();
        int antallJournalposterMedOppgave = antallMottatteJournalposter - antallJournalposterUtenOppgave;

        incrementMetrics(antallMottatteJournalposter, antallJournalposterUtenOppgave, antallJournalposterMedOppgave);
    }

    private ArrayList<UbehandletJournalpost> finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<UbehandletJournalpost> ubehandledeJournalpostList) {
        return fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList);
    }

    private ArrayList<UbehandletJournalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<UbehandletJournalpost> ubehandledeJournalpostList) {
        FinnOppgaveResponse oppgaveResponse = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList);

        if (oppgaveResponse.getOppgaver() == null) {
            return new ArrayList<>();
        }

        List<String> journalposterMedOppgaver = oppgaveResponse.getOppgaver().stream()
                .map(OppgaveJson::getJournalpostId)
                .collect(Collectors.toList());

        return new ArrayList<>(ubehandledeJournalpostList.stream()
                .filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains("" + ubehandletJournalpost.getJournalpostId()))
                .collect(Collectors.toList()));
    }

    private void incrementMetrics(int antallMottatteJournalposter, int antallUtenOppgave, int antallMedOppgave) {
        Counter.builder(DOK_METRIC + "mottatte.journalposter.antall")
                .description("Counter for antall ubehandlede journalposter funnet")
                .tags(CLASS, this.getClass().getCanonicalName())
                .tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
                .register(meterRegistry)
                .increment(antallMottatteJournalposter);

        Counter.builder(DOK_METRIC + "uten.oppgave.antall")
                .description("Counter for antall ubehandlede journalposter som ikke har en åpen oppgave")
                .tags(CLASS, this.getClass().getCanonicalName())
                .tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
                .register(meterRegistry)
                .increment(antallUtenOppgave);

        Counter.builder(DOK_METRIC + "med.oppgave.antall")
                .description("Counter for antall ubehandlede journalposter som allerede har en åpen oppgave")
                .tags(CLASS, this.getClass().getCanonicalName())
                .tags(PROCESS_NAME, "finnJournalposterUtenTilknyttetOppgave")
                .register(meterRegistry)
                .increment(antallMedOppgave);
    }
}

