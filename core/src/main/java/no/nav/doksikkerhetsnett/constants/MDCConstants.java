package no.nav.doksikkerhetsnett.constants;

import java.util.Set;

public class MDCConstants {
	public static final String MDC_CALL_ID = "callId";

	public static Set<String> ALL_KEYS = Set.of(MDC_CALL_ID);

	private MDCConstants() {
		//no-op
	}
}
