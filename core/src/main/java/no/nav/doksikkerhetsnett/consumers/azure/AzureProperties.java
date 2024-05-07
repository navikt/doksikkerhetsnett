package no.nav.doksikkerhetsnett.consumers.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureProperties(
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret,
		@NotEmpty String openidConfigTokenEndpoint
) {
	public static final String CLIENT_REGISTRATION_DOKARKIV = "azure-dokarkiv";
	public static final String CLIENT_REGISTRATION_PDL = "azure-pdl";
	public static final String CLIENT_REGISTRATION_OPPGAVE = "azure-oppgave";
}
