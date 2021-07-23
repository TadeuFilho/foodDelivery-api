package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.BrokerRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateBrokerRequestParams {

    default void setDefaultParams(String xApplicationName, String xCurrentDate, String xLocale, String companyId, BrokerRequest<?> brokerRequest) {
        brokerRequest.setId(UUID.randomUUID().toString());
        brokerRequest.setInitialTimestamp(System.currentTimeMillis());
        brokerRequest.setDate(LocalDateTime.now());
        brokerRequest.setXApplicationName(xApplicationName);
        brokerRequest.setXCurrentDate(xCurrentDate);
        brokerRequest.setXLocale(xLocale);
        brokerRequest.setCompanyId(companyId);
    }

}
