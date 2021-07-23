package br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment;

import br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.strategy.DeliveryOptionsOriginTypeStrategy;
import br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.strategy.DeliveryOptionsOriginTypeStrategys;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.DeliveryOptionsOriginTypeEnum;

public enum DeliveryOptionsOriginTypeStrategyEnum {

	OK_SAME_ORIGIN(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN, DeliveryOptionsOriginTypeStrategys.getOkSameOriginStrategy()),
	OK_NEW_ORIGIN(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN, DeliveryOptionsOriginTypeStrategys.getOkNewOriginStrategy()),
	PARTIAL_SAME_ORIGIN(DeliveryOptionsOriginTypeEnum.PARTIAL_SAME_ORIGIN, DeliveryOptionsOriginTypeStrategys.getPartialOriginStrategy()),
	PARTIAL_NEW_ORIGIN(DeliveryOptionsOriginTypeEnum.PARTIAL_NEW_ORIGIN, DeliveryOptionsOriginTypeStrategys.getPartialOriginStrategy()),
	PARTIAL_NO_ORIGIN(DeliveryOptionsOriginTypeEnum.NO_ORIGIN, DeliveryOptionsOriginTypeStrategys.getPartialNoOriginStrategy()),
	NO_ORIGIN(DeliveryOptionsOriginTypeEnum.NO_ORIGIN, DeliveryOptionsOriginTypeStrategys.getPartialNoOriginStrategy());

	private DeliveryOptionsOriginTypeEnum origin;
	private DeliveryOptionsOriginTypeStrategy strategy;

	DeliveryOptionsOriginTypeStrategyEnum(DeliveryOptionsOriginTypeEnum deliveryOptionsOriginType, DeliveryOptionsOriginTypeStrategy strategy) {
		this.origin = deliveryOptionsOriginType;
		this.strategy = strategy;
	}

	public DeliveryOptionsOriginTypeEnum getOrigin() {
		return origin;
	}

	public DeliveryOptionsOriginTypeStrategy getStrategy() {
		return strategy;
	}
}
