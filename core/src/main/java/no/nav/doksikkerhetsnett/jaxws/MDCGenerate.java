package no.nav.doksikkerhetsnett.jaxws;

import static no.nav.doksikkerhetsnett.constants.MDCConstants.MDC_CALL_ID;

import org.slf4j.MDC;

import java.util.UUID;

public class MDCGenerate {

	public static void generateNewCallIdIfThereAreNone() {
		if (MDC.get(MDC_CALL_ID) == null) {
			MDC.put(MDC_CALL_ID, UUID.randomUUID().toString());
		}
	}
}
