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
	private String finnoppgaverurl;

	@NotNull
	private String securityservicetokenurl;

	@NotNull
	private ServiceUserProperties serviceuser;

	//Denne kan vaere null da et soek på et tomt tema betyr et generelt soek paa alle temaer
	private String temaer;


}


