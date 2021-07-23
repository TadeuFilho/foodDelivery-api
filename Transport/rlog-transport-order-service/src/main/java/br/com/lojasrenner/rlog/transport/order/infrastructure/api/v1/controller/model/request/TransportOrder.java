package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
public class TransportOrder {
	  
	  private PartnerPlataform partnerPlatform;
	  private Customer customer;
	  private List<OrderVolume> orderVolume;

	  private String orderType;
	  private String orderSubType;
	  private String orderNumber;
	  private String salesOrderNumber;
	  private String originZipCode;
	  private LocalDateTime estimatedDeliveryDate;
	  private BigDecimal providerShippingCosts;
	  private BigDecimal customerShippingCosts;
	  private String salesChannel;
	  private Boolean scheduled;
	 
	  
	  
	  
	  
	  
//	  private CartDestination destination;
//	  private ExtraIdentification extraIdentification;
//	  private boolean containsRestrictedOriginItems;

//	public TransportOrder(TransportOrder cart) {
//		List<CartItem> cartItems = new ArrayList<>();
//		cartItems.addAll(cart.getItems().stream().map(CartItem::new).collect(Collectors.toList()));
//		this.items = cartItems;
//		this.destination = cart.destination;
//		this.extraIdentification = cart.extraIdentification;
//		this.containsRestrictedOriginItems = cart.containsRestrictedOriginItems;
//	}
}
