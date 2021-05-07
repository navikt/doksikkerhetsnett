package no.nav.doksikkerhetsnett.consumers.pdl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class PdlRequest {
    private final String query;
    private final Map<String, Object> variables;

    @JsonCreator
    public PdlRequest(@JsonProperty("query") String query,
                      @JsonProperty("variables") Map<String, Object> variables) {
        this.query = query;
        this.variables = variables;
    }
}
