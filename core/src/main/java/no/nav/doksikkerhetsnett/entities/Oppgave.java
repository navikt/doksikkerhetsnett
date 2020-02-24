package no.nav.doksikkerhetsnett.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oppgave {

    private String tildeltEnhetsnr;
    private String opprettetAvEnhetsnr;
    private String journalpostId;
    private String orgnr;
    private String bnr;
    private String beskrivelse;
    private String tema;
    private String behandlingstema;
    private String oppgavetype;

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