package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.client.ElasticSearchClient;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.BranchOfficeMetricProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@Log4j2
public class ElasticBranchOfficeWithoutParamsService {

	@Autowired
	private ElasticSearchClient elasticSearchClient;

	public void registerMetrics(BranchOfficeMetricProperties properties) {
		elasticSearchClient.newMetrics(UUID.randomUUID().toString(), properties,"branch-office-without-params");
	}

}
