package br.com.lojasrenner.rlog.transport.order.business.domain.query.model;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.ShippingGroupResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FindBestSolution {

    final List<ShippingGroupResponseV1> geolocationResponse;
    final List<CartItem> itemsList;
    final List<LocationStockV1Response> stockResponseFiltered;
    List<String> eagerBranches;
    final Map<String, Integer> skuQuantityMap;
    final List<BranchOfficeEntity> activeBranchOffices;
}
