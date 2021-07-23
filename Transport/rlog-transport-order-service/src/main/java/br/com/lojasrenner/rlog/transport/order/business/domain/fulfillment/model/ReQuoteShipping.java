package br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.model;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsOriginTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReQuoteShipping {

	private DeliveryOptionsRequest deliveryOptionsRequest;
	private Map<String, List<CartItemWithMode>> items;
	private DeliveryOptionsOriginTypeEnum deliveryOptionsOriginType;

}
