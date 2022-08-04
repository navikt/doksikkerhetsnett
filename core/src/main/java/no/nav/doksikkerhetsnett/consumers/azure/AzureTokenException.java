package no.nav.doksikkerhetsnett.consumers.azure;

public class AzureTokenException extends RuntimeException {
	public AzureTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
