package no.nav.doksikkerhetsnett.scheduler;

import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.Journalpost;
import no.nav.doksikkerhetsnett.entities.Oppgave;
import no.nav.doksikkerhetsnett.entities.responses.FinnOppgaveResponse;
import no.nav.doksikkerhetsnett.entities.responses.OpprettOppgaveResponse;
import no.nav.doksikkerhetsnett.metrics.MetricsScheduler;
import no.nav.doksikkerhetsnett.services.FinnMottatteJournalposterService;
import no.nav.doksikkerhetsnett.services.FinnOppgaveService;
import no.nav.doksikkerhetsnett.services.OpprettOppgaveService;
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

    @Scheduled(initialDelay = 30000, fixedDelay = 24 * HOUR)
    public void triggerOppdatering() {
        //Dette er bare en midlertidig løsning. Vi ønsker etter hvert å være i skrivemodus for alle temaer.
        runDoksikkerhetsnettInReadMode();

        //Dette er default-kjøringen av doksikkerhetsnett
        runDokSikkerhetsnettInReadWriteMode();
    }

    public void runDoksikkerhetsnettInReadMode(){
        if(dokSikkerhetsnettProperties.getLesTemaer() != null) {
            for(String tema : dokSikkerhetsnettProperties.getLesTemaer().split(","))
                finnJournalposterUtenOppgaver(tema);
        }
    }

    public void runDokSikkerhetsnettInReadWriteMode(){
        lagOppgaverForGlemteJournalposter(dokSikkerhetsnettProperties.getSkrivTemaer());
    }

    public void lagOppgaverForGlemteJournalposter(String temaer) {
        List<Journalpost> ubehandletJournalpostsUtenOppgave = finnJournalposterUtenOppgaver(temaer);
        try {
            List<OpprettOppgaveResponse> opprettedeOppgaver = opprettOppgaveService.opprettOppgaver(ubehandletJournalpostsUtenOppgave);
            if (opprettedeOppgaver.size() > 0) {
                log.info("doksikkerhetsnett har opprettet oppgaver med ID'ene: {}", opprettedeOppgaver.stream().map(opg -> opg.getId()).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under oppretting av oppgaver", e);
        }
        System.out.println("Done!");
    }

    public List<Journalpost> finnJournalposterUtenOppgaver(String temaer) {
        List<Journalpost> ubehandledeJournalposter;
        List<Journalpost> ubehandledeJournalposterUtenOppgave;

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
            log.info("doksikkerhetsnett fant {} journalposter uten oppgave {}{}",
                    ubehandledeJournalposterUtenOppgave.size(),
                    Utils.logWithTema(temaer),
                    ubehandledeJournalposterUtenOppgave.size() > 0 ?
                            ". Journalpostene hadde ID'ene:" + ubehandledeJournalposterUtenOppgave :
                            "");
        } catch (Exception e) {
            log.error("doksikkerhetsnett feilet under hentingen av alle oppgaver", e);
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
                .filter(ubehandletJournalpost -> !journalposterMedOppgaver.contains(Long.toString(ubehandletJournalpost.getJournalpostId())))
                .collect(Collectors.toList()));
    }
}