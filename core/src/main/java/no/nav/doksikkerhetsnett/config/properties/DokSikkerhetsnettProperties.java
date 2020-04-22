package no.nav.doksikkerhetsnett.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("doksikkerhetsnett")
@Validated
public class DokSikkerhetsnettProperties {

    @NotNull
    private String finnmottattejournalposterurl;

    @NotNull
    private String oppgaveurl;

    @NotNull
    private String securityservicetokenurl;

    @NotNull
    private String opprettjiraissueurl;

    @NotNull
    private ServiceUserProperties serviceuser;

    //kommaseparert liste, "ALLE" for å opprette oppgaver på alle temaer.
    private String skrivTemaer;

    //kommaseparert liste, "ALLE" for å lese alle temaer.
    private String lesTemaer;

}


