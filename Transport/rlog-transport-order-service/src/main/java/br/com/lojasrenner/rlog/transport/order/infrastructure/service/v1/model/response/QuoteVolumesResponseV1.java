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
public class QuoteVolumesResponseV1 {

    private Long weight;
    @JsonProperty("cost_of_goods")
    private Double costOfGoods;
    private Long width;
    private Long height;
    private Long length;
    private String description;
    @JsonProperty("sku_groups_ids")
    private List<Object> skuGroupsIds;
    @JsonProperty("volume_type")
    private String volumeType;
}
