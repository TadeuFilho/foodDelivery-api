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
public class AvailabilityFulfillMetricsProperties {
	private String companyId;
	private String channel;
	private DeliveryOptionsAvailabilityEnum availability;
	private MetricsOriginsEnum origin;
	private String reason;
	private String branchId;
	private String skusWithProblems;
	private Boolean fulfillmentConditionsHasChanged;
	private DeliveryOptionsOriginTypeEnum originType;
	private DeliveryOptionsStockTypeEnum stockType;
	private ShippingMethodEnum shippingMethod;
}
