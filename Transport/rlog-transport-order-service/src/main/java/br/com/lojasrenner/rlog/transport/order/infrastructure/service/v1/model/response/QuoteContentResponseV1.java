package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(value = AccessLevel.PACKAGE)
@ToString(exclude = "deliveryOptions")
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteContentResponseV1 {

	private Long id;
	@JsonProperty("quoting_mode")
	private String quotingMode;
	@JsonProperty("client_id")
	private Long clientId;
	
	@JsonProperty("origin_zip_code")
	private String originZipCode;
	@JsonProperty("destination_zip_code")
	private String destinationZipCode;

	@JsonProperty("base_date")
	private Long baseDate;
	@JsonProperty("base_date_iso")
	private String baseDateIso;
	private Long result;
	@JsonProperty("result_iso")
	private String resultIso;
	private Long days;

	@JsonProperty("delivery_options")
	private List<QuoteDeliveryOptionsResponseV1> deliveryOptions;
	private QuoteIdentificationResponseV1 identification;
	@JsonProperty("additional_information")
	private QuoteAdditionalInformationResponseV1 additionalInformation;
	private List<QuoteVolumesResponseV1> volumes;


}