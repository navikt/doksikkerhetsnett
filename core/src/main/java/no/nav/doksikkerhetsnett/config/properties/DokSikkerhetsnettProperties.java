package no.nav.doksikkerhetsnett.config.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@ConfigurationProperties("doksikkerhetsnett")
@Validated
public class DokSikkerhetsnettProperties {

    private final Proxy proxy = new Proxy();
    private final Endpoints endpoints = new Endpoints();

    @NotNull
    private ServiceUserProperties serviceuser;

    //kommaseparert liste, "ALLE" for å opprette oppgaver på alle temaer.
    private String skrivTemaer;

    //kommaseparert liste, "ALLE" for å lese alle temaer.
    private String lesTemaer;

    @Data
    @Validated
    public static class Proxy {
        private String host;
        private int port;

        public boolean isSet() {
            return (host != null && !host.equals(""));
        }
    }

    @Data
    @Validated
    public static class Endpoints {

        @NotNull
        private AzureEndpoint dokarkiv;

        /*
        @NotNull
        private String finnmottattejournalposter;*/

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
    public static class AzureEndpoint {
        /**
         * Url til tjeneste som har azure autorisasjon
         */
        @NotEmpty
        private String url;
        /**
         * Scope til azure client credential flow
         */
        @NotEmpty
        private String scope;
    }

}


