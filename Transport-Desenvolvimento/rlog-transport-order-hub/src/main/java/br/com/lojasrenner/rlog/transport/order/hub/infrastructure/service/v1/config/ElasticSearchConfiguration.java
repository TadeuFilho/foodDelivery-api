package br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service.v1.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration {
	@Value("${oms.elastic.host}")
	private String host;

	@Value("${oms.elastic.port}")
	private int port;

	@Value("${oms.elastic.username}")
	private String username;

	@Value("${oms.elastic.password}")
	private String password;

	@Value("${oms.elastic.timeout}")
	private Integer timeout;

	@Bean
	public RestHighLevelClient client() {
		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

		HttpHost httpHost = new HttpHost(host, port, "http");
		RestClientBuilder restBuilder = RestClient.builder(httpHost)
				.setHttpClientConfigCallback(httpClientBuilder ->
						httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
				.setMaxRetryTimeoutMillis(timeout);

		return new RestHighLevelClient(restBuilder);
	}

	@Bean(destroyMethod = "close")
	public RestClient restClient() {
		return client().getLowLevelClient();
	}
}
