package br.com.lojasrenner.rlog.transport.order.business.model;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsStockTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapPickupOptionDto {
	GeoLocationResponseV1 geoResponse;
	BranchOfficeEntity branch;
	FulfillmentMethodEnum fulfillmentMethod;
	int deliveryTime;
	Long quotationId;
	QuoteDeliveryOptionsResponseV1 deliveryMethod;
	PickupOptionsRequest pickupOptionsRequest;
	String bestShippingStore;
	DeliveryOptionsStockTypeEnum stockType;
}
