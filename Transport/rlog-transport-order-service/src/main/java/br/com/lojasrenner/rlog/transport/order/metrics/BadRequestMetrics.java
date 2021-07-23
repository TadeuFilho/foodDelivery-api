package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ReasonTypeEnum;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class BadRequestMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "badRequest";
	}

	public void sendBadRequestMetrics(String companyId, String channel, ReasonTypeEnum reason, String controllerName) {
		Counter.Builder builder = beginCounter("bfl_bad_request", "Counts how many errors occurred with the request body");
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		addTag(companyId, channel, builder, "reason", reason.toString());
		addTag(companyId, channel, builder, "controller", controllerName);
		Counter counter = register(companyId, channel, builder);
		increment(counter, 1);
	}

}
