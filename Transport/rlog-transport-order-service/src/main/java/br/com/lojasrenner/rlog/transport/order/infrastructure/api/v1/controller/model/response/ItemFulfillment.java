package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class ItemFulfillment {

	private String sku;

	private Integer quantity;

	@JsonProperty(value = "isOmniStock")
	private Boolean isOmniStock;
}
