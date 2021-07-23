package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsStockTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TimeUnityEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "quoteDeliveryOption")
@EqualsAndHashCode
public class PickupOption  {
	private String deliveryModeId;
	private String branchId;
	private String name;
	private BranchTypeEnum branchType;
	private String fulfillmentMethod;
	private Double distance;
	private String deliveryTime;
	private Integer deliveryEstimateBusinessDays;
	private TimeUnityEnum deliveryTimeUnit;
	private DeliveryOptionsStockTypeEnum stockType;
	private String state;
	
	@JsonIgnore
	private Long quotationId;
	
	@JsonIgnore
	private String deliveryMethodId;
	
	@JsonIgnore
	private String originBranchOfficeId;
	
	@JsonIgnore
	private QuoteDeliveryOptionsResponseV1 quoteDeliveryOption;
}
