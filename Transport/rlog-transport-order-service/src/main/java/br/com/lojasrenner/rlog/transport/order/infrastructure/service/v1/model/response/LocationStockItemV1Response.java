package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class LocationStockItemV1Response {
    private String sku;
    private Integer amountSaleable;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer amountPhysical;
    private Boolean blocked;
    
	public boolean isBlocked() {
		return blocked != null && blocked;
	}

	public Integer getAmountSaleable(){ return amountSaleable == null ? 0 : amountSaleable; }
}
