package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ProductTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class CartItem extends ItemFreightProperties {
	private String sku;
	private Integer quantity;
	private Integer branchOfficeId;

	private StockStatusEnum stockStatus;
	private ProductTypeEnum productType;

	public CartItem(CartItem item) {
		this.setCanGroup(item.getCanGroup());
		this.setCostOfGoods(item.getCostOfGoods());
		this.setHeight(item.getHeight());
		this.setLength(item.getLength());
		this.setProductCategory(item.getProductCategory());
		this.setQuantity(item.getQuantity());
		this.setSku(item.getSku());
		this.setWeight(item.getWeight());
		this.setWidth(item.getWidth());
		this.setStockStatus(item.getStockStatus());
		this.setProductType(item.getProductType());
		this.setBranchOfficeId(item.getBranchOfficeId());
	}
}
