package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TimeUnityEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DeliveryMode {
	private String id;
	private String modalId;
	private String displayName;
	private Boolean isRecommendation;
	private String estimatedDeliveryTimeValue;
	private TimeUnityEnum estimatedDeliveryTimeUnit;
	private String estimatedDeliveryDate;
	private ShippingMethodEnum shippingMethod;
	private String fulfillmentMethod;
	private Double freightCost;
	private String state;
	@JsonIgnore
	private Double providerShippingCost;
	private String freightCostCurrency;
	private String description;
	private LogisticCarrier logisticCarrierInfo;
	
	@JsonIgnore
	private Integer deliveryEstimateBusinessDays;
	
	@JsonIgnore
	private String branchOfficeId;
	
	@JsonIgnore
	private String originBranchOfficeId;
	
	@JsonIgnore
	private int distance;
	
	@JsonIgnore
	private String origin;
	
	@JsonIgnore
	private String destination;
	
	@JsonIgnore
	private Integer deliveryMethodId;
	
	@JsonIgnore
	private Long quotationId;
	
	@JsonIgnore
	private String key;
	
	public static String generateModalId(String fulfillmentMethodEnum, ShippingMethodEnum shippingMethod, Integer deliveryEstimateBusinessDays, Double finalShippingCost, String branchOrigin) {
		return (fulfillmentMethodEnum + "-" +
				shippingMethod + "-" +
				deliveryEstimateBusinessDays + "-" +
				((finalShippingCost == null) ? "0" : Math.round(finalShippingCost * 100))).toUpperCase() + "-" +
				branchOrigin;
	}

}
