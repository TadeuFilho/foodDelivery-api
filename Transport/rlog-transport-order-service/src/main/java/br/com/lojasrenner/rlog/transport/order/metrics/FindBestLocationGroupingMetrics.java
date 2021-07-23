package br.com.lojasrenner.rlog.transport.order.metrics;

import org.springframework.stereotype.Component;

@Component
public class FindBestLocationGroupingMetrics extends QuoteStatusMetrics {
	@Override
	protected String getMetricName() {
		return "findBestLocationGrouping";
	}

	@Override
	protected String getDescription() { return "find the best grouping of locations"; }
}
