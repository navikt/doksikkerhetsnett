package no.nav.doksikkerhetsnett.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@Validated
public class ServiceUserProperties {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

} 
