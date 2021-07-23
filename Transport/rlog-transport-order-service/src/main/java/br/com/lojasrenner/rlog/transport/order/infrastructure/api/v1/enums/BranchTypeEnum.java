package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum BranchTypeEnum {

	STORE("STORE"),
	EXTERNAL_LOCKER("EXTERNAL_LOCKER");
	
	private final String value;

	BranchTypeEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static BranchTypeEnum fromValue(String text) {
		for (BranchTypeEnum b : BranchTypeEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}
	
}
