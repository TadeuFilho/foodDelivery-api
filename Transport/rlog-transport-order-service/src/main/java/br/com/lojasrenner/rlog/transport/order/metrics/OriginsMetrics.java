package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.GroupOriginType;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Component;

@Component
public class OriginsMetrics extends BaseMetrics{
	@Override
	protected String getMetricName() {
		return "originMetric";
	}

	public void sendOriginsMetrics(String companyId, String channel, double originsQuantity, GroupOriginType originType) {
		DistributionSummary.Builder builder = beginDistribution("bfl_delivery_options_origins_counter", "find the quantity of origins");
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		addTag(companyId, channel, builder, "groupOriginType", originType.toString());
		DistributionSummary distribution = register(companyId, channel, builder);
		record(distribution, originsQuantity);
	}
}
