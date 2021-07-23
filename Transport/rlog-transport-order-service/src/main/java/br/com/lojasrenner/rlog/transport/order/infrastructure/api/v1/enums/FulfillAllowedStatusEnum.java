package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum FulfillAllowedStatusEnum {
	NONE("NONE"),
	SAME_ORIGIN("SAME_ORIGIN"),
	REQUOTE_ORIGIN("REQUOTE_ORIGIN"),
	EVERY_ORIGIN("EVERY_ORIGIN");

	private final String value;

	FulfillAllowedStatusEnum(String value) { this.value = value; }

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static FulfillAllowedStatusEnum fromValue(String text) {
		for (FulfillAllowedStatusEnum b : FulfillAllowedStatusEnum.values()) {
			if (String.valueOf(b.value).equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}

}
