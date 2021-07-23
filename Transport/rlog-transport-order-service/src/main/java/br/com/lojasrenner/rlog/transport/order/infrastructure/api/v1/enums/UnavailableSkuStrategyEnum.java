package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum UnavailableSkuStrategyEnum {

    UNAVAILABLE_MODE("UNAVAILABLE_MODE"),
    RETRY_MODE("RETRY_MODE");

    private final String value;

    UnavailableSkuStrategyEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static UnavailableSkuStrategyEnum fromValue(String text) {
        for (UnavailableSkuStrategyEnum b : UnavailableSkuStrategyEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }

}
