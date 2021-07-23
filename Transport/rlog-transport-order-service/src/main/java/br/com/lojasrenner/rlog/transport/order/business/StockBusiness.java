package br.com.lojasrenner.rlog.transport.order.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.business.util.QueryUtil;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchSortingEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.BrokerRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.StockServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LocationStockV1Request;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response.BranchOfficeStatus;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;

@Component
public class StockBusiness {

	@Autowired
	private StockServiceV1 stockService;

	@Autowired
	private LiveConfig config;

	public ResponseEntity<List<LocationStockV1Response>> findStoreWithStock(
			String companyId,
			String channel,
			List<? extends CartItem> items,
			List<BranchOfficeEntity> branchOffices
	) {
		return findStoreWithStockWithString(companyId, channel, items, branchOffices.stream()
				.map(BranchOfficeEntity::getBranchOfficeId)
				.collect(Collectors.toList()));
	}

	public ResponseEntity<List<LocationStockV1Response>> findStoreWithStockWithString(
			String companyId,
			String channel,
			List<? extends CartItem> items,
			List<String> branchOffices
	) {
		List<String> skus = items.stream().map(CartItem::getSku).collect(Collectors.toList());

		return stockService.postLocationStock(companyId, channel, LocationStockV1Request.builder()
				.branchesOfficeId(branchOffices)
				.skus(skus)
				.build());
	}

	public LocationStockV1Response findBestLocation(
			List<? extends CartItem> items,
			List<LocationStockV1Response> stockResponse,
			List<String> activeBranchOfficeList,
			List<GeoLocationResponseV1> geoLocationBranches,
			BrokerRequest<?> request
	) {

		List<LocationStockV1Response> filteredLocations = filterAndSortLocations(items, stockResponse, activeBranchOfficeList, geoLocationBranches, request);

		if (filteredLocations.isEmpty())
			return null;

		return filteredLocations.get(0);
	}

	public List<LocationStockV1Response> filterAndSortLocations(
			List<? extends CartItem> items,
			List<LocationStockV1Response> stockResponse,
			List<String> activeBranchOfficeIds,
			List<GeoLocationResponseV1> geoLocationBranches,
			BrokerRequest<?> request
	) {
		List<LocationStockV1Response> filteredLocations = stockResponse.stream()
				.filter(f ->
						// branch office tem que estar ativo no Branch Service
						activeBranchOfficeIds.containsAll(Arrays.asList(f.getBranchOfficeId().split("\\+")))
								// tem que estar OK no stock service
								&& f.getBranchOfficeStatus().equals(BranchOfficeStatus.OK.name()))
				.collect(Collectors.toList());

		if (filteredLocations.size() > 1) {
			filteredLocations.sort(this::sortByAbsoluteAmountSaleableSum);
			Map<String, Integer> skuQuantityMap = items.stream()
					.collect(Collectors.toMap(CartItem::getSku, CartItem::getQuantity));

			filteredLocations = filteredLocations.stream()
					.sorted((a, b) -> sortByDistance(geoLocationBranches, a, b))
					.sorted((a, b) -> (
							BranchSortingEnum.fromValue(config.getConfigValueString(request.getCompanyId(), Optional.ofNullable(request.getXApplicationName()), CompanyConfigEntity::getBranchSorting, false)) == BranchSortingEnum.COST) ?
							(int) sortByMonetaryValue(items, a, b) :
							sortByMostSkusFullyAvailable(skuQuantityMap, a, b)
					)
					.sorted((a, b) -> sortByStoreWithCompleteItems(items, a, b))
					.collect(Collectors.toList());
		}


		return filteredLocations;
	}

	private int sortByStoreWithCompleteItems(List<? extends CartItem> items, LocationStockV1Response a, LocationStockV1Response b){
		return checkAllItens(b.getItems(), items) - checkAllItens(a.getItems(), items);
	}

	private double sortByMonetaryValue(List<? extends CartItem> items, LocationStockV1Response a, LocationStockV1Response b) {
		// b - a: ordena do maior para o menor
		return getMonetaryValue(items, b)*100 - getMonetaryValue(items, a)*100;
	}

	private static Double getMonetaryValue(List<? extends CartItem> items, LocationStockV1Response location) {
		return location.getItems()
				.stream()
				.filter(i -> !i.isBlocked() && i.getAmountSaleable() >= getQuantityFromItem(items, i.getSku()))
				.mapToDouble(i -> getValueFromItem(items, i.getSku()))
				.sum();
	}

	private static Integer getQuantityFromItem(List<? extends CartItem> items, String sku) {
		Optional<? extends CartItem> findFirst = items.stream().filter(i -> i.getSku().equals(sku)).findFirst();

		if (findFirst.isEmpty())
			return Integer.MAX_VALUE;

		return findFirst.get().getQuantity();
	}

	private static Double getValueFromItem(List<? extends CartItem> items, String sku){
		return items.stream().filter(i -> i.getSku().equals(sku)).mapToDouble(i -> i.getQuantity() * i.getCostOfGoods()).sum();
	}

	private int sortByMostSkusFullyAvailable(Map<String, Integer> skuQuantityMap, LocationStockV1Response a, LocationStockV1Response b) {
		// b - a: ordena do maior para o menor
		return getSkuAvailableCount(skuQuantityMap, b) - getSkuAvailableCount(skuQuantityMap, a);
	}

	private int getSkuAvailableCount(Map<String, Integer> skuQuantityMap, LocationStockV1Response location) {
		return location.getItems()
				.stream()
				.mapToInt(i -> hasAmountSaleableGreaterThanRequested(skuQuantityMap, i) ? 1 : 0)
				.sum();
	}

	private boolean hasAmountSaleableGreaterThanRequested(Map<String, Integer> skuQuantityMap, LocationStockItemV1Response item) {
		int amountSaleable = 0;
		String sku = item.getSku();

		if (item.isBlocked())
			return false;

		if (item.getAmountSaleable() != null)
			amountSaleable = item.getAmountSaleable();

		Integer optionalValue = skuQuantityMap.get(sku);

		int amountRequested = 0;

		if (optionalValue != null)
			amountRequested  = optionalValue;
		else
			return false;

		return amountSaleable >= amountRequested;
	}

	//ordenação de lojas
	private int sortByAbsoluteAmountSaleableSum(LocationStockV1Response a, LocationStockV1Response b) {
		// b - a: ordena do maior para o menor
		return getAmountSaleableSum(b) - getAmountSaleableSum(a);
	}

	private int sortByDistance(List<GeoLocationResponseV1> branchs, LocationStockV1Response a, LocationStockV1Response b){
		return getDistance(a, branchs) - getDistance(b, branchs);
	}

	private int getDistance(LocationStockV1Response location, List<GeoLocationResponseV1> branchs){
		Optional<GeoLocationResponseV1> geoStore = branchs.stream().filter(b -> b.getBranchOfficeId().equals(location.getBranchOfficeId())).findFirst();
		return geoStore.map(GeoLocationResponseV1::getDistance).orElse(Integer.MAX_VALUE);
	}

	private static int getAmountSaleableSum(LocationStockV1Response location) {
		return location.getItems()
				.stream()
				.mapToInt(i -> i.getAmountSaleable() == null ? 0 : i.getAmountSaleable())
				.sum();
	}

	private int checkAllItens(List<LocationStockItemV1Response> locationItens, List<? extends CartItem> items){
		AtomicInteger hasAllItens = new AtomicInteger(1);
		items.forEach(i -> {
			Optional<LocationStockItemV1Response> location = locationItens.stream()
					.filter(l -> !l.isBlocked() && i.getSku().equals(l.getSku()) && i.getQuantity() <= l.getAmountSaleable())
					.findFirst();
			if(location.isEmpty()){
				hasAllItens.set(0);
			}
		});
		return hasAllItens.get();
	}

	public List<LocationStockV1Response> overrideStockQuantities(
			List<CartItem> itemsList,
			ResponseEntity<List<LocationStockV1Response>> stockResponse,
			List<BranchOfficeEntity> branchOffices,
			BranchOfficeEntity ecommBranchOffice
	) {
		List<LocationStockV1Response> resultList = new ArrayList<>();

		if (stockResponse != null && stockResponse.getStatusCode().is2xxSuccessful() && stockResponse.getBody() != null)
			resultList.addAll(stockResponse.getBody());

		List<String> giftItems = itemsList.stream()
				.filter(i -> ProductTypeEnum.isGift(i.getProductType()) && i.getStockStatus() != StockStatusEnum.INOMNISTOCK)
				.map(CartItem::getSku)
				.collect(Collectors.toList());

		List<String> invalidGifts = itemsList.stream()
				.filter(i -> i.getProductType() == ProductTypeEnum.GIFT_INVALID)
				.map(CartItem::getSku)
				.collect(Collectors.toList());

		resultList = resultList.stream()
				.map(l -> {
					if (l.getItems() != null) {
						l.setItems(l.getItems().stream()
								.map(i -> {
									if (invalidGifts.contains(i.getSku())) {
										i.setAmountPhysical(0);
										i.setAmountSaleable(0);
									}
									if (!l.getBranchOfficeId().equals(ecommBranchOffice.getBranchOfficeId()) && giftItems.contains(i.getSku())){
											i.setAmountPhysical(0);
											i.setAmountSaleable(0);
									}

									return i;
								})
								.collect(Collectors.toList()));
					}

					return l;
				}).collect(Collectors.toList());

		//se nao foi solicitado estoque de CD, retorna sem alteração
		if (branchOffices.stream().noneMatch(b -> b.getBranchOfficeId().equals(ecommBranchOffice.getBranchOfficeId())))
			return resultList;

		List<LocationStockV1Response> finalResultList = resultList;
		itemsList.forEach(item -> {
			Boolean stockStatusEnum = StockStatusEnum.isOmniStock(item.getStockStatus()) || invalidGifts.contains(item.getSku());
			Optional<LocationStockV1Response> ecommLocationOptional = finalResultList.stream()
					.filter(l -> l.getBranchOfficeId().equals(ecommBranchOffice.getBranchOfficeId()))
					.findAny();

			LocationStockV1Response ecommLocation = null;

			if (ecommLocationOptional.isEmpty()) {
				ecommLocation = LocationStockV1Response.builder()
						.branchOfficeId(ecommBranchOffice.getBranchOfficeId())
						.branchOfficeStatus("OK")
						.companyId(ecommBranchOffice.getCompanyId())
						.items(new ArrayList<>())
						.build();
				finalResultList.add(ecommLocation);
			} else {
				ecommLocation = ecommLocationOptional.get();
			}

			Optional<LocationStockItemV1Response> locationItemOptional = Optional.empty();

			if (ecommLocation.getItems() != null)
				locationItemOptional = ecommLocation.getItems().stream().filter(i -> i.getSku().equals(item.getSku())).findAny();

			LocationStockItemV1Response newItem = LocationStockItemV1Response.builder()
					.amountPhysical(stockStatusEnum.booleanValue() ? 0 : 9999)
					.amountSaleable(stockStatusEnum.booleanValue() ? 0 : 9999)
					.blocked(stockStatusEnum)
					.sku(item.getSku())
					.build();

			ArrayList<LocationStockItemV1Response> newItems = new ArrayList<>();

			if (ecommLocation.getItems() != null)
				newItems.addAll(ecommLocation.getItems());

			ecommLocation.setItems(newItems);

			if (locationItemOptional.isPresent())
				ecommLocation.getItems().remove(locationItemOptional.get());

			ecommLocation.getItems().add(newItem);
		});

		return resultList;
	}

	public List<LocationStockV1Response> prepareStockResponse(
			List<LocationStockV1Response> stockResponse,
			DeliveryOptionsRequest deliveryRequest,
			List<String> storesInRange,
			Map<String, Integer> skuQuantityMap,
			List<String> eagerBranches
	) {
		LocationStockV1Response emptyLocation = LocationStockV1Response.builder().fingerPrint("").build();
		skuQuantityMap.entrySet().stream().forEach(sku ->
				emptyLocation.setFingerPrint(emptyLocation.getFingerPrint() + sku.getKey() + ":NOK/")
		);

		Integer negativePositionCounter = (eagerBranches.size() * -1);

		for (LocationStockV1Response entity : stockResponse) {
			entity.setFingerPrint("");
			List<String> okItems = new ArrayList<>();
			int index = storesInRange.indexOf(entity.getBranchOfficeId());

			if (eagerBranches.contains(entity.getBranchOfficeId())) {
				entity.setPositionBasedOnPriority(negativePositionCounter);
				negativePositionCounter++;
			} else {
				entity.setPositionBasedOnPriority(index >= 0 ? index : Integer.MAX_VALUE);
			}

			Integer groupIndex = deliveryRequest.getShippingGroupResponseObject().getShippingGroupResponse()
					.stream()
					.filter(item -> item.getBranches().contains(Integer.parseInt(entity.getBranchOfficeId())))
					.findFirst()
					.map(ShippingGroupResponseV1::getPriority)
					.orElse(Integer.MAX_VALUE);

			entity.setGroupIndex(groupIndex);

			for (Entry<String, Integer> sku : skuQuantityMap.entrySet()) {

				Optional<LocationStockItemV1Response> locationItemOptional = Optional.empty();

				if (entity.getItems() != null)
					locationItemOptional = entity.getItems().stream().filter(i -> sku.getKey().equals(i.getSku())).findFirst();

				if (locationItemOptional.isEmpty() || locationItemOptional.get().isBlocked() || locationItemOptional.get().getAmountSaleable() < sku.getValue()) {
					entity.setFingerPrint(entity.getFingerPrint() + sku.getKey() + ":NOK/");
					continue;
				}

				entity.setFingerPrint(entity.getFingerPrint() + sku.getKey() + ":OK/");
				entity.setOkCount(entity.getOkCount() + 1);
				okItems.add(sku.getKey());
			}
			entity.setOkItems(okItems);
		}

		List<LocationStockV1Response> stockResponseDistinct = stockResponse.stream()
				.sorted(Comparator.comparingInt(LocationStockV1Response::getPositionBasedOnPriority))
				.filter(QueryUtil.distinctByKey(LocationStockV1Response::getFingerPrint))
				.filter(s -> !s.getFingerPrint().equals(emptyLocation.getFingerPrint()))
				.collect(Collectors.toList());

		if (stockResponse.size() != stockResponseDistinct.size())
			deliveryRequest.getStockResponseList().add(stockResponseDistinct);

		List<LocationStockV1Response> stockResponseFiltered = stockResponseDistinct.stream()
				.sorted((a, b) -> b.getOkCount() - a.getOkCount())
				.sorted(Comparator.comparingInt(LocationStockV1Response::getGroupIndex))
				.filter(QueryUtil.distinctStockOverlap(LocationStockV1Response::getOkItems, LocationStockV1Response::getPositionBasedOnPriority))
				.sorted(Comparator.comparingInt(LocationStockV1Response::getPositionBasedOnPriority))
				.collect(Collectors.toList());

		if (stockResponseDistinct.size() != stockResponseFiltered.size())
			deliveryRequest.getStockResponseList().add(stockResponseFiltered);

		return stockResponseFiltered;
	}

	public Integer getAmountSaleableForItem(String sku, LocationStockV1Response v) {
		Optional<LocationStockItemV1Response> optionalItem = v.getItems()
				.stream()
				.filter(i -> i.getSku().equals(sku))
				.findFirst();

		if (optionalItem.isEmpty())
			return 0;

		LocationStockItemV1Response locationStockItemV1Response = optionalItem.get();
		Integer amountSaleable = locationStockItemV1Response.getAmountSaleable();

		if (amountSaleable == null || locationStockItemV1Response.isBlocked())
			return 0;

		return amountSaleable;
	}

}
