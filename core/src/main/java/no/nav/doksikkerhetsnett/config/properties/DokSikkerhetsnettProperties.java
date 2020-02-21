package no.nav.doksikkerhetsnett.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Random;

@Getter
@Setter
@ToString
@ConfigurationProperties("doksikkerhetsnett")
@Validated
public class DokSikkerhetsnettProperties {

    @NotNull
    private String finnmottattejournalposterurl;

    @NotNull
    private String finnoppgaverurl;

    @NotNull
    private String securityservicetokenurl;

    @NotNull
    private ServiceUserProperties serviceuser;

    // Denne kan være null da et søk på et tomt tema betyr et generelt søk på alle temaer
    private String temaer;

    public String getTemaer() {
        String[] temaListe = {"AAP", "GRU", "HEL", "MED", "SUP", "TIL", "UFM", "BIL", "FUL", "GRA", "KON", "STO", "TSO", "ERS", "UKJ", "GEN", "TRY", "UFO", "RVE", "SAP", "BAR", "HJE", "PER", "TSR", "VEN", "AGR", "ENF", "FOR", "FOS", "IAR", "IND", "KTR", "OMS", "MOB", "REK", "DAG", "OPA", "SYK", "FEI", "OPP", "SER", "TRK", "PEN", "REH", "SAK", "SYM", "YRK"};
        String temaer = "";
        int nTema = new Random().nextInt(3) + 1;
        for (int i = 0; i < nTema; i++) {
            temaer += temaListe[new Random().nextInt(temaListe.length - 1)];
            if (i < nTema - 1)
                temaer += ",";
        }
        return temaer;
        //return "SYK";
    }
}


