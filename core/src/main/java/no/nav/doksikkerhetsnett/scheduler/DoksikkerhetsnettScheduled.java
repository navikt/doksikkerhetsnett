package no.nav.doksikkerhetsnett.scheduler;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumer.finnmottattejournalposter.UbehandletJournalpost;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.consumer.finnoppgave.OppgaveJson;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.service.FinnOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {
    private final FinnMottatteJournalposterService finnMottatteJournalposterService;
    private final FinnOppgaveService finnOppgaveService;
    private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
    private final MetricsScheduler metricsScheduler;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
                                      DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
                                      FinnOppgaveService finnOppgaveService,
									  MetricsScheduler metricsScheduler) {
        this.finnMottatteJournalposterService = finnMottatteJournalposterService;
        this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
        this.finnOppgaveService = finnOppgaveService;
        this.metricsScheduler = metricsScheduler;
    }

    //@Scheduled(initialDelay = 2500, fixedDelay = 24 * HOUR)
    @Scheduled(initialDelay = 2500, fixedDelay = 2 * MINUTE)
    public void triggerOppdatering() {
        lagOppgaverForGlemteJournalposter();
    }

    public void lagOppgaverForGlemteJournalposter() {
        List<UbehandletJournalpost> ubehandletJournalpostsUtenOppgave = finnjournalposterUtenOppgaver();
        //TODO: Del 2; sikkerhetsnett i skriv-modus.
    }

    public List<UbehandletJournalpost> finnjournalposterUtenOppgaver() {
        List<UbehandletJournalpost> ubehandledeJournalposter;
        List<UbehandletJournalpost> ubehandledeJournalposterUtenOppgave;
        String temaer = dokSikkerhetsnettProperties.getTemaer();

        log.info("doksikkerhetsnett henter alle ubehandlede journalposter eldre enn 1 uke {}", Utils.logWithTema(temaer));

        try {
            ubehandledeJournalposter = finnMottatteJournalposterService.finnMottatteJournalPoster(temaer)
                    .getJournalposter();
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under hentingen av alle journalposter {}: " + e.getMessage(), Utils.logWithTema(temaer), e);
            return null;
        }

        try {
            ubehandledeJournalposterUtenOppgave = finnEksisterendeOppgaverFraUbehandledeJournalpostList(ubehandledeJournalposter);
            log.info("doksikkerhetsnett fant {} journalposter uten oppgave {}",
                    ubehandledeJournalposterUtenOppgave.size(), Utils.logWithTema(temaer));
            if (ubehandledeJournalposterUtenOppgave.size() > 0) {
                log.info("journalpostene hadde ID'ene: {}", ubehandledeJournalposterUtenOppgave);
            }
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under hentingen av alle oppgaver");
            return null;
        }

        metricsScheduler.incrementMetrics(ubehandledeJournalposter, ubehandledeJournalposterUtenOppgave, this.getClass());
        return ubehandledeJournalposterUtenOppgave;
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

}

