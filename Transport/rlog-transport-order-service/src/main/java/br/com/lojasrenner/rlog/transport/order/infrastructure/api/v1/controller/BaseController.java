package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.BrokerRequest;

public abstract class BaseController {
	
	protected BaseController() { }

	protected static void setDefaultParams(String xApplicationName, String xCurrentDate, String xLocale, String companyId,
			BrokerRequest<?> brokerRequest) {
		brokerRequest.setId(UUID.randomUUID().toString());
		brokerRequest.setInitialTimestamp(System.currentTimeMillis());
		brokerRequest.setDate(LocalDateTime.now());
		brokerRequest.setXApplicationName(xApplicationName);
		brokerRequest.setXCurrentDate(xCurrentDate);
		brokerRequest.setXLocale(xLocale);
		brokerRequest.setCompanyId(companyId);
	}
	
}
