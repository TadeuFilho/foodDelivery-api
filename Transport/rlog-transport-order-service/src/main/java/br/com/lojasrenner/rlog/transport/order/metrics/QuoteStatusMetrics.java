package br.com.lojasrenner.rlog.transport.order.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Component;

@Component
public abstract class QuoteStatusMetrics extends BaseMetrics {
	protected abstract String getDescription();

	protected void send(String companyId, String channel, double value) {
		DistributionSummary.Builder builder = beginDistribution("bfl_delivery_options_counter", getDescription());
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		addTag(companyId, channel, builder, "name", getMetricName());
		DistributionSummary distribution = register(companyId, channel, builder);
		record(distribution, value);
	}
}
