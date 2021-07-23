package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum TypeTimeoutEnum {
    CONNECT_TIMEOUT("ConnectTimeout"),
    SOCKET_TIMEOUT("SocketTimeout");

    private final String value;

    TypeTimeoutEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static TypeTimeoutEnum fromValue(String text) {
        for (TypeTimeoutEnum b : TypeTimeoutEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
