package no.nav.doksikkerhetsnett;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.config.properties.JiraAuthProperties;
import no.nav.doksikkerhetsnett.consumers.azure.AzureProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.resilience.annotation.EnableResilientMethods;

@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAspectJAutoProxy
@EnableResilientMethods
@EnableConfigurationProperties({DokSikkerhetsnettProperties.class, AzureProperties.class, JiraAuthProperties.class})
@Configuration
public class ApplicationConfig {
}
