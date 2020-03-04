package no.nav.doksikkerhetsnett.entities.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JiraResponse {

    @NotNull
    private String id;

    @NotNull
    private String key;

    private String self;
}