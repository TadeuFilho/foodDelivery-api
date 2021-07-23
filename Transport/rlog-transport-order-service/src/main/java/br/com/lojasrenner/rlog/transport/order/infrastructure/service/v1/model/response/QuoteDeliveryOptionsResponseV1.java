package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(value = AccessLevel.PACKAGE)
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteDeliveryOptionsResponseV1 {
    @JsonProperty("delivery_method_id")
    private Integer deliveryMethodId;
    @JsonProperty("delivery_estimate_business_days")
    private Integer deliveryEstimateBusinessDays;
    @JsonProperty("delivery_estimate_transit_time_business_days")
    private Integer deliveryEstimateTransitTimeBusinessDays;
    @JsonProperty("provider_shipping_cost")
    private Double providerShippingCost;
    @JsonProperty("final_shipping_cost")
    private Double finalShippingCost;
    private String description;
    @JsonProperty("delivery_note")
    private String deliveryNote;
    @JsonProperty("cubic_weight")
    private Double cubicWeight;
    @JsonProperty("delivery_method_type")
    private String deliveryMethodType;
    @JsonProperty("delivery_method_name")
    private String deliveryMethodName;
    @JsonProperty("scheduling_enabled")
    private boolean schedulingEnabled;
    @JsonProperty("logistic_provider_name")
    private String logisticProviderName;
    @JsonProperty("delivery_estimate_date_exact_iso")
    private String deliveryEstimateDateExactIso;
    @JsonProperty("pickup_enabled")
    private boolean isPickupEnabled;
    @JsonProperty("removed_by_quote_rules")
    private boolean removedByQuoteRules;
}
