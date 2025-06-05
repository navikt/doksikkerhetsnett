package no.nav.doksikkerhetsnett.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@ConfigurationProperties("doksikkerhetsnett")
@Validated
public class DokSikkerhetsnettProperties {

    private final Endpoints endpoints = new Endpoints();
    private final SlackProperties slack = new SlackProperties();

    @NotNull
    private ServiceUserProperties serviceuser;

    //kommaseparert liste, "ALLE" for å opprette oppgaver på alle temaer.
    private String skrivTemaer;

    //kommaseparert liste, "ALLE" for å lese alle temaer.
    private String lesTemaer;

    @Data
    @Validated
    public static class Endpoints {

        @NotNull
        private String opprettjiraissue;

        @NotNull
        private AzureEndpoint dokarkiv;

        @NotNull
        private AzureEndpoint pdl;

        @NotNull
        private AzureEndpoint oppgave;
    }

    @Data
    @Validated
    public static class AzureEndpoint {
        @NotEmpty
        private String url;
        @NotEmpty
        private String scope;
    }

    @Data
    @Validated
    public static class SlackProperties {
        @NotEmpty
        @ToString.Exclude
        private String token;
        @NotEmpty
        private String channel;
        private boolean enabled;
    }
}


