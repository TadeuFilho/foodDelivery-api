package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class QuoteProductsRequestV1 {
	private Double weight;
	@JsonProperty("cost_of_goods")
	private Double costOfGoods;
	private Double width;
	private Double height;
	private Double length;
	private Integer quantity;
	@JsonProperty("sku_id")
	private String skuId;
	@JsonProperty("productCategory")
	private String productCategory;
}
