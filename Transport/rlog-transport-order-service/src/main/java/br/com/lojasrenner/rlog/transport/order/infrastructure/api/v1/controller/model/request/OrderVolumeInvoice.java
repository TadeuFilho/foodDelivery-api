package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class OrderVolumeInvoice {

	private String invoiceSeries;
	private String invoiceNumber;
	private String invoiceKey;
	private LocalDateTime invoiceDate;
	private BigDecimal invoiceTotalValue;
	private BigDecimal invoiceProductsValue;
	
}
