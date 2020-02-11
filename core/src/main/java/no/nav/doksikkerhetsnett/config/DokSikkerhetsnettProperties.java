package no.nav.doksikkerhetsnett.config;

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

	private String temaer;

	@NotNull
	private ServiceUserProperties serviceuser;

}


