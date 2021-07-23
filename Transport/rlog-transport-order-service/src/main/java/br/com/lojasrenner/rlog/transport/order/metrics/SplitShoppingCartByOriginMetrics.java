package br.com.lojasrenner.rlog.transport.order.metrics;

import org.springframework.stereotype.Component;

@Component
public class SplitShoppingCartByOriginMetrics extends QuoteStatusMetrics {

	@Override
	protected String getMetricName() {
		return "splitShoppingCartByOrigin";
	}

	@Override
	protected String getDescription() { return "find the best indoor location grouping"; }
}
