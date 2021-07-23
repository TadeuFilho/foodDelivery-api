package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum ProductTypeEnum {
	DEFAULT("DEFAULT"),
	GIFT("GIFT"),
	GIFT_GENERATOR("GIFT_GENERATOR"),
	GIFT_INVALID("GIFT_INVALID"),
	KIT("KIT");

	private final String value;

	ProductTypeEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static ProductTypeEnum fromValue(String text) {
		for (ProductTypeEnum b : ProductTypeEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return DEFAULT;
	}

	public static Boolean isGift(ProductTypeEnum productTypeEnum) {
		return productTypeEnum != null && (productTypeEnum.equals(GIFT) || productTypeEnum.equals(GIFT_GENERATOR));
	}
}
