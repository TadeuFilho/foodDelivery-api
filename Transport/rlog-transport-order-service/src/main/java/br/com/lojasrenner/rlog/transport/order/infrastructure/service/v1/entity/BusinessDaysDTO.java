package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteBusinessDaysResponseV1;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BusinessDaysDTO {
	private String companyId;
	private String originZipCode;
	private String destinationZipCode;
	private int businessDays;
	private QuoteBusinessDaysResponseV1 response;
}
