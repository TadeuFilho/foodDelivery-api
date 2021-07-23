package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum BranchOfficeTypeEnum {
	IN_STORE("IN_STORE"),
	WEB_STORE("WEB_STORE");

	private final String value;

	BranchOfficeTypeEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static BranchOfficeTypeEnum fromValue(String text) {
		for (BranchOfficeTypeEnum b : BranchOfficeTypeEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}

	public static List<String> getBranchOfficeList() {
		return Arrays.stream(BranchOfficeTypeEnum.values())
				.map(BranchOfficeTypeEnum::toString)
				.collect(Collectors.toList());
	}
}
