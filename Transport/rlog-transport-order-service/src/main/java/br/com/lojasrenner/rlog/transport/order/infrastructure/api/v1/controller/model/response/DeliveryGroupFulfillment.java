package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.util.List;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsStockTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryGroupFulfillment {
	@ApiModelProperty(value = "Id da cotação retornado pelo sistema de cotação e frete (Ex: Intelipost).")
	private String extQuotationId;

	@ApiModelProperty(value = "Id da modal de entrega retornado pelo sistema de cotação e frete (Ex: Intelipost).")
	private String extDeliveryMethodId;

	@ApiModelProperty(example = "170", value = "Branch (loja, cd, hub, etc) de qual estoque o item deverá ser atendido e realizado o fulfillment (picking, packing) deste item nesta quantidade.")
	private String originBranchId;

	private String extProvider;

	private String extDescription;

	private String estimatedDeliveryTimeValue;

	private String estimatedDeliveryTimeUnit;

	private String estimatedDeliveryDate;

	private ShippingMethodEnum extDeliveryMethodType;

	private String fulfillmentMethod;

	private Boolean unavailable;

	private Double freightCost;

	private String freightCostCurrency;

	private DeliveryOptionsStockTypeEnum stockType;

	private List<ItemFulfillment> items;

	public boolean isUnavailable() {
		return unavailable != null && unavailable;
	}
}
