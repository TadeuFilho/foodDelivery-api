package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum ServiceTypeEnum {
    FREIGHT_SERVICE("FREIGHT_SERVICE"),
    GEOLOCATION_SERVICE("GEOLOCATION_SERVICE"),
    BRANCH_SERVICE("BRANCH_SERVICE"),
    STOCK_API("STOCK_API"),
    COMBINATIONS("COMBINATIONS"),
    CHECKOUT_SERVICE("CHECKOUT_SERVICE");
    private final String value;

    ServiceTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static ServiceTypeEnum fromValue(String text) {
        for (ServiceTypeEnum b : ServiceTypeEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
