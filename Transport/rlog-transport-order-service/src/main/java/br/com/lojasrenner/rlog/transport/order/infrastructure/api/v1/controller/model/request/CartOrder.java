package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CartOrder {

	@ApiModelProperty(example = "129038-12381293-1238123", value = "Identificador da oferta de modais. (retornado por /cart/query)")
	private String id;
	
	@ApiModelProperty(value = "Lista de skus e quantidades que compoem o no carrinho")
	private List<CartItemWithMode> items;
	
	private CartDestination destination;

}
