package no.nav.doksikkerhetsnett.constants;

public class MDCConstants {
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_NAV_CALL_ID = "Nav-CallId";
    public static final String MDC_CALL_ID = "callId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_NAV_CONSUMER_ID = "Nav-Consumer-Id";
    public static final String MDC_APP_ID = "appId";

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String UUID_HEADER = "X-Uuid";

    private MDCConstants() {
        //no-op
    }
}
