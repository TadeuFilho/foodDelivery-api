package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum MetricsOriginsEnum {
	QUERY("query"),
	FULFILLMENT("fulfillment");
	
	private final String value;
	
	MetricsOriginsEnum(String value) {
	        this.value = value;
	    }
	
	@Override
	public String toString() {
		return this.value;
	}
	
	public static MetricsOriginsEnum fromValue(String text) {
        for (MetricsOriginsEnum b : MetricsOriginsEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
