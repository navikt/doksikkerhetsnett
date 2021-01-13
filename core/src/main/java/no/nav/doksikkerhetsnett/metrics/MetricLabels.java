package no.nav.doksikkerhetsnett.metrics;

public class MetricLabels {
    public static final String CLASS = "class";
    public static final String PROCESS_NAME = "process.name";
    public static final String DOK_METRIC = "dok";

    public static final String TEMA = "tema";
    public static final String MOTTAKSKANAL = "mottakskanal";
    public static final String JOURNALFORENDE_ENHET = "journalforende.enhet";

    public static final String TOTAL_NAME = DOK_METRIC + ".antall.mottatte.journalposter";
    public static final String UTEN_OPPGAVE_NAME = DOK_METRIC + ".antall.uten.oppgave";
    public static final String UTEN_OPPGAVE_NAME_EN_DAG = DOK_METRIC + ".antall.uten.oppgave.enDag";
    public static final String UTEN_OPPGAVE_NAME_TO_DAGER = DOK_METRIC + ".antall.uten.oppgave.toDager";


    private MetricLabels() {
        //no-op
    }
}
