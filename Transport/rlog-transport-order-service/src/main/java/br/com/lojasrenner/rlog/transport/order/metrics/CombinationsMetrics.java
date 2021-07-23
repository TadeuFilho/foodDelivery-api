package br.com.lojasrenner.rlog.transport.order.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class CombinationsMetrics {

	@Autowired
	private SplitShoppingCartByOriginMetrics splitShoppingCartByOriginMetrics;

	@Autowired
	private FindBestLocationGroupingInternalMetrics findBestLocationGroupingInternalMetrics;

	@Autowired
	private FindBestLocationGroupingMetrics findBestLocationGroupingMetrics;

	public void sendCounterMap(String companyId, String channel, Map<String, Integer> counterMap) {
		double splitShoppingCartByOrigin = 0.0;
		double findBestLocationGroupingInternal = 0.0;
		double findBestLocationGrouping = 0.0;

		if (Objects.nonNull(counterMap)) {
			if (counterMap.containsKey("splitShoppingCartByOrigin"))
				splitShoppingCartByOrigin = (double) counterMap.get("splitShoppingCartByOrigin");
			if (counterMap.containsKey("findBestLocationGrouping-internal"))
				findBestLocationGroupingInternal = (double) counterMap.get("findBestLocationGrouping-internal");
			if (counterMap.containsKey("findBestLocationGrouping"))
				findBestLocationGrouping = (double) counterMap.get("findBestLocationGrouping");
		}

		splitShoppingCartByOriginMetrics.send(companyId, channel, splitShoppingCartByOrigin);
		findBestLocationGroupingInternalMetrics.send(companyId, channel, findBestLocationGroupingInternal);
		findBestLocationGroupingMetrics.send(companyId, channel, findBestLocationGrouping);
	}
}
