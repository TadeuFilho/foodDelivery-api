package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.EndpointEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ServiceTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TypeTimeoutEnum;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class TimeoutMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "timeout";
	}

	public void sendTimeoutMetrics(String companyId, String channel, ServiceTypeEnum service, EndpointEnum endpoint, TypeTimeoutEnum type) {
		Counter.Builder builder = beginCounter("bfl_timeout_services", "Counts how much time has been exceeded by a particular type of service");
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		addTag(companyId, channel, builder, "service", service.toString());
		addTag(companyId, channel, builder, "endpoint", endpoint.toString());
		addTag(companyId, channel, builder, "type", type.toString());
		Counter counter = register(companyId, channel, builder);
		increment(counter, 1);
	}
}
