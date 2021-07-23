package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document("deliveryOptionsRequest")
@Getter
@Setter
@ToString(exclude = { "shoppingCartOriginal" })
@EqualsAndHashCode(callSuper = true)
public class DeliveryOptionsRequest extends DeliveryRequest<DeliveryOptionsReturn> {

	private ShoppingCart shoppingCart;
	private ShoppingCart shoppingCartOriginal;
	private List<CartItem> excessItems;
	private List<String> usedItems;
	private boolean verbose;
	private boolean logisticInfo;
	private QuoteDeliveryOptionsResponseV1 bestDeliveryOptionForPickup;

	private List<Map<String, Object>> stockRequestInput = new ArrayList<>();

	@Override
	public List<CartItem> getItemsList() {
		return this.shoppingCart.getItems();
	}

	@Override
	public String getDestinationZipcode() {
		return this.shoppingCart.getDestination().getZipcode();
	}

	@Override
	public int getAvailableSkuCount() {
		if (this.getResponse() != null && this.getResponse().getDeliveryOptions() != null)
			return (int) this.getResponse()
					.getDeliveryOptions()
					.stream()
					.filter(d -> d.getDeliveryModes() != null && !d.getDeliveryModes().isEmpty())
					.count();

		return 0;
	}

}
