package no.nav.doksikkerhetsnett.consumer.finnoppgave;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinnOppgaveResponse {

    @NotNull(message = "Feilet ved henting av oppgaver")


    private int antallTreffTotalt;

    @ApiModelProperty(
            dataType = "List",
            value = "oppgaver",
            required = true
    )
    private List<OppgaveJson> oppgaver;
}