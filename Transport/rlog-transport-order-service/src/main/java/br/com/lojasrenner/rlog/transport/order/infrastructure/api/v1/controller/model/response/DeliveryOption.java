package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOption {
	@ApiModelProperty(value = "SKU do item")
	private String sku;

	@ApiModelProperty(value = "Quantidade do item")
	private Integer quantity;

	@ApiModelProperty(value = "Modais de enterga disponíveis para o item")
	@Valid
	private List<DeliveryMode> deliveryModes;
	
	@JsonIgnore
	private List<DeliveryMode> deliveryModesVerbose;
}
