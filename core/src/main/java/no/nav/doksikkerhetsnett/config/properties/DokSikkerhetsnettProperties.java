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
}


