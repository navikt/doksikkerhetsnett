package no.nav.doksikkerhetsnett.itest.config;

import no.nav.doksikkerhetsnett.CoreConfig;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureTokenConsumer;
import no.nav.doksikkerhetsnett.consumers.azure.TokenResponse;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.anyString;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(DokSikkerhetsnettProperties.class)
@Import({CoreConfig.class})
public class TestConfig {

	@Bean
	@Primary
	AzureTokenConsumer azureTokenConsumer() {
		AzureTokenConsumer azureTokenConsumer = Mockito.mock(AzureTokenConsumer.class);
		Mockito.when(azureTokenConsumer.getClientCredentialToken(anyString())).thenReturn(
				TokenResponse.builder()
						.access_token("dummy")
						.build()
		);

		return azureTokenConsumer;
	}

}
