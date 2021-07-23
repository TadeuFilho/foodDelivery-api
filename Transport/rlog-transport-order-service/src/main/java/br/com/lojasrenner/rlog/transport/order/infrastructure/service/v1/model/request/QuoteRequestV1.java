package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"quotingMode"})
public class QuoteRequestV1 {
	@JsonProperty("origin_zip_code")
	private String originZipCode;
	@JsonProperty("destination_zip_code")
	private String destinationZipCode;
	@JsonProperty("quoting_mode")
	private String quotingMode;
	private List<QuoteProductsRequestV1> products;
	@JsonProperty("additional_information")
	private QuoteAdditionalInformationRequestV1 additionalInformation;
	private QuoteIdentificationRequestV1 identification;
}
