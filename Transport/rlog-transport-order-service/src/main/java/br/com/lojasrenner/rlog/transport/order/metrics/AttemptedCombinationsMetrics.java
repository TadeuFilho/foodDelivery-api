package br.com.lojasrenner.rlog.transport.order.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class AttemptedCombinationsMetrics extends BaseMetrics {

	@Override
	protected String getMetricName() {
		return "attemptedCombinationsSize";
	}

	public void sendAttemptedCombinationSize(String companyId, String channel, List<String> attemptedCombinations) {
		double attemptedCombinationsSize = Objects.nonNull(attemptedCombinations) ? (double) attemptedCombinations.size() : 0.0;

		DistributionSummary.Builder builder = beginDistribution("bfl_delivery_options_attempted_combination", "Counts the number of combinations that are made in a quote");
		addTag(companyId, channel, builder, "companyId", companyId);
		addTag(companyId, channel, builder, "channel", channel);
		DistributionSummary distribution = register(companyId, channel, builder);
		record(distribution, attemptedCombinationsSize);
	}

}
