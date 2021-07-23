package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

import org.springframework.data.annotation.Reference;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsAvailabilityEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, exclude = { "quoteFromEcomm", "statistics" })
public abstract class DeliveryRequest<T> extends BrokerRequest<T> {

	//TODO: isso tem cara de que precisa ser removido, mas é usado em leitura no fulfill. Nao tem set. Sem sentido
	private List<GeoLocationResponseV1> geolocationResponse;

	private ShippingGroupResponseObjectV1 shippingGroupResponseObject;
	private List<String> activeBranchIdsInRange;
	private List<String> storesInRange;
	private List<List<LocationStockV1Response>> stockResponseList = new ArrayList<>();
	private List<List<String>> eagerBranchesList = new ArrayList<>();
	private Map<String, QuoteResponseV1> quoteMap;
	private Map<String, List<CartItem>> itemBranchMap;
	private DeliveryRequestDetails deliveryRequestDetails = new DeliveryRequestDetails();
	//FromPreSale
	private BranchOfficeEntity preSaleBranchOfficeUsed;
	private QuoteResponseV1 quoteFromPreSale;

	private Map<String, QuoteResponseV1> preSaleQuoteMap;
	private Map<String, List<CartItem>> preSaleItemBranchMap;

	@Reference
	private Map<String, PickupOptionsReturn> preSalePickupOptionsReturnMap;

	@Reference
	private Map<String, PickupOptionsReturn> pickupOptionsReturnMap;

	private Map<String, QuoteResponseV1> finalQuoteMap;
	private Map<String, List<CartItem>> finalItemBranchMap;

	@Reference
	private Map<String, PickupOptionsReturn> finalPickupOptionsReturnMap;

	//FromEcomm
	private List<BranchOfficeEntity> ecommBranchOfficeList;
	private QuoteResponseV1 quoteFromEcomm;
	private BranchOfficeEntity ecommBranchOfficeUsed;
	private List<LocationStockV1Response> stockResponseForEcomm;
	private LocationStockV1Response bestLocationForEcomm;
	private Map<String, List<CartItem>> itemBranchMapForEcomm;
	private Map<String, QuoteResponseV1> quoteMapForEcomm;
	private Map<String, PickupOptionsReturn> ecommPickupOptionsReturnMap;

	//Pickup
	private List<String> activeStoresForPickup;
	private List<LocationStockV1Response> stockResponseForPickup;
	private CheckoutInfo checkout;
	//Stats
	private DeliveryRequestStatistics statistics = new DeliveryRequestStatistics();

	public abstract List<CartItem> getItemsList();

	public abstract String getDestinationZipcode();

	public abstract int getAvailableSkuCount();

	public void addQuoteFromStore(String branchId, QuoteResponseV1 quote) {
		if (quoteMap == null)
			quoteMap = new ConcurrentHashMap<>();

		quoteMap.put(branchId, quote);
	}

	public void calculateStatistics() {
		int skuCount = 0;
		int availableSkuCount = 0;

		if (this.getItemsList() != null)
			skuCount = this.getItemsList().size();

		availableSkuCount = getAvailableSkuCount();

		DeliveryOptionsAvailabilityEnum availability;
		if (skuCount == availableSkuCount)
			availability = DeliveryOptionsAvailabilityEnum.AVAILABLE;
		else if (availableSkuCount > 0) {
			availability = DeliveryOptionsAvailabilityEnum.PARTIAL;
		}else {
			availability = DeliveryOptionsAvailabilityEnum.UNAVAILABLE;
		}
		statistics.setSkuCount(skuCount);
		statistics.setAvailableSkuCount(availableSkuCount);
		statistics.setAvailability(availability);

		this.registerFinalTimestamp();
	}
}
