package no.nav.doksikkerhetsnett.entities.responses;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.doksikkerhetsnett.entities.UbehandletJournalpost;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinnMottatteJournalposterResponse {

    @NotNull(message = "FinnMottatteJournalposterResponse mangler Journalposter")
    @ApiModelProperty(
            dataType = "List",
            value = "journalposter",
            required = true
    )
    private List<UbehandletJournalpost> journalposter;
}