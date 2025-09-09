package no.nav.doksikkerhetsnett;

import no.nav.dok.jiraapi.JiraProperties;
import no.nav.dok.jiraapi.JiraService;
import no.nav.dok.jiraapi.client.JiraClient;
import no.nav.doksikkerhetsnett.config.properties.DokSikkerhetsnettProperties;
import no.nav.doksikkerhetsnett.config.properties.JiraAuthProperties;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@ComponentScan
@Configuration
public class CoreConfig {

	@Bean
	ClientHttpRequestFactory requestFactory(HttpClient httpClient) {
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	@Bean
	HttpClient httpClient(HttpClientConnectionManager connectionManager) {
		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.build();
	}

	@Bean
	HttpClientConnectionManager httpClientConnectionManager() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(400);
		connectionManager.setDefaultMaxPerRoute(100);
		return connectionManager;
	}

	@Bean
	public JiraService jiraService(JiraClient jiraClient) {
		return new JiraService(jiraClient);
	}

	@Bean
	public JiraClient jiraClient(DokSikkerhetsnettProperties properties, JiraAuthProperties jiraAuthProperties) {
		return new JiraClient(jiraProperties(properties, jiraAuthProperties));
	}

	public JiraProperties jiraProperties(DokSikkerhetsnettProperties properties, JiraAuthProperties jiraAuthProperties) {
		String jiraUrl = properties.getEndpoints().getJira();

		return JiraProperties.builder()
				.jiraServiceUser(new JiraProperties.JiraServiceUser(jiraAuthProperties.username(), jiraAuthProperties.password()))
				.url(jiraUrl)
				.build();
	}

}
