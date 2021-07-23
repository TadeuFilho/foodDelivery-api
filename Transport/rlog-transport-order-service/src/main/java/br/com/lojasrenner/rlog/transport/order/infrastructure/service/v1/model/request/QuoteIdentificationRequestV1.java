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
public class QuoteIdentificationRequestV1 {
	private String session;
	private String ip;
	@JsonProperty("page_name")
	private String pageName;
	private String url;
}