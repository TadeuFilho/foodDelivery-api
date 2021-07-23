package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ShoppingCart {
	  private List<CartItem> items;
	  private CartDestination destination;
	  private ExtraIdentification extraIdentification;
	  private boolean containsRestrictedOriginItems;

	public ShoppingCart(ShoppingCart cart) {
		List<CartItem> cartItems = new ArrayList<>();
		cartItems.addAll(cart.getItems().stream().map(CartItem::new).collect(Collectors.toList()));
		this.items = cartItems;
		this.destination = cart.destination;
		this.extraIdentification = cart.extraIdentification;
		this.containsRestrictedOriginItems = cart.containsRestrictedOriginItems;
	}
}
