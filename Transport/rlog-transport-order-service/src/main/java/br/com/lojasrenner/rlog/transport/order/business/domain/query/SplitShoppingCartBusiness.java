package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.UnavailableSkuStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SplitShoppingCartBusiness {

	@Autowired
	private EcommBusiness ecommBusiness;

	@Autowired
	private LiveConfig config;

	private static final String UNAVAILABLE_KEY = "0";

	public Map<String, List<CartItem>> splitShoppingCartByOrigin(DeliveryRequest<?> deliveryRequest,
	                                                             final List<CartItem> itemsList,
	                                                             final List<LocationStockV1Response> stockResponse,
	                                                             final LocationStockV1Response bestLocation) {
		deliveryRequest.getStatistics().incrementCounter("splitShoppingCartByOrigin");

		final Set<LocationStockV1Response> bestLocationStock = Optional.ofNullable(bestLocation.getBranchOfficeId().split("\\+"))
				.map(Arrays::asList)
				.orElseGet(Collections::emptyList)
				.stream()
				.map(branchOfficeId -> stockResponse.stream()
						.filter(l -> l.getBranchOfficeId().equals(branchOfficeId))
						.collect(Collectors.toList()))
				.flatMap(List::stream)
				.collect(Collectors.toSet());

		final String branchEcomId = ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName())
				.getBranchOfficeId();

		final var unavailableSkusStrategy = config.getConfigValueString(deliveryRequest.getCompanyId(), Optional.ofNullable(deliveryRequest.getXApplicationName()), CompanyConfigEntity::getUnavailableSkusStrategy, true);

		return itemsList.parallelStream()
				.map(cartItem -> getMapBranchIdToCartItem(cartItem, bestLocationStock, unavailableSkusStrategy, branchEcomId))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(existing, replacement) -> Stream.concat(existing.stream(), replacement.stream()).collect(Collectors.toList()))
				);
	}

	private Map.Entry<String, List<CartItem>> getMapBranchIdToCartItem(final CartItem cartItem, final Set<LocationStockV1Response> bestLocationStock, final String unavailableSkusStrategy, final String branchEcomId) {

		final Map<String, Integer> availablePerBranch = bestLocationStock.stream()
				.collect(Collectors.toMap(LocationStockV1Response::getBranchOfficeId, v -> getAmountSaleableForItem(cartItem.getSku(), v)));

		var branchWithMostAvailable = availablePerBranch.entrySet()
				.stream()
				.filter(entry -> entry.getValue() >= cartItem.getQuantity())
				.sorted((a, b) -> b.getValue() - a.getValue())
				.sorted((a, b) -> (a.getKey().equals(branchEcomId) ? 1 : 0) - (b.getKey().equals(branchEcomId) ? 1 : 0))
				.findFirst();

		if (branchWithMostAvailable.isPresent()) {
			return Map.entry(branchWithMostAvailable.get().getKey(), Collections.singletonList(cartItem));
		} else {
			if (Objects.equals(UnavailableSkuStrategyEnum.fromValue(unavailableSkusStrategy), UnavailableSkuStrategyEnum.UNAVAILABLE_MODE)) {
				return Map.entry(UNAVAILABLE_KEY, Collections.singletonList(cartItem));
			} else {
				return Map.entry(branchEcomId, Collections.singletonList(cartItem));
			}
		}
	}

	private Integer getAmountSaleableForItem(final String sku, final LocationStockV1Response locationStockV1Response) {
		return locationStockV1Response.getItems()
				.stream()
				.filter(locationStock -> locationStock.getSku().equals(sku))
				.findFirst()
				.filter(Predicate.not(LocationStockItemV1Response::isBlocked))
				.map(LocationStockItemV1Response::getAmountSaleable)
				.orElse(0);
	}

}
