package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class GetQuotationRequest extends BrokerRequest<DeliveryOptionsReturn> {

	@Indexed
	private String deliveryOptionsId;
	
}
