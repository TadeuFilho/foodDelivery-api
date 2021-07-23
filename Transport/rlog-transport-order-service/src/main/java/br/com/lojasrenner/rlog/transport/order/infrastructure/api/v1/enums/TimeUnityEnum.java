package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum TimeUnityEnum {

		MINUTE("MINUTE"),

		HOUR("HOUR"),

		DAY("DAY"),

		WEEK("WEEK"),

		MONTH("MONTH"),

		YEAR("YEAR");

		private final String value;

		TimeUnityEnum(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}

		public static TimeUnityEnum fromValue(String text) {
			for (TimeUnityEnum b : TimeUnityEnum.values()) {
				if (String.valueOf(b.value).equals(text)) {
					return b;
				}
			}
			return null;
		}
	
}
