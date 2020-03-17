package no.nav.doksikkerhetsnett.consumers;

import static no.nav.doksikkerhetsnett.config.cache.LokalCacheConfig.STS_CACHE;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MULTIPLIER_SHORT;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.entities.responses.StsResponseTo;
import no.nav.doksikkerhetsnett.exceptions.technical.AbstractDoksikkerhetsnettTechnicalException;
import no.nav.doksikkerhetsnett.exceptions.technical.StsTechnicalException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class StsRestConsumer {

    private final RestTemplate restTemplate;
    private final String stsUrl;

    @Inject
    public StsRestConsumer(RestTemplateBuilder restTemplateBuilder,
                           DokSikkerhetsnettProperties dokSikkerhetsnettProperties) {
        this.stsUrl = dokSikkerhetsnettProperties.getSecurityservicetokenurl();
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(20))
                .setConnectTimeout(Duration.ofSeconds(5))
                .basicAuthentication(dokSikkerhetsnettProperties.getServiceuser().getUsername(), dokSikkerhetsnettProperties.getServiceuser().getPassword())
                .build();
    }

    @Retryable(include = AbstractDoksikkerhetsnettTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
    @Cacheable(STS_CACHE)
    public String getOidcToken() {
        try {
            return restTemplate.getForObject(stsUrl + "?grant_type=client_credentials&scope=openid", StsResponseTo.class)
                    .getAccessToken();
        } catch (HttpStatusCodeException e) {
            throw new StsTechnicalException(String.format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e
                    .getMessage()), e);
        }
    }
}
