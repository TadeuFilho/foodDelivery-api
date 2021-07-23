package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum BranchSortingEnum {

	COUNT("COUNT"),
	COST("COST");

	private final String value;

	BranchSortingEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static BranchSortingEnum fromValue(String text) {
		for (BranchSortingEnum b : BranchSortingEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
	
}
