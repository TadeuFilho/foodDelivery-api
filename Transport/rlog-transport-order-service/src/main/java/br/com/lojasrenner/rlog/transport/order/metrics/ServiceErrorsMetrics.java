package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class ServiceErrorsMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "serviceErrors";
	}

	public void sendErrorMetric(String companyId, String channel, String reason, ServiceTypeEnum service, EndpointEnum endpoint) {
		Counter.Builder builder = beginCounter("bfl_services_error", "Counts how many errors occurred by a particular type of service");
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		addTag(companyId, channel, builder, "service", service.toString());
		addTag(companyId, channel, builder, "endpoint", endpoint.toString());
		addTag(companyId, channel, builder, "reason", reason);
		Counter counter = register(companyId, channel, builder);
		increment(counter, 1);
	}
}
