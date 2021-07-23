package br.com.lojasrenner.rlog.transport.order.metrics.properties;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityQueryMetricsProperties {
	private String companyId;
	private String channel;
	private DeliveryOptionsAvailabilityEnum availability;
	private DeliveryOptionsErrorEnum reason;
	private String destinationCity;
	private String destinationState;
	private String destinationCountry;
	private String destinationZipCode;
	private String originBranch;
	private String originCity;
	private String originState;
	private String originCountry;
	private String originZipCode;
	private String skusWithProblems;
}
