package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum GroupOriginType {
	NORMAL("normal"),
	PRIORITY("priority"),
	EXTRA_GROUP("extraGroup");

	private final String value;

	GroupOriginType(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static GroupOriginType fromValue(String text) {
		for (GroupOriginType b : GroupOriginType.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
