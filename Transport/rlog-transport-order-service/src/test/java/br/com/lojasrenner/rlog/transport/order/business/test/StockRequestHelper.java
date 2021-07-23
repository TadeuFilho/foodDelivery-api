package br.com.lojasrenner.rlog.transport.order.business.test;

import java.util.List;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class StockRequestHelper {
	private String companyId;
	List<CartItem> requestedSkus;
	List<BranchOfficeEntity> activeBranchOfficesForPickup;
}