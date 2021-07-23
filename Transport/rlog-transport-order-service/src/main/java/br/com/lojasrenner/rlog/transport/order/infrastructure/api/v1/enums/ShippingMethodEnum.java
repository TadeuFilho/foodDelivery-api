package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tipo de shipping (retira em loja, normal, expressa, agendada, same day, etc).
 */
public enum ShippingMethodEnum {
	
	STANDARD(ShippingTypeEnum.SHIPPING, 1, "STANDARD"),
	EXPRESS(ShippingTypeEnum.SHIPPING, 2, "EXPRESS"),
	PICKUP(ShippingTypeEnum.PICKUP, 3, "PICKUP"),
	SCHEDULED(ShippingTypeEnum.SHIPPING, 4, "SCHEDULED", "PRIORITY"),
	LOCKER(ShippingTypeEnum.PICKUP, 3, "PICKUP_LOCKER");

	private final List<String> values;
	private final ShippingTypeEnum shippingType;
	private final int order;

	ShippingMethodEnum(ShippingTypeEnum shippingType, int order, String... values) {
		this.values = new ArrayList<>();
		Collections.addAll(this.values, values);
		this.shippingType = shippingType;
		this.order = order;
	}
	
	public List<String> getValues() {
		return values;
	}
	
	public ShippingTypeEnum getShippingType() {
		return shippingType;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public String toString() {
		return name();
	}

	public static ShippingMethodEnum fromValue(String text) {
		for (ShippingMethodEnum b : ShippingMethodEnum.values()) {
			if (b.values.contains(text)) {
				return b;
			}
		}
		return null;
	}
}
