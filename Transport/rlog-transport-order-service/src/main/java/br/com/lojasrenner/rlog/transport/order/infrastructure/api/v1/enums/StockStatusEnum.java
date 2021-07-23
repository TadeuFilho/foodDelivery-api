package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum StockStatusEnum {
	INSTOCK("INSTOCK"),
	INOMNISTOCK("INOMNISTOCK"),
	PREORDERABLE("PREORDERABLE"),
	BACKORDERABLE("BACKORDERABLE");

	private final String value;

	StockStatusEnum(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static StockStatusEnum fromValue(String text) {
		for (StockStatusEnum b : StockStatusEnum.values()) {
			if (String.valueOf(b.value).equals(text)) {
				return b;
			}
		}
		return null;
	}

	public static Boolean isPreSale(StockStatusEnum stockStatusEnum) {
		return stockStatusEnum != null && (stockStatusEnum.equals(BACKORDERABLE) || stockStatusEnum.equals(PREORDERABLE));
	}

	public static Boolean isOmniStock(StockStatusEnum stockStatusEnum) {
		return stockStatusEnum != null && stockStatusEnum.equals(INOMNISTOCK);
	}
}
