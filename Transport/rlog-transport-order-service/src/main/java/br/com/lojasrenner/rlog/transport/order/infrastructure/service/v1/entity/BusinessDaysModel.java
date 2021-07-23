package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class BusinessDaysModel {
	private String companyId;
	private String originZipCode;
	private String destinationZipCode;
	private int businessDays;
	
	public String getKey() {
		return String.join("+", companyId, originZipCode, destinationZipCode, businessDays + "");
	}
}
