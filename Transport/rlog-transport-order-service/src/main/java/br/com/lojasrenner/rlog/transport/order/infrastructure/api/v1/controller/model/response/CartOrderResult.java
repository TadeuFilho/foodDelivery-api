package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsOriginTypeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CartOrderResult {
	private String id;
	private DeliveryGroup fulfillmentInfo;
	private boolean fulfillmentConditionsHasChanged;
	private DeliveryOptionsOriginTypeEnum originType;
}
