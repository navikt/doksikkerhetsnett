package no.nav.doksikkerhetsnett.consumer;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UbehandletBruker {
    @ApiModelProperty(
            value = "ID til bruker i Joark",
            example = "22345678"
    )
    private String id;

    @ApiModelProperty(
            value = "Brukertype i Joark",
            example = "PERSON"
    )
    private String type;
}
