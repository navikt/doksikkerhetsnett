package no.nav.doksikkerhetsnett.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oppgave {
    public static final String ENHETSNUMMER_GENERISK = "9999";
    public static final String OPPGAVETYPE_JOURNALFOERT = "JFR";
    public static final String PRIORITET_NORMAL = "NORM";
    public static final String TEMA_UKJENT = "UKJ";
    public static final String TEMA_GENERELL = "GEN";

    private String tildeltEnhetsnr;

    private String opprettetAvEnhetsnr;

    @NotNull(message = "JournalpostId mangler")
    private String journalpostId;

    private String orgnr;

    private String bnr;

    private String beskrivelse;

    private String tema;

    private String behandlingstema;

    private String oppgavetype;

    private String prioritet;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date aktivDato;
}