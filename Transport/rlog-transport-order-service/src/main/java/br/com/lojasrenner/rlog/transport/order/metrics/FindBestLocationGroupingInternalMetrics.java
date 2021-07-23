package br.com.lojasrenner.rlog.transport.order.metrics;

import org.springframework.stereotype.Component;

@Component
public class FindBestLocationGroupingInternalMetrics extends QuoteStatusMetrics {

	@Override
	protected String getMetricName() {
		return "findBestLocationGroupingInternal";
	}

	@Override
	protected String getDescription() { return "find the best indoor location grouping"; }
}
