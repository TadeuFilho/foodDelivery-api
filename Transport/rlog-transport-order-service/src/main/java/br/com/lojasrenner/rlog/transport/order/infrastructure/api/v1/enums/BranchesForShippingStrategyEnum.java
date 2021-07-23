package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum BranchesForShippingStrategyEnum {
	GEOLOCATION("GEOLOCATION"),
	ZIPCODE_RANGE("ZIPCODE_RANGE");

	private final String value;

	BranchesForShippingStrategyEnum(String value) {
		this.value = value;
	}

	@Override
    public String toString() {
        return this.value;
    }

    public static BranchesForShippingStrategyEnum fromValue(String text) {
        for (BranchesForShippingStrategyEnum b : BranchesForShippingStrategyEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
