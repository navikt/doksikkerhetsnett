package no.nav.doksikkerhetsnett.consumers.pdl;

import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.consumers.StsRestConsumer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;

import static java.util.Objects.requireNonNull;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.DELAY_SHORT;
import static no.nav.doksikkerhetsnett.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class PdlIdentConsumer implements IdentConsumer {
	private static final String HEADER_PDL_BEHANDLINGSNUMMER = "behandlingsnummer";
	// https://behandlingskatalog.nais.adeo.no/process/purpose/ARKIVPLEIE/756fd557-b95e-4b20-9de9-6179fb8317e6
	private static final String ARKIVPLEIE_BEHANDLINGSNUMMER = "B315";
	private static final String PERSON_IKKE_FUNNET_CODE = "not_found";

	private final RestTemplate restTemplate;
	private final StsRestConsumer stsRestConsumer;
	private final URI pdlUri;

	public PdlIdentConsumer(RestTemplateBuilder restTemplateBuilder,
							StsRestConsumer stsRestConsumer,
							DokSikkerhetsnettProperties doksikkerhetsnettProperties) {
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(3))
				.setReadTimeout(Duration.ofSeconds(20))
				.build();
		this.stsRestConsumer = stsRestConsumer;
		this.pdlUri = UriComponentsBuilder.fromHttpUrl(doksikkerhetsnettProperties.getEndpoints().getPdl()).build().toUri();
	}

	@Retryable(
			retryFor = HttpServerErrorException.class,
			backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT)
	)
	@Override
	public String hentAktoerId(String folkeregisterIdent) throws PersonIkkeFunnetException {
		if (isBlank(folkeregisterIdent)) {
			throw new PersonIkkeFunnetException("Folkeregisterident er null eller blank.");
		}
		try {
			final RequestEntity<PdlRequest> requestEntity = baseRequest()
					.body(mapHentAktoerIdForFolkeregisterident(folkeregisterIdent));
			final PdlResponse pdlResponse = requireNonNull(restTemplate.exchange(requestEntity, PdlResponse.class).getBody());

			if (pdlResponse.getErrors() == null || pdlResponse.getErrors().isEmpty()) {
				return pdlResponse.getData().getHentIdenter().getIdenter().get(0).getIdent();
			} else {
				if (PERSON_IKKE_FUNNET_CODE.equals(pdlResponse.getErrors().get(0).getExtensions().getCode())) {
					throw new PersonIkkeFunnetException("Fant ikke aktørid for person i pdl.");
				}
				throw new PdlFunctionalException("Kunne ikke hente aktørid for folkeregisterident i pdl. " + pdlResponse.getErrors());
			}
		} catch (HttpClientErrorException e) {
			throw new PdlFunctionalException("Kall mot pdl feilet funksjonelt.", e);
		}
	}

	private PdlRequest mapHentAktoerIdForFolkeregisterident(final String ident) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", ident);
		return PdlRequest.builder()
				.query("query hentIdenter($ident: ID!) {hentIdenter(ident: $ident, grupper: AKTORID, historikk: false) {identer { ident gruppe historisk } } }")
				.variables(variables)
				.build();
	}

	private RequestEntity.BodyBuilder baseRequest() {
		final String serviceuserToken = stsRestConsumer.getOidcToken();
		return RequestEntity.post(pdlUri)
				.accept(APPLICATION_JSON)
				.headers(httpHeaders -> {
					httpHeaders.setContentType(APPLICATION_JSON);
					httpHeaders.setBearerAuth(serviceuserToken);
					httpHeaders.set(HEADER_PDL_BEHANDLINGSNUMMER, ARKIVPLEIE_BEHANDLINGSNUMMER);
				});
	}
}
