package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DataLakeTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Log4j2
public class HandleResponse {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private DataLakeService dataLakeService;

    @Autowired
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

    @Async
    public void handleQueryResponse(final DeliveryOptionsRequest deliveryOptionsRequest,
                                    final String companyId,
                                    final String xApplicationName) {
        try {
            deliveryOptionsRequest.setErrorFlags();
            deliveryOptionsRequest.calculateStatistics();
            metricsService.sendAvailabilityMetricsForQuery(deliveryOptionsRequest);
            metricsService.sendCombinationsMetrics(deliveryOptionsRequest);
            metricsService.sendOriginsMetrics(deliveryOptionsRequest);

            final String transactionId = dataLakeService.send(companyId, Optional.ofNullable(xApplicationName), deliveryOptionsRequest, DataLakeTypeEnum.QUOTE);
            deliveryOptionsRequest.setTransactionId(transactionId);
            deliveryOptionsDB.save(deliveryOptionsRequest);
        } catch (Exception e) {
            log.error("Error saving entity:", e);
        }
        log.info("delivery options request end: {}", deliveryOptionsRequest);
    }

    public void handleQueryResponseSync(final DeliveryOptionsRequest deliveryOptionsRequest,
                                    final String companyId,
                                    final String xApplicationName) {
        try {
            deliveryOptionsRequest.setErrorFlags();
            deliveryOptionsRequest.calculateStatistics();
            metricsService.sendAvailabilityMetricsForQuery(deliveryOptionsRequest);
            metricsService.sendCombinationsMetrics(deliveryOptionsRequest);
            metricsService.sendOriginsMetrics(deliveryOptionsRequest);

            final String transactionId = dataLakeService.send(companyId, Optional.ofNullable(xApplicationName), deliveryOptionsRequest, DataLakeTypeEnum.QUOTE);
            deliveryOptionsRequest.setTransactionId(transactionId);
            deliveryOptionsDB.save(deliveryOptionsRequest);
        } catch (Exception e) {
            log.error("Error saving entity:", e);
        }
        log.info("delivery options request end: {}", deliveryOptionsRequest);
    }

}
