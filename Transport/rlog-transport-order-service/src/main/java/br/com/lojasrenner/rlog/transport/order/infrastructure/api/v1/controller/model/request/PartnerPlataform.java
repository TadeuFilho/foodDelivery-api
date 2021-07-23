package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

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
public class PartnerPlataform {

	private String name;
	private Long quoteId; // perguntar
	private Integer deliveryMethodId; //perguntar
}
