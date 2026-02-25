package no.nav.doksikkerhetsnett.consumers.pdl;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.exceptions.functional.PdlFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.PdlTechnicalException;
import org.slf4j.MDC;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.function.Consumer;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.NavHeaders.NAV_CALL_ID;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.doksikkerhetsnett.consumers.azure.AzureProperties.CLIENT_REGISTRATION_PDL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Component
public class PdlIdentConsumer {
	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";
	// https://behandlingskatalog.nais.adeo.no/process/purpose/ARKIVPLEIE/756fd557-b95e-4b20-9de9-6179fb8317e6
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";
	private static final String PERSON_IKKE_FUNNET_CODE = "not_found";

	private final WebClient webClient;

	public PdlIdentConsumer(WebClient webClient,
							DokSikkerhetsnettProperties doksikkerhetsnettProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(doksikkerhetsnettProperties.getEndpoints().getPdl().getUrl())
				.defaultHeaders(httpHeaders -> {
					httpHeaders.set(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
					httpHeaders.setContentType(APPLICATION_JSON);
					httpHeaders.set(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
				})
				.build();
	}

	@Retryable(
			includes = PdlTechnicalException.class,
			delay = DELAY_SHORT,
			multiplier = MULTIPLIER_SHORT
	)
	public String hentAktoerId(String folkeregisterIdent) throws PersonIkkeFunnetException {
		return webClient.post()
				.uri("/graphql")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_PDL))
				.bodyValue(mapHentAktoerIdForFolkeregisterident(folkeregisterIdent))
				.retrieve()
				.bodyToMono(PdlResponse.class)
				.doOnError(handlePdlErrors())
				.mapNotNull(this::mapPersonInfo)
				.block();

	}

	private PdlRequest mapHentAktoerIdForFolkeregisterident(final String ident) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", ident);
		return PdlRequest.builder()
				.query("query hentIdenter($ident: ID!) {hentIdenter(ident: $ident, grupper: AKTORID, historikk: false) {identer { ident gruppe historisk } } }")
				.variables(variables)
				.build();
	}

	private String mapPersonInfo(PdlResponse pdlResponse) {
		if (pdlResponse.getErrors() == null || pdlResponse.getErrors().isEmpty()) {
			return pdlResponse.getData().getHentIdenter().getIdenter().getFirst().getIdent();
		} else {
			if (PERSON_IKKE_FUNNET_CODE.equals(pdlResponse.getErrors().getFirst().getExtensions().getCode())) {
				throw new PersonIkkeFunnetException("Fant ikke aktørid for person i pdl.");
			}
			throw new PdlFunctionalException("Kunne ikke hente aktørid for folkeregisterident i pdl. " + pdlResponse.getErrors());
		}
	}

	private Consumer<Throwable> handlePdlErrors() {
		return error ->
		{
			throw new PdlTechnicalException("Ukjent teknisk feil mot pdl: ", error);
		};
	}
}
