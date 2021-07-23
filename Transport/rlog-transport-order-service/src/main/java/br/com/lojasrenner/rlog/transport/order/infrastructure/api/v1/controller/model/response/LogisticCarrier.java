package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticCarrier {
	
	private String provider;
	private Long quotationId;
	private Integer deliveryMethodId;
	private boolean isPickupEnabled;
	private boolean isSchedulingEnabled;
	private String description;
	private String deliveryMethodType;
	
	
}
