package br.com.lojasrenner.rlog.transport.order.business.test;

import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigPermissionEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeConfigurationEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeStatusEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteProductsRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.*;

import com.google.common.base.Strings;

import java.util.*;
import java.util.stream.Collectors;

public class BuildHelper {

	public static CartItem buildCartItem(String sku, int quantity) {
		CartItem item = CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.build();
		item.setCostOfGoods(100.0);
		return item;
	}

	public static CartItem buildCartItemWithStockStatusAndProductType(String sku, int quantity, StockStatusEnum stockStatusEnum, ProductTypeEnum productTypeEnum) {
		 CartItem item = CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.stockStatus(stockStatusEnum)
				.productType(productTypeEnum)
				.build();
		item.setCostOfGoods(100.0);
		return item;
	}

	public static CartItem buildCartItemWithCosts(String sku, int quantity, Double costOfGoods, StockStatusEnum stockStatusEnum) {
		CartItem item = CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.stockStatus(stockStatusEnum)
				.build();
		item.setCostOfGoods(costOfGoods);
		return item;
	}

	public static CartItem buildCartItemWithRelatedSku(String sku, int quantity, Double costOfGoods, ProductTypeEnum productTypeEnum, CartItem relatedSku) {
		CartItem item = CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.build();
		item.setCostOfGoods(costOfGoods);
		item.setProductType(productTypeEnum);
		return item;
	}

	public static CartItemWithMode buildCartItemWithMode(String sku, int quantity, String modalId, Integer branchOfficeId) {
		CartItemWithMode item = new CartItemWithMode();
		item.setSku(sku);
		item.setModalId(modalId);
		item.setBranchOfficeId(branchOfficeId);
		item.setQuantity(quantity);
		item.setStockStatus(StockStatusEnum.INSTOCK);
		item.setProductType(ProductTypeEnum.DEFAULT);
		item.setWeight(1.0);
		item.setWidth(1.0);
		item.setHeight(1.0);
		item.setLength(1.0);
		item.setCostOfGoods(10.0);
		return item;
	}

	public static CartItemWithMode buildCartItemWithModeAndCostOfGoods(String sku, int quantity, String modalId, Integer branchOfficeId, Double costOfGoods) {
		CartItemWithMode item = new CartItemWithMode();
		item.setSku(sku);
		item.setModalId(modalId);
		item.setBranchOfficeId(branchOfficeId);
		item.setQuantity(quantity);
		return item;
	}

	public static CartItem buildCartItemWithCosts(String sku, int quantity, Double costOfGoods) {
		CartItem item = CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.build();
		item.setCostOfGoods(costOfGoods);
		return item;
	}

	public static CartItem buildCartItemWithPreSale(String sku, int quantity, StockStatusEnum stockStatusEnum) {
		return CartItem.builder()
				.sku(sku)
				.quantity(quantity)
				.stockStatus(stockStatusEnum)
				.build();
	}

	public static ShippingGroupResponseV1 buildShippingGroup(String company, String id, String name, Integer priority, List<Integer> branches, Integer from, Integer to) {
		return ShippingGroupResponseV1.builder()
				.id(id)
				.companyId(company)
				.name(name)
				.priority(priority)
				.branches(branches)
				.zipCodeRanges(Arrays.asList(ZipCodeRange.builder()
						.from(from)
						.to(to)
						.build()))
				.build();
	}

	public static BranchOfficeEntity buildBranchOffice(String company, String branchId, boolean status, boolean pickup, boolean shipping, String orderStatus, Integer branchWithdrawalTerm, String state, boolean isEcomm, String cdManagement) {
		return BranchOfficeEntity.builder()
				.id(company + branchId)
				.name("branch " + company + " " + branchId)
				.state(state)
				.zipcode(Strings.padEnd(Strings.padStart(branchId, 5, '0') + "-", 9, '5'))
				.status(BranchOfficeStatusEntity.builder()
						.order(orderStatus)
						.build())
				.branchType(isEcomm ? "WEB_STORE" : "IN_STORE")
				.configuration(BranchOfficeConfigurationEntity.builder()
						.active(status)
						.cdManagement(cdManagement)
						.permission(BranchOfficeConfigPermissionEntity.builder()
								.branchWithdraw(pickup)
								.doShipping(shipping)
								.branchWithdrawStockCD(true)
								.build())
						.storeWithdrawalTerm(branchWithdrawalTerm)
						.build())
				.build();
	}

	public static GeoLocationResponseV1 buildGeolocationItem(String branchId, int distance, boolean inRange) {
		return GeoLocationResponseV1.builder()
				.branchOfficeId(branchId)
				.distance(distance)
				.inRange(inRange)
				.build();
	}

	public static ShippingToResponseV1 buildShippingToItem(String destinationBranch, List<String> originBranches){
		return ShippingToResponseV1.builder()
				.destinationBranch(destinationBranch)
				.originBranches(originBranches)
				.build();
	}

	public static PickupOptionsRequest buildPickupOptionsRequest(String id, String companyId) {
		PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
		pickupOptionsRequest.setDeliveryOptionsId(id);
		pickupOptionsRequest.setCompanyId(companyId);
		pickupOptionsRequest.setXApplicationName("[unit-test]");
		pickupOptionsRequest.setQuoteSettings(QuoteSettings.builder().build());
		return pickupOptionsRequest;
	}

	public static PickupOptionsRequest buildPickupOptionsRequestWithReQuote(String id, String companyId) {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest(id, companyId);
		pickupOptionsRequest.getQuoteSettings().setReQuotePickup(Boolean.TRUE);
		return pickupOptionsRequest;
	}

	public static DeliveryOptionsRequest buildDeliveryOptionsRequest(String companyId, List<CartItem> items, String zipcode, Long quoteId, int estimate, int maxOrigins, int maxOriginsStore, int combinationTimeOut, int combinationApproachCartSizeParam) {
		return buildDeliveryOptionsRequest(companyId, items, zipcode, quoteId, estimate, false, maxOrigins, maxOriginsStore, combinationTimeOut, combinationApproachCartSizeParam);
	}

	public static DeliveryOptionsRequest buildDeliveryOptionsRequest(String companyId, List<CartItem> items, String zipcode, Long quoteId, int estimate, boolean restricted, int maxOriginsParam, int maxOriginsStoreParam, int combinationsTimeoutParam, int combinationApproachCartSizeParam) {
		ShoppingCart cart = ShoppingCart.builder()
				.items(items)
				.containsRestrictedOriginItems(restricted)
				.destination(CartDestination.builder()
						.zipcode(zipcode)
						.build())
				.extraIdentification(ExtraIdentification.builder()
						.extOrderCode("5123123").build())
				.build();

		DeliveryOptionsRequest deliveryOptionsRequest = new DeliveryOptionsRequest();
		deliveryOptionsRequest.setInitialTimestamp(System.currentTimeMillis());
		deliveryOptionsRequest.setShoppingCart(cart);
		deliveryOptionsRequest.setCompanyId(companyId);
		deliveryOptionsRequest.setXApplicationName("[unit-test]");
		deliveryOptionsRequest.setQuoteSettings(QuoteSettings.builder()
				.maxOriginsHeader(maxOriginsParam)
				.maxOriginsConfig(3)
				.maxOriginsStoreHeader(maxOriginsStoreParam)
				.maxOriginsStoreConfig(2)
				.eagerBranchesConfig(Arrays.asList("899"))
				.maxCombinationsTimeOutHeader(combinationsTimeoutParam)
				.maxCombinationsTimeOutConfig(500000000)
				.combinationApproachCartSizeLimitConfig(100)
				.combinationApproachCartSizeLimitHeader(combinationApproachCartSizeParam)
				.build());

		if (quoteId != null) {
			QuoteResponseV1 quoteResponseV1 = buildQuoteResponse(quoteId, estimate);
			deliveryOptionsRequest.setQuoteFromEcomm(quoteResponseV1);
		}

		return deliveryOptionsRequest;
	}

	public static FulfillmentRequest buildFulfillmentRequest(String companyId, List<CartItemWithMode> items, String zipcode, String deliveryOptionsRequestId, DeliveryOptionsRequest deliveryOptionsRequest) {
		CartOrder cart = CartOrder.builder()
				.items(items)
				.destination(CartDestination.builder()
						.zipcode(zipcode)
						.build())
				.id(deliveryOptionsRequestId)
				.build();

		FulfillmentRequest fulfillmentRequest = new FulfillmentRequest();
		fulfillmentRequest.setCartOrder(cart);
		fulfillmentRequest.setDeliveryOptionsRequest(deliveryOptionsRequest);
		fulfillmentRequest.setCompanyId(companyId);
		fulfillmentRequest.setXApplicationName("[unit-test]");
		fulfillmentRequest.setQuoteSettings(QuoteSettings.builder().build());

		return fulfillmentRequest;
	}

	public static QuoteResponseV1 buildQuoteResponse(Long quoteId, int estimate) {
		return buildQuoteResponse(quoteId, estimate, (quoteId / 2) + "");
	}

	public static QuoteResponseV1 buildQuoteResponseWithFilter(Long quoteId, int estimate, ShippingMethodEnum shippingMethodEnum) {
		QuoteResponseV1 quoteResponseV1 = buildQuoteResponse(quoteId, estimate, (quoteId / 2) + "");
		List<QuoteDeliveryOptionsResponseV1> deliveryOptionsResponse = quoteResponseV1.getContent().getDeliveryOptions()
				.stream()
				.filter(i -> !i.getDeliveryMethodType().equals(shippingMethodEnum.getValues().get(0)))
				.collect(Collectors.toList());

		return QuoteResponseV1.builder()
				.content(QuoteContentResponseV1.builder()
						.id(quoteResponseV1.getContent().getId())
						.originZipCode(quoteResponseV1.getContent().getOriginZipCode())
						.destinationZipCode(quoteResponseV1.getContent().getDestinationZipCode())
						.deliveryOptions(deliveryOptionsResponse)
						.build())
				.build();
	}

	public static QuoteResponseV1 buildQuoteResponse(Long quoteId, int estimate, String destination) {
		return QuoteResponseV1.builder()
				.content(QuoteContentResponseV1.builder()
						.id(quoteId)
						.originZipCode(quoteId + "")
						.destinationZipCode(destination)
						.deliveryOptions(Arrays.asList(
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("STANDARD")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 3)
										.logisticProviderName("JadLog STANDARD")
										.finalShippingCost(5.0)
										.providerShippingCost(5.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("STANDARD")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 2)
										.logisticProviderName("JadLog STANDARD")
										.finalShippingCost(7.0)
										.providerShippingCost(7.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("PICKUP")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 2)
										.logisticProviderName("JadLog Pickup")
										.finalShippingCost(5.0)
										.providerShippingCost(5.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("PRIORITY")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 20)
										.logisticProviderName("JadLog Scheduled")
										.finalShippingCost(5.0)
										.providerShippingCost(5.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("EXPRESS")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate)
										.logisticProviderName("JadLog Express")
										.finalShippingCost(15.0)
										.providerShippingCost(15.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("EXPRESS")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 1)
										.logisticProviderName("JadLog Express")
										.finalShippingCost(13.0)
										.providerShippingCost(13.0)
										.build()
						))
						.build())
				.build();
	}

	public static QuoteResponseV1 buildQuoteResponseWithProblem(Long quoteId, int estimate, String destination) {
		return QuoteResponseV1.builder()
				.content(QuoteContentResponseV1.builder()
						.id(quoteId)
						.originZipCode(quoteId + "")
						.destinationZipCode(destination)
						.deliveryOptions(Arrays.asList(
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("PICKUP")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 2)
										.logisticProviderName("JadLog Pickup")
										.finalShippingCost(5.0)
										.providerShippingCost(5.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("PRIORITY")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 20)
										.logisticProviderName("JadLog Scheduled")
										.finalShippingCost(5.0)
										.providerShippingCost(5.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("EXPRESS")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate)
										.logisticProviderName("JadLog Express")
										.finalShippingCost(15.0)
										.providerShippingCost(15.0)
										.build(),
								QuoteDeliveryOptionsResponseV1.builder()
										.deliveryMethodType("EXPRESS")
										.deliveryMethodId(quoteId != null ? quoteId.intValue() : null)
										.deliveryEstimateBusinessDays(estimate + 1)
										.logisticProviderName("JadLog Express")
										.finalShippingCost(13.0)
										.providerShippingCost(13.0)
										.build()
						))
						.build())
				.build();
	}

	public static List<LocationStockV1Response> buildStockResponse(List<CartItem> items, List<BranchOfficeEntity> branches, Map<String, int[]> quantityMap) {
		List<LocationStockV1Response> list = new ArrayList<>();

		for (BranchOfficeEntity branch : branches) {
			List<LocationStockItemV1Response> locationItems = new ArrayList<>();

			for (int i = 0; i < items.size(); i++) {
				if (quantityMap.get(branch.getBranchOfficeId()).length > i)
					locationItems.add(LocationStockItemV1Response.builder()
							.sku(items.get(i).getSku())
							.blocked(quantityMap.get(branch.getBranchOfficeId())[i] < 0)
							.amountSaleable(quantityMap.get(branch.getBranchOfficeId())[i] < 0 ? quantityMap.get(branch.getBranchOfficeId())[i] * -1 : quantityMap.get(branch.getBranchOfficeId())[i])
							.build());
			}

			LocationStockV1Response location = LocationStockV1Response.builder()
					.branchOfficeId(branch.getBranchOfficeId())
					.branchOfficeStatus("OK")
					.items(locationItems)
					.build();

			list.add(location);
		}

		return list;
	}

	public static LocationStockV1Response buildStockResponseItem(List<CartItem> items, BranchOfficeEntity branch, Map<String, int[]> quantityMap) {

			List<LocationStockItemV1Response> locationItems = new ArrayList<>();

			for (int i = 0; i < items.size(); i++) {
				if (quantityMap.get(branch.getBranchOfficeId()).length > i)
					locationItems.add(LocationStockItemV1Response.builder()
							.sku(items.get(i).getSku())
							.blocked(quantityMap.get(branch.getBranchOfficeId())[i] < 0)
							.amountSaleable(quantityMap.get(branch.getBranchOfficeId())[i] < 0 ? quantityMap.get(branch.getBranchOfficeId())[i] * -1 : quantityMap.get(branch.getBranchOfficeId())[i])
							.build());
			}

			LocationStockV1Response location = LocationStockV1Response.builder()
					.branchOfficeId(branch.getBranchOfficeId())
					.branchOfficeStatus("OK")
					.items(locationItems)
					.build();

			return location;
	}

	public static BranchOfficeEntity addGeolocation(BranchOfficeEntity buildBranchOffice, int i, int j) {
		buildBranchOffice.setLatitude(i + "");
		buildBranchOffice.setLongitude(j + "");
		return buildBranchOffice;
	}

	public static QuoteRequestV1 buildQuoteRequest(String origin, String destination, List<CartItem> items) {
		return QuoteRequestV1.builder()
				.destinationZipCode(destination)
				.originZipCode(origin)
				.quotingMode("DYNAMIC_BOX_ALL_ITEMS")
				.products(buildProductList(items))
				.build();
	}

	private static List<QuoteProductsRequestV1> buildProductList(List<? extends CartItem> cartItems) {
		List<QuoteProductsRequestV1> products = new ArrayList<>();

		cartItems.forEach(item -> products.add(
				QuoteProductsRequestV1.builder()
						.skuId(item.getSku())
						.costOfGoods(item.getCostOfGoods())
						.height(item.getHeight())
						.length(item.getLength())
						.productCategory(item.getProductCategory())
						.quantity(item.getQuantity())
						.weight(item.getWeight())
						.width(item.getWidth())
						.build()
				)
		);

		return products;
	}

	public static DeliveryOptionsReturn buildDeliveryOptionsReturn() {
		return DeliveryOptionsReturn.builder()
				.build();
	}

	public static DeliveryOptionsRequest withResult(DeliveryOptionsRequest deliveryOptionsRequest,
													DeliveryOptionsReturn deliveryOptionsReturn) {
		deliveryOptionsRequest.setResponse(deliveryOptionsReturn);

		Map<String, List<CartItem>> finalItemBranchMap = new HashMap<>();
		for (DeliveryOption option : deliveryOptionsReturn.getDeliveryOptions()) {
			if (option.getDeliveryModesVerbose() != null && !option.getDeliveryModesVerbose().isEmpty()) {
				DeliveryMode mode = option.getDeliveryModesVerbose().get(0);
				List<CartItem> cartItemList = finalItemBranchMap.computeIfAbsent(mode.getOriginBranchOfficeId(), k -> new ArrayList<CartItem>());
				Optional<CartItem> item = deliveryOptionsRequest.getShoppingCart().getItems().stream().filter(i -> i.getSku().equals(option.getSku())).findFirst();

				item.ifPresent(cartItemList::add);
			}
		}

		deliveryOptionsRequest.setFinalItemBranchMap(finalItemBranchMap);
		return deliveryOptionsRequest;
	}

	public static PickupOptionsRequest withResult(PickupOptionsRequest buildPickupOptionsRequest, PickupOptionsReturn response, List<String> skus) {
		buildPickupOptionsRequest.setResponse(response);
		buildPickupOptionsRequest.setSkus(skus);
		return buildPickupOptionsRequest;
	}

	public static QuotationDTO buildQuote(Map<String, List<CartItem>> itemListMap, Map<String, PickupOptionsReturn> pickupMap, Long quoteId, Integer estimate, boolean isPreSale) {
		return QuotationDTO.builder()
				.itemListMap(itemListMap)
				.quoteMap(itemListMap.keySet().stream()
						.filter(key -> !key.equals("0"))
						.map(key -> Map.entry(key, BuildHelper.buildQuoteResponse(quoteId, estimate)))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
				)
				.pickupOptionsReturnMap(pickupMap)
				.preSale(isPreSale)
				.build();
	}

}
