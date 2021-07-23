package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.metrics.properties.AvailabilityFulfillMetricsProperties;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FulfillmentAvailabilityMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "fulfillmentAvailability";
	}

	public void sendAvailabilityMetricsForFulfill(AvailabilityFulfillMetricsProperties properties) {
		Counter.Builder builder = beginCounter("bfl_delivery_options_availability_fulfill", "count for delivery options with stock available, partial and unavailable");
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "companyId", properties.getCompanyId());
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "channel", properties.getChannel());
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "availability", properties.getAvailability().name());

		if (Objects.nonNull(properties.getReason()) && properties.getAvailability() != DeliveryOptionsAvailabilityEnum.AVAILABLE)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "reason", properties.getReason());

		if (properties.getOrigin() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "origin", properties.getOrigin().toString());

		if (properties.getBranchId() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "branchId", properties.getBranchId());

		if (properties.getSkusWithProblems() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "skusWithProblems", properties.getSkusWithProblems());

		if (properties.getOriginType() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originType", properties.getOriginType().toString());

		if (properties.getStockType() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "stockType", properties.getStockType().toString());

		if (properties.getShippingMethod() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "shippingMethod", properties.getShippingMethod().toString());

		addTag(properties.getCompanyId(), properties.getChannel(), builder, "fulfillmentConditionsHasChanged", properties.getFulfillmentConditionsHasChanged().toString());
		Counter counter = register(properties.getCompanyId(), properties.getChannel(), builder);
		increment(counter, 1);
	}
}
