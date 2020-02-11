package no.nav.doksikkerhetsnett;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import no.nav.doksikkerhetsnett.config.MDCConstants;
import no.nav.doksikkerhetsnett.consumer.FinnMottatteJournalposterResponse;
import no.nav.doksikkerhetsnett.exceptions.functional.AbstractDoksikkerhetsnettFunctionalException;
import no.nav.doksikkerhetsnett.exceptions.technical.AbstractDoksikkerhetsnettTechnicalException;
import no.nav.doksikkerhetsnett.metrics.MetricLabels;
import no.nav.doksikkerhetsnett.metrics.Metrics;
import no.nav.doksikkerhetsnett.service.FinnMottatteJournalposterService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.UUID;

@Api
@RestController
@RequestMapping("/rest")
@Slf4j

//TODO: Slett klassen etter schedule-jobben er fikset
public class FinnMottatteJournalposterRestController {
	private final FinnMottatteJournalposterService finnMottatteJournalposterService;
	
	@Inject
	public FinnMottatteJournalposterRestController(final FinnMottatteJournalposterService finnMottatteJournalposterService) {
		this.finnMottatteJournalposterService = finnMottatteJournalposterService;
	}

	@ApiOperation(value = "Finn mottatte journalposter.")
	@GetMapping("/{temaer}")
	@Metrics(value = MetricLabels.DOK_METRIC, extraTags = {MetricLabels.PROCESS_NAME, "finnMottatteJournalposter"}, percentiles = {0.5, 0.95}, histogram = true, logExceptions = false, createErrorMetric = true)
	public ResponseEntity<FinnMottatteJournalposterResponse> tilknyttVedlegg(@ApiParam(value = "Nav-Consumer-Id - brukes for sporingsinfo i joark", required = true) @RequestHeader(value = "Nav-Consumer-Id") String navConsumerId,
																			 @ApiParam(value = "Nav-CallId - teknisk sporingsid") @RequestHeader(value = "Nav-CallId", required = false) String navCallId,
																			 @ApiParam(value = "Liste av temaer, evt tom for alle journalposter", required = false) @PathVariable String temaer) {
		checkValuesForMDC(navCallId, navConsumerId);

		try {
			boolean hasTemaer = StringUtils.isEmpty(temaer);
			if (hasTemaer) {
				log.info("finnMottatteJournalposter har fått har fått kall for å finne alle mottatte journalposter med tema blandt: \"{}\"", temaer);
			} else {
				log.info("finnMottatteJournalposter har fått har fått kall for å finne alle mottatte journalposter");
			}

			FinnMottatteJournalposterResponse finnMottatteJournalposterResponse = finnMottatteJournalposterService.finnMottatteJournalPoster(temaer);

			if (hasTemaer) {
				log.info("finnMottatteJournalposter har funnet {} mottatte journalposter med tema blandt: \"{}\"",
						finnMottatteJournalposterResponse.getJournalposter().size(), temaer);
			} else {
				log.info("finnMottatteJournalposter har funnet {} mottatte journalposter",
						finnMottatteJournalposterResponse.getJournalposter().size());
			}

			if (finnMottatteJournalposterResponse.getJournalposter() == null || finnMottatteJournalposterResponse.getJournalposter()
					.isEmpty()) {
				return ResponseEntity.ok().body(finnMottatteJournalposterResponse);
			} else {
				return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(finnMottatteJournalposterResponse);
			}

		} catch (AbstractDoksikkerhetsnettFunctionalException e) {
			log.warn("finnMottatteJournalposter- feilet funksjonelt ved søk etter alle mottatte journalposter(evt med tema i: \"{}\")", temaer, e
					.getMessage());
			throw e;
		} catch (AbstractDoksikkerhetsnettTechnicalException e) {
			log.warn("finnMottatteJournalposter - feilet teknisk ved søk etter alle mottatte journalposter(evt med tema i: \"{}\")", temaer, e
					.getMessage());
			throw e;
		}
	}


	private void checkValuesForMDC(String navCallId, String navConsumerId) {
		if (navConsumerId != null) {
			addValueToMDC(navConsumerId, MDCConstants.MDC_NAV_CONSUMER_ID);
		}

		if (navCallId == null || navCallId.isEmpty()) {
			navCallId = String.valueOf(UUID.randomUUID());
		}
		addValueToMDC(navCallId, MDCConstants.MDC_NAV_CALL_ID);
	}

	private void addValueToMDC(String value, String key) {
		if (value != null && !value.isEmpty()) {
			MDC.put(key, value);
		}
	}
}
