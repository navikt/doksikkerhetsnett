package no.nav.doksikkerhetsnett.entities.jira;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Fields {

    @NotNull
    private Project project;

    @NotNull
    private Issuetype issuetype;

    @NotNull
    private List<String> labels;

    @NotNull
    private String summary;

    @NotNull
    private String description;

}
