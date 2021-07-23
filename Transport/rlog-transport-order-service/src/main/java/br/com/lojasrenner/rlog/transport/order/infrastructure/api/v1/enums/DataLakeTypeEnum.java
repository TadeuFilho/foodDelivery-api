package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum DataLakeTypeEnum {
	ORDER("1200"),
	QUOTE("1201"),
	FULFILLMENT("1202");

	private final String value;

	DataLakeTypeEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static DataLakeTypeEnum fromValue(String text) {
		for (DataLakeTypeEnum b : DataLakeTypeEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
