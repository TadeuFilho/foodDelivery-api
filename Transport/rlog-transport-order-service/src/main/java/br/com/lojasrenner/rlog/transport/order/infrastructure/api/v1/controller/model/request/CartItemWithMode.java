package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CartItemWithMode extends CartItem {
	private String modalId;
	private CartItem cartItem;

	@JsonIgnore
	private DeliveryMode deliveryMode;

	public CartItemWithMode(
			String sku,
			String modal,
			Integer branchOfficeId,
			CartItem cartItem,
			DeliveryMode deliveryMode
	) {
		this.setSku(sku);
		this.setModalId(modal);
		this.setBranchOfficeId(branchOfficeId);
		this.setCartItem(cartItem);
		this.setDeliveryMode(deliveryMode);
	}
}
