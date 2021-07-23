package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum ReasonTypeEnum {
    ITEMS_IS_NULL("ITEMS_IS_NULL"),
    ZIPCODE_IS_NULL("ZIPCODE_IS_NULL"),
    QUANTITY_IS_NULL("QUANTITY_IS_NULL"),
    STOCK_STATUS_IS_NULL("STOCK_STATUS_IS_NULL"),
    PRODUCT_TYPE_IS_NULL("PRODUCT_TYPE_IS_NULL"),
    DUPLICATED_ITEMS_IS_NULL("DUPLICATED_ITEMS_IS_NULL"),
    MODAL_ID_IS_NULL("MODAL_ID_IS_NULL"),
    INVALID_MODAL_ID("INVALID_MODAL_ID"),
    EMPTY_QUOTE_ID("EMPTY_QUOTE_ID"),
    INVALID_SKU_GROUP("INVALID_SKU_GROUP"),
    INVALID_SKU("INVALID_SKU"),
    NOT_FOUND_QUOTE_ID("NOT_FOUND_QUOTE_ID"),
    GIFT_WITH_OMNISTOCK("GIFT_WITH_OMNISTOCK"),
    MEASURES_IS_NULL("MEASURES_IS_NULL");

    private final String value;

    ReasonTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static ReasonTypeEnum fromValue(String text) {
        for (ReasonTypeEnum b : ReasonTypeEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
