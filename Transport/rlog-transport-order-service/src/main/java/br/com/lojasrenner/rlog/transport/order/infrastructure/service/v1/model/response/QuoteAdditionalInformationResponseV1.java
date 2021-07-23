package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(value = AccessLevel.PACKAGE)
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteAdditionalInformationResponseV1 {
    @JsonProperty("extra_cost_absolute")
    private Integer extraCostAbsolute;
    @JsonProperty("lead_time_business_days")
    private Integer leadTimeBusinessDays;
    @JsonProperty("free_shipping")
    private Integer freeShipping;
    @JsonProperty("delivery_method_ids")
    private List<Integer> deliveryMethodIds;
    @JsonProperty("extra_cost_percentage")
    private Integer extraCostPercentage;
    @JsonProperty("tax_id")
    private String taxId;
    @JsonProperty("client_type")
    private String clientType;
    @JsonProperty("sales_channel")
    private String salesChannel;
    @JsonProperty("payment_type")
    private String paymentType;
    @JsonProperty("is_state_tax_payer")
    private String isStateTaxPayer;
}
