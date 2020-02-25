package no.nav.doksikkerhetsnett.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oppgave {

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

/*
{
    "tildeltEnhetsnr": "",
    "opprettetAvEnhetsnr": "",
    "journalpostId": "",
    "orgnr": "",
    "bnr": "",
    "beskrivelse": "",
    "tema": "",
    "behandlingstema": "",
    "oppgavetype": ""
}
*/