package no.nav.doksikkerhetsnett.itest.config;

import no.nav.doksikkerhetsnett.CoreConfig;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties({
		DokSikkerhetsnettProperties.class,
		AzureProperties.class
})
@Import({CoreConfig.class})
public class TestConfig {


}
