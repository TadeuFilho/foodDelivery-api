package br.com.lojasrenner.rlog.transport.order.metrics;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public abstract class BaseMetrics {

	@Autowired
	private MeterRegistry registry;

	@Autowired
	private LiveConfig config;

	protected abstract String getMetricName();

	private List<String> getActiveTagsForMetric(String company, String channel) {
		Map<String, List<String>> metrics = config.getConfigMetricsMap(company, Optional.ofNullable(channel), CompanyConfigEntity::getMetrics, false);

		if (metrics == null)
			return new ArrayList<>();

		return metrics.getOrDefault(getMetricName(), new ArrayList<>());
	}

	private boolean isTagActive(String company, String channel, String tag) {
		List<String> activeTagsForMetric = getActiveTagsForMetric(company, channel);
		return activeTagsForMetric.contains(tag) || (activeTagsForMetric.size() == 1 && activeTagsForMetric.get(0).equals("*"));
	}

	private boolean isActive(String company, String channel) {
		List<String> activeTagsForMetric = getActiveTagsForMetric(company, channel);
		return !activeTagsForMetric.isEmpty();
	}

	protected Counter.Builder beginCounter(String table, String description) {
		return Counter.builder(table)
				.description(description);
	}

	protected DistributionSummary.Builder beginDistribution(String table, String description) {
		return DistributionSummary
				.builder(table)
				.description(description);
	}

	protected Counter.Builder addTag(String company, String channel, Counter.Builder builder, String tag, String value) {
		if (isTagActive(company, channel, tag))
			return builder.tag(tag, value);
		else return builder;
	}

	protected DistributionSummary.Builder addTag(String company, String channel, DistributionSummary.Builder builder, String tag, String value) {
		if (isTagActive(company, channel, tag))
			return builder.tag(tag, value);
		else return builder;
	}

	protected Counter register(String company, String channel, Counter.Builder builder) {
		if (isActive(company, channel))
			return builder.register(registry);
		return null;
	}

	protected DistributionSummary register(String company, String channel, DistributionSummary.Builder builder) {
		if (isActive(company, channel))
			return builder.register(registry);
		return null;
	}

	protected void increment(Counter counter, Integer increment) {
		if (counter != null)
			counter.increment(increment);
	}

	protected void record(DistributionSummary distribution, double attemptedCombinationsSize) {
		if (distribution != null)
			distribution.record(attemptedCombinationsSize);
	}

}
