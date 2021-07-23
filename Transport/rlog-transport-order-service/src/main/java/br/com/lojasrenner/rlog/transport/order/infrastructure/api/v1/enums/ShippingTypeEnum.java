package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum ShippingTypeEnum {
	SHIPPING("SHIPPING"),
	PICKUP("PICKUP");
	
	private final String value;

	ShippingTypeEnum(String value) {
		this.value = value;
	}
	
	public String value() {
		return value;
	}

	@Override
	public String toString() {
		return name();
	}

	public static ShippingTypeEnum fromValue(String text) {
		for (ShippingTypeEnum b : ShippingTypeEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
