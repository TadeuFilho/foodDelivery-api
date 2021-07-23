package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ItemFreightProperties {
	private Double costOfGoods;
	private String productCategory;
	private Double weight;
	private Double width;
	private Double height;
	private Double length;
	private Boolean canGroup;

}
