package no.nav.doksikkerhetsnett.config.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("doksikkerhetsnett")
@Validated
public class DokSikkerhetsnettProperties {

    private final Endpoints endpoints = new Endpoints();
    private final Dokarkiv dokarkiv = new Dokarkiv();

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
        private String pdl;

        @NotEmpty
        private String oppgave;

        @NotEmpty
        private String sts;

        @NotNull
        private String opprettjiraissue;
    }

    @Data
    @Validated
    public static class Dokarkiv {
        @NotEmpty
        private String url;
        @NotEmpty
        private String scope;
    }
}


