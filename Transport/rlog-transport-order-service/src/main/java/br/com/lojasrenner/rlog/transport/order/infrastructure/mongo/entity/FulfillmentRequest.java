package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.CartOrderResult;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Document("fulfillmentRequest")
@Getter
@Setter
@ToString(exclude = {"pickupOptionsRequestList", "deliveryOptionsRequest", "bestDeliveryOption"})
@EqualsAndHashCode(callSuper = true)
public class FulfillmentRequest extends DeliveryRequest<CartOrderResult> {

	private CartOrder cartOrder;

	private QuoteDeliveryOptionsResponseV1 bestDeliveryOption;

	//@Indexed
	private String deliveryOptionsRequestId;

	@Reference
	private DeliveryOptionsRequest deliveryOptionsRequest;

	@Reference
	private PickupOptionsRequest pickupOptionsRequest;

	@Reference
	private PickupOption pickupOption;

	@Reference
	private DeliveryOptionsRequest newQuoteDeliveryOptionsRequest;

	@Reference
	private List<PickupOptionsRequest> pickupOptionsRequestList;

	private List<Map<String, List<CartItemWithMode>>> itemMapList = new ArrayList<>();

	private ShippingMethodEnum shippingMethod;

	private String mainBranch;

	private Boolean hasItemOmniStockOnCart;

	@Reference
	private DeliveryOptionsRequest reQuoteDeliveryOptions;
	private Boolean reQuoteDeliveryOptionsMapHasChanged;

	@Reference
	private PickupOptionsRequest reQuotePickupOptionsRequest;
	private Boolean reQuotePickupOptionsMapHasChanged;

	private Boolean autoReQuoteHasChanged;

	@Override
	public List<CartItem> getItemsList() {
		return this.cartOrder.getItems()
				.stream()
				.map(CartItemWithMode::getCartItem)
				.collect(Collectors.toList());
	}

	@Override
	public String getDestinationZipcode() {
		return this.cartOrder.getDestination().getZipcode();
	}

	@Override
	public int getAvailableSkuCount() {
		if (this.getResponse() != null && this.getResponse().getFulfillmentInfo() != null && this.getResponse().getFulfillmentInfo().getGroups() != null)
			return this.getResponse()
					.getFulfillmentInfo()
					.getGroups()
					.stream()
					.mapToInt(g -> {
						if (g.isUnavailable())
							return 0;

						return g.getItems().size();
					})
					.sum();

		return 0;
	}

}
