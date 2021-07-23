package br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.strategy;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.OriginPreview;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeliveryOptionsOriginTypeStrategys {

	private static final String UNALLOCATED_INVENTORY = "0";

	public static DeliveryOptionsOriginTypeStrategy getOkSameOriginStrategy() {
		return (fulfillmentRequestCartOrderItems, fulfillmentRequestMainBranch, reQuoteOriginItem, unavailableOriginItem, newOriginItem) ->
				Map.of(fulfillmentRequestMainBranch, fulfillmentRequestCartOrderItems);
	}

	public static DeliveryOptionsOriginTypeStrategy getOkNewOriginStrategy() {
		return (fulfillmentRequestCartOrderItems, fulfillmentRequestMainBranch, reQuoteOriginItem, unavailableOriginItem, newOriginItem) ->
				Map.of(newOriginItem.get().getBranchId(), fulfillmentRequestCartOrderItems);
	}

	public static DeliveryOptionsOriginTypeStrategy getNoOriginStrategy() {
		return (fulfillmentRequestCartOrderItems, fulfillmentRequestMainBranch, reQuoteOriginItem, unavailableOriginItem, newOriginItem) ->
				Map.of(unavailableOriginItem.get().getBranchId(), fulfillmentRequestCartOrderItems);
	}

	public static DeliveryOptionsOriginTypeStrategy getPartialOriginStrategy() {
		return (fulfillmentRequestCartOrderItems, fulfillmentRequestMainBranch, reQuoteOriginItem, unavailableOriginItem, newOriginItem) -> {
			var reQuoteOriginMap = reQuoteOriginItem
					.map(reQuoteOrigin -> Map.of(fulfillmentRequestMainBranch, fulfillmentRequestCartOrderItems.stream()
																				.filter(i -> reQuoteOrigin.getSkus().contains(i.getSku()))
																				.collect(Collectors.toList())))
					.orElse(Collections.emptyMap());

			var newOriginItemMap = newOriginItem
					.map(newOrigin -> Map.of(newOrigin.getBranchId(), fulfillmentRequestCartOrderItems.stream()
																			.filter(i -> newOrigin.getSkus().contains(i.getSku()))
																			.collect(Collectors.toList())))
					.orElse(Collections.emptyMap());

			var unavailableOriginItemMap = getUnavailableOriginItem(unavailableOriginItem, fulfillmentRequestCartOrderItems);

			return mergeMaps(reQuoteOriginMap, newOriginItemMap, unavailableOriginItemMap);
		};
	}

	public static DeliveryOptionsOriginTypeStrategy getPartialNoOriginStrategy() {
		return (fulfillmentRequestCartOrderItems, fulfillmentRequestMainBranch, reQuoteOriginItem, unavailableOriginItem, newOriginItem) -> {
			var reQuoteOriginMap = reQuoteOriginItem
					.map(reQuoteOrigin -> Map.of(fulfillmentRequestMainBranch, fulfillmentRequestCartOrderItems.stream()
																				.filter(i -> reQuoteOrigin.getSkus().contains(i.getSku()))
																				.collect(Collectors.toList())))
					.orElse(Collections.emptyMap());

			var unavailableNewOriginItemMap = newOriginItem
					.map(newOrigin -> Map.of(UNALLOCATED_INVENTORY, fulfillmentRequestCartOrderItems.stream()
																			.filter(i -> newOrigin.getSkus().contains(i.getSku()))
																			.collect(Collectors.toList())))
					.orElse(Collections.emptyMap());


			var unavailableOriginItemMap = getUnavailableOriginItem(unavailableOriginItem, fulfillmentRequestCartOrderItems);

			return mergeMaps(reQuoteOriginMap, unavailableNewOriginItemMap, unavailableOriginItemMap);
		};
	}

	private static Map<String, List<CartItemWithMode>> getUnavailableOriginItem(final Optional<OriginPreview> unavailableOriginItem, final List<CartItemWithMode> fulfillmentRequestCartOrderItems) {
		return unavailableOriginItem
				.map(unavailableOrigin -> Map.of(unavailableOrigin.getBranchId(), fulfillmentRequestCartOrderItems.stream()
						.filter(i -> unavailableOrigin.getSkus().contains(i.getSku()))
						.collect(Collectors.toList())))
				.orElse(Collections.emptyMap());
	}

	private static Map<String, List<CartItemWithMode>> mergeMaps(Map<String, List<CartItemWithMode>>... values) {
		return Stream.of(values)
				.flatMap(map -> map.entrySet().stream())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(v1, v2) -> Stream.of(v1, v2)
								.flatMap(List::stream)
								.collect(Collectors.toList())));
	}

}
