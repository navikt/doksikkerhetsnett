package no.nav.doksikkerhetsnett.scheduler;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Bruker;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.services.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.services.FinnOppgaveService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
import no.nav.doksikkerhetsnett.utils.Utils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DoksikkerhetsnettScheduled {
    private final FinnMottatteJournalposterService finnMottatteJournalposterService;
    private final FinnOppgaveService finnOppgaveService;
    private final OpprettOppgaveService opprettOppgaveService;
    private final DokSikkerhetsnettProperties dokSikkerhetsnettProperties;
    private final MetricsScheduler metricsScheduler;
    private final int MINUTE = 60_000;
    private final int HOUR = 60 * MINUTE;

    public DoksikkerhetsnettScheduled(FinnMottatteJournalposterService finnMottatteJournalposterService,
                                      DokSikkerhetsnettProperties dokSikkerhetsnettProperties,
                                      FinnOppgaveService finnOppgaveService,
                                      OpprettOppgaveService opprettOppgaveService,
                                      MetricsScheduler metricsScheduler) {
        this.finnMottatteJournalposterService = finnMottatteJournalposterService;
        this.dokSikkerhetsnettProperties = dokSikkerhetsnettProperties;
        this.finnOppgaveService = finnOppgaveService;
        this.opprettOppgaveService = opprettOppgaveService;
        this.metricsScheduler = metricsScheduler;
    }

    @Scheduled(initialDelay = 2500, fixedDelay = 24 * HOUR)
    public void triggerOppdatering() {
        lagOppgaverForGlemteJournalposter();
    }

    public void lagOppgaverForGlemteJournalposter() {
        List<Journalpost> ubehandletJournalpostsUtenOppgave = finnJournalposterUtenOppgaver();
        List<OpprettOppgaveResponse> opprettedeOppgaver = opprettOppgaveService.opprettOppgaver(ubehandletJournalpostsUtenOppgave);

        log.info("doksikkerhetsnett har opprettet oppgaver med ID'ene: {}", opprettedeOppgaver.stream().map(opg -> opg.getId()).collect(Collectors.toList()));

        /*Journalpost dummyJp = Journalpost.builder()
                .behandlingstema("ab0335")
                .bruker(Bruker.builder()
                        .id("22345678")
                        .type("PERSON")
                        .build())
                .datoOpprettet(new Date())
                .journalStatus("M")
                .journalforendeEnhet("0100")
                .journalpostId(22345678)
                .mottaksKanal("NAV_NO")
                .tema("TIL")
                .build();
        opprettOppgaveService.opprettOppgave(exampleJp);*/
    }

    public List<Journalpost> finnJournalposterUtenOppgaver() {
        List<Journalpost> ubehandledeJournalposter;
        List<Journalpost> ubehandledeJournalposterUtenOppgave;
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

    private ArrayList<Journalpost> finnEksisterendeOppgaverFraUbehandledeJournalpostList(List<Journalpost> ubehandledeJournalpostList) {
        return fjernJournalposterMedEksisterendeOppgaverFraListe(ubehandledeJournalpostList);
    }

    private ArrayList<Journalpost> fjernJournalposterMedEksisterendeOppgaverFraListe(List<Journalpost> ubehandledeJournalpostList) {
        FinnOppgaveResponse oppgaveResponse = finnOppgaveService.finnOppgaver(ubehandledeJournalpostList);

        if (oppgaveResponse.getOppgaver() == null) {
            return new ArrayList<>();
        }

        List<String> journalposterMedOppgaver = oppgaveResponse.getOppgaver().stream()
                .map(Oppgave::getJournalpostId)
                .collect(Collectors.toList());

        return new ArrayList<>(ubehandledeJournalpostList.stream()
                .filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains("" + ubehandletJournalpost.getJournalpostId()))
                .collect(Collectors.toList()));
    }

}

