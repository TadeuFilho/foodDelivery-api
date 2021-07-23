package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.util.List;
import java.util.Map;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingToResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteResponseV1;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document("pickupOptionsRequest")
@Getter
@Setter
@ToString(exclude = { "quoteMap", "stockResponse", "activeBranchOffices", "geolocationResponse" })
@EqualsAndHashCode(callSuper = false)
public class PickupOptionsRequest extends BrokerRequest<PickupOptionsReturn> {

	@Indexed
	private String deliveryOptionsId;
	
	private String state;
	private String zipcode;
	private List<String> skus;
	
	private List<BranchOfficeEntity> activeBranchOffices;
	private List<CartItem> requestedCartItems;
	private List<LocationStockV1Response> stockResponse;
	private List<String> branchesWithStock;
	private Map<String, QuoteResponseV1> quoteMap;
	private Map<String, QuoteResponseV1> shippingToQuoteMap;
	private List<GeoLocationResponseV1> geolocationResponse;
	private List<ShippingToResponseV1> shippingToResponse;
	private DeliveryRequestStatistics statistics = new DeliveryRequestStatistics();

	private Boolean preSale;

	public boolean isPreSale() {
		return preSale != null && preSale.booleanValue();
	}

}
