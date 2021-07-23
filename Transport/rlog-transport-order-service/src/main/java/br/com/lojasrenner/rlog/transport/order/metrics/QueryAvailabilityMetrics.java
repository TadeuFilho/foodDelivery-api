package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsAvailabilityEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.MetricsOriginsEnum;
import br.com.lojasrenner.rlog.transport.order.metrics.properties.AvailabilityQueryMetricsProperties;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class QueryAvailabilityMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "queryAvailability";
	}

	public void sendAvailabilityMetricsForQuery(AvailabilityQueryMetricsProperties properties) {
		Counter.Builder builder = beginCounter("bfl_delivery_options_availability_query", "count for delivery options with stock available, partial and unavailable");
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "companyId", properties.getCompanyId());
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "channel", properties.getChannel());
		addTag(properties.getCompanyId(), properties.getChannel(), builder, "availability", properties.getAvailability().name());

		if (properties.getReason() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "reason", properties.getReason().name());

		if (properties.getSkusWithProblems() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "skusWithProblems", properties.getSkusWithProblems());

		if (properties.getDestinationCountry() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "destinationCountry", properties.getDestinationCountry());

		if (Objects.nonNull(properties.getDestinationState()) && properties.getAvailability() != DeliveryOptionsAvailabilityEnum.AVAILABLE)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "destinationState", properties.getDestinationState());

		if (properties.getDestinationCity() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "destinationCity", properties.getDestinationCity());

		if (properties.getDestinationZipCode() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "destinationZipCode", properties.getDestinationZipCode());

		if (properties.getOriginCity() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originCity", properties.getOriginCity());

		if (properties.getOriginState() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originState", properties.getOriginState());

		if (properties.getOriginCountry() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originCountry", properties.getOriginCountry());

		if (properties.getOriginZipCode() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originZipCode", properties.getOriginZipCode());

		if (properties.getOriginBranch() != null)
			addTag(properties.getCompanyId(), properties.getChannel(), builder, "originBranch", properties.getOriginBranch());

		addTag(properties.getCompanyId(), properties.getChannel(), builder, "origin", MetricsOriginsEnum.QUERY.toString());
		Counter counter = register(properties.getCompanyId(), properties.getChannel(), builder);
		increment(counter, 1);
	}
}
