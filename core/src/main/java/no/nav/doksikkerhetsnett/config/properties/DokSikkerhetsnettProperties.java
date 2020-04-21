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

    @NotNull
    private String skrivTemaer;

    //Man trenger kun å sette denne om man ønsker å kun lese noen temaer.
    private String lesTemaer;

}


