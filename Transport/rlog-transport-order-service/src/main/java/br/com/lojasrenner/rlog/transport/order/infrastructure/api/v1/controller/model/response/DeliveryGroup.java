package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DeliveryGroup {
	private List<DeliveryGroupFulfillment> groups;
}
