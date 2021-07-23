package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Component
@Log4j2
public class ElasticSearchClient {

	@Autowired
	private ObjectMapper mapper;

	@Value("${spring.profiles.active}")
	private String environmentCloud;

	@Autowired
	@Qualifier("client")
	private RestHighLevelClient restHighLevelClient;

	public void newMetrics(String id, Object entity, String indexName) {
		try {
			DateTime date = DateTime.now();
			IndexRequest request = new IndexRequest(
					String.format("bfl-%s-metrics-broker-fulfillment-service-%s-%d.%s.%s",
							indexName,
							environmentCloud,
							date.getYear(),
							StringUtils.leftPad(String.valueOf(date.getMonthOfYear()), 2, "0"),
							StringUtils.leftPad(String.valueOf(date.getDayOfMonth()), 2, "0")));
			request.id(id);
			request.source(mapper.writeValueAsString(entity), XContentType.JSON);
			Map<String, Object> sourceAsMap = request.sourceAsMap();
			sourceAsMap.put("@timestamp", new Date());
			request.source(sourceAsMap);
			restHighLevelClient.index(request, RequestOptions.DEFAULT);
		} catch (IOException | ElasticsearchException ex) {
			log.error(ex.getMessage(), ex);
		}
	}
}
