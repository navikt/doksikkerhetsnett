package no.nav.doksikkerhetsnett.consumer.sts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
public class StsResponseTo {

	private final String accessToken;
	private final String tokenType;
	private final String expiresIn;

	@JsonCreator
	public StsResponseTo(@JsonProperty("access_token") String accessToken, @JsonProperty("token_type") String tokenType, @JsonProperty("expires_in") String expiresIn) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;

	}
}
