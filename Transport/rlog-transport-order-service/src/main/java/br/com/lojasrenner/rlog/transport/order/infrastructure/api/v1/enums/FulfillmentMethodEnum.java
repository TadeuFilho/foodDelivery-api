package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Tipo de fulfillment (loja, cd, ...).
 */
@Getter
public enum FulfillmentMethodEnum {
	STORE("STORE", Pattern.compile("^(?i)\\bstore\\b$")),

	CD("CD", Pattern.compile("^(?i)\\bCD\\d{0,2}\\b$"));

	private final String value;
	private final Pattern pattern;

	FulfillmentMethodEnum(String value, Pattern pattern) {
		this.value = value;
		this.pattern = pattern;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static FulfillmentMethodEnum fromValue(String text) {
		for (FulfillmentMethodEnum b : FulfillmentMethodEnum.values()) {
			if (String.valueOf(b.value).equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}

	public boolean notMatch(String value) { return !isMatch(value); }

	public boolean isMatch(String value) { return Objects.nonNull(value) && pattern.matcher(value).matches(); }
}
