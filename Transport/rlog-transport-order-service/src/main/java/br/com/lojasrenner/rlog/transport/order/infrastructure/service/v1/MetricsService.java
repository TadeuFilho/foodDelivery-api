package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.metrics.*;
import br.com.lojasrenner.rlog.transport.order.metrics.properties.AvailabilityFulfillMetricsProperties;
import br.com.lojasrenner.rlog.transport.order.metrics.properties.AvailabilityQueryMetricsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class MetricsService {

    @Autowired
    private TimeoutMetrics timeoutMetrics;

    @Autowired
    private QueryAvailabilityMetrics availabilityMetricsQuery;

    @Autowired
    private FulfillmentAvailabilityMetrics availabilityMetricsFulfill;

    @Autowired
    private CombinationsMetrics combinationsMetrics;

    @Autowired
    private CombinationsTimeoutMetrics combinationsTimeoutMetrics;

    @Autowired
    private AttemptedCombinationsMetrics attemptedCombinationsMetrics;

    @Autowired
    private BadRequestMetrics badRequestMetrics;

    @Autowired
    private OriginsMetrics originsMetrics;

    public boolean combinationTimeoutExceeded(DeliveryRequest<?> deliveryRequest) {
        return combinationTimeoutExceeded(deliveryRequest, false);
    }

    public boolean combinationTimeoutExceeded(DeliveryRequest<?> deliveryRequest, boolean sendTimeoutMetric) {
        if ((System.currentTimeMillis() - deliveryRequest.getInitialTimestamp()) >=
                deliveryRequest.getQuoteSettings().getMaxCombinationsTimeOutUsed()) {
            deliveryRequest.getStatistics().setCombinationTimeOut(true);
            if (sendTimeoutMetric)
                timeoutMetrics.sendTimeoutMetrics(deliveryRequest.getCompanyId(),
                        deliveryRequest.getXApplicationName(),
                        ServiceTypeEnum.COMBINATIONS,
                        EndpointEnum.COMBINATIONS_DELIVERY_MODES_QUERY_FOR_SHOPPING_CART,
                        TypeTimeoutEnum.CONNECT_TIMEOUT);
            return true;
        }
        return false;
    }

    public void sendBadRequestMetrics(final String companyId, final String channel, final ReasonTypeEnum reasonTypeEnum, final String controllerName) {
        badRequestMetrics.sendBadRequestMetrics(companyId, channel, reasonTypeEnum, controllerName);
    }

    public void sendOriginsMetrics(final DeliveryOptionsRequest deliveryOptionsRequest) {
        double origins = (double) (deliveryOptionsRequest.getResponse() == null ? 0 : deliveryOptionsRequest.getResponse().getDistinctOrigins());
        GroupOriginType originType = GroupOriginType.NORMAL;

        if(Boolean.TRUE.equals(deliveryOptionsRequest.getStatistics().getPassedOnExtraGroup())) {
            originType = GroupOriginType.EXTRA_GROUP;
        } else if(Boolean.TRUE.equals(deliveryOptionsRequest.getStatistics().getPassedOnPriorityGroup()))
            originType = GroupOriginType.PRIORITY;

        originsMetrics.sendOriginsMetrics(deliveryOptionsRequest.getCompanyId(),
                deliveryOptionsRequest.getXApplicationName(),
                origins,
                originType);
    }

    public void sendAvailabilityMetricsForQuery(final DeliveryOptionsRequest deliveryOptionsRequest) {
        availabilityMetricsQuery.sendAvailabilityMetricsForQuery(AvailabilityQueryMetricsProperties.builder()
                .companyId(deliveryOptionsRequest.getCompanyId())
                .channel(deliveryOptionsRequest.getXApplicationName())
                .availability(deliveryOptionsRequest.getStatistics().getAvailability())
                .reason(Objects.nonNull(deliveryOptionsRequest.getStatistics()) ?
                        deliveryOptionsRequest.getStatistics().getReason() :
                        null)
                .destinationCity(Objects.nonNull(deliveryOptionsRequest.getShippingGroupResponseObject()) ?
                        deliveryOptionsRequest.getShippingGroupResponseObject().getCity() :
                        null)
                .destinationState(Objects.nonNull(deliveryOptionsRequest.getShippingGroupResponseObject()) ?
                        deliveryOptionsRequest.getShippingGroupResponseObject().getState() :
                        null)
                .destinationCountry(Objects.nonNull(deliveryOptionsRequest.getShippingGroupResponseObject()) ?
                        deliveryOptionsRequest.getShippingGroupResponseObject().getCountry() :
                        null)
                .destinationZipCode(deliveryOptionsRequest.getShoppingCart().getDestination().getZipcode())
                .originBranch(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getBranchId() :
                        null)
                .originCity(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getCity() :
                        null)
                .originState(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getState() :
                        null)
                .originCountry(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getCountry() :
                        null)
                .originZipCode(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getZipCode() :
                        null)
                .skusWithProblems(!deliveryOptionsRequest.getStatistics().getBranchesWithErrors().isEmpty() ?
                        deliveryOptionsRequest.getStatistics().getBranchesWithErrors().get(0).getSkus() :
                        null)
                .build());

    }

    public void sendAvailabilityMetricsForFulfill(final FulfillmentRequest fulfillmentRequest) {
        final Boolean conditionsHasChanged = fulfillmentRequest.getResponse() != null ?
                fulfillmentRequest.getResponse().isFulfillmentConditionsHasChanged() : Boolean.FALSE;

        availabilityMetricsFulfill.sendAvailabilityMetricsForFulfill(AvailabilityFulfillMetricsProperties.builder()
                .companyId(fulfillmentRequest.getCompanyId())
                .channel(fulfillmentRequest.getXApplicationName())
                .availability(fulfillmentRequest.getStatistics().getAvailability())
                .origin(MetricsOriginsEnum.FULFILLMENT)
                .reason((fulfillmentRequest.getStatistics().getReason() != null) ?
                        fulfillmentRequest.getStatistics().getReason().toString() : "")
                .branchId(fulfillmentRequest.getStatistics().getBranchId())
                .skusWithProblems((fulfillmentRequest.getStatistics().getSkusWithProblems() != null) ?
                        fulfillmentRequest.getStatistics().getReason().toString() : "")
                .fulfillmentConditionsHasChanged(conditionsHasChanged)
                .originType(fulfillmentRequest.getStatistics().getOriginType())
                .stockType(fulfillmentRequest.getStatistics().getStockType())
                .shippingMethod(fulfillmentRequest.getShippingMethod())
                .build());
    }

    public void sendCombinationsMetrics(final DeliveryOptionsRequest deliveryOptionsRequest) {
        combinationsTimeoutMetrics.sendCombinationsTimeOutMetrics(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName(), deliveryOptionsRequest.getStatistics().getCombinationTimeOut());
        attemptedCombinationsMetrics.sendAttemptedCombinationSize(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName(), deliveryOptionsRequest.getStatistics().getAttemptedCombinations());
        combinationsMetrics.sendCounterMap(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName(), deliveryOptionsRequest.getStatistics().getCounterMap());
    }

}
