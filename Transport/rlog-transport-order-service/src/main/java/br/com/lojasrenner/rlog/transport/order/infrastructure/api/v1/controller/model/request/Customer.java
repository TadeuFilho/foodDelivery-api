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
public class Customer {

	private String fullName;
	private String firstName;
	private String lastName;
	private String email;
	private String cellphone;
	private Boolean isCompany;
	private String federalTaxPayerId;
	private String shippingAddress;
	private String shippingNumber;
	private String shippingAdditional;
	private String shippingQuarter;
	private String shippingCity;
	private String shippingState;
	private String shippingZipCode;
	private String shippingCountry;
	private String shipmentNote;
	private Notify notify;                          
}
