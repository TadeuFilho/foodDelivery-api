package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class QuoteAdditionalInformationRequestV1 {
	@JsonProperty("sales_channel")
	private String salesChannel;
	@JsonProperty("lead_time_business_days")
	private Double leadTimeBusinessDays;
	@JsonProperty("client_type")
	private String clientType;
}
