package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum ScheduleTypeExceptionEnum {
	TIMEOUT("TIMEOUT"),
	ERROR("ERROR");

	private final String value;

	ScheduleTypeExceptionEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static ScheduleTypeExceptionEnum fromValue(String text) {
		for (ScheduleTypeExceptionEnum b : ScheduleTypeExceptionEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
}
