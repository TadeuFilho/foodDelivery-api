package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

public enum EndpointEnum {
    FREIGHT_SERVICE_GET_QUOTE("companies/{companyId}/quotes"),
    FREIGHT_SERVICE_GET_SCHEDULE_DATES("companies/{companyId}/quotes/scheduling-dates"),
    GEOLOCATION_SERVICE_GET_CLOSEST_STORES_IN_STATE("companies/{companyId}/nearest-branch/{zipcode}"),
    GEOLOCATION_SERVICE_GET_CLOSEST_STORES_IN_RANGE("companies/{companyId}/nearest-branch-in-range/{zipcode}"),
    GEOLOCATION_SERVICE_GET_GROUPS_BY_ZIPCODE("companies/{companyId}/range-nearest-branch/{zipcode}"),
    GEOLOCATION_SERVICE_GET_STORES_BY_STATE("companies/{companyId}/state/{state}"),
    BRANCH_SERVICE_FIND_BY_COMPANY_ID("companies/{companyId}/branch-offices"),
    STOCK_API_POST_LOCATION_STOCK("companies/{companyId}/stocks"),
    COMBINATIONS_DELIVERY_MODES_QUERY_FOR_SHOPPING_CART("companies/{companyId}/broker/delivery/cart/query"),
    CHECKOUT_API_CHECKOUT_QUOTATION("companies/{companyId}/checkout"),
    GEOLOCATION_SHIPPING_TO_PER_BRANCHES("companies/{companyId}/shipping-to/list-branches?branches={branches}");


    private final String value;

    EndpointEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static EndpointEnum fromValue(String text) {
        for (EndpointEnum b : EndpointEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
