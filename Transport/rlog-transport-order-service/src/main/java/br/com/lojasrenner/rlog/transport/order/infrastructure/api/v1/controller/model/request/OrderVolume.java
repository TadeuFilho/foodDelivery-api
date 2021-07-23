package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class OrderVolume {

	private String name;
	private String orderVolumeNumber;
	private String volumeTypeCode; // talvez fosse melhor um enum
	private Double width;
	private Double height;
	private Double lenght;
	private Double weight;
	private String productsNature; // talvez fosse melhor um enum
	private Integer productsQuantity;
	private Boolean isIcmsExempt;

	private OrderVolumeInvoice orderVolumeInvoice;
}
