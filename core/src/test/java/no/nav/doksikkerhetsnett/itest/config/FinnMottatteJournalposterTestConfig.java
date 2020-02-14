package no.nav.doksikkerhetsnett.itest.config;

import no.nav.doksikkerhetsnett.CoreConfig;
import no.nav.doksikkerhetsnett.config.DokSikkerhetsnettProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(DokSikkerhetsnettProperties.class)
@Import(CoreConfig.class)
public class FinnMottatteJournalposterTestConfig {

}
