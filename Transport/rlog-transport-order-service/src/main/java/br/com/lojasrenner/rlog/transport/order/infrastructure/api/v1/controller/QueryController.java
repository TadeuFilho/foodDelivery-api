package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.QueryService;

@RestController
public class QueryController {

    @Autowired
    private QueryService queryService;

    @PostMapping(value = "/v1/companies/{companyId}/broker/delivery/cart/query",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeliveryOptionsReturn> deliveryModesQueryForShoppingCart(
            @RequestHeader(value = "X-Application-Name") String xApplicationName,
            @RequestHeader(value = "X-Current-Date") String xCurrentDate,
            @RequestHeader(value = "X-Locale") String xLocale,
            @RequestHeader(value = "X-Max-Origins", required = false) Integer xMaxOrigins,
            @RequestHeader(value = "X-Max-Origins-Store", required = false) Integer xMaxOriginsStore,
            @RequestHeader(value = "X-Branches-For-Shipping-Strategy", required = false) BranchesForShippingStrategyEnum xBranchesForShippingStrategyEnum,
            @RequestHeader(value = "X-Eager-Branches", required = false) List<String> xEagerBranches,
            @RequestHeader(value = "X-Combinations-Timeout", required = false) Integer xCombinationsTimeOut,
            @RequestHeader(value = "X-Combination-Approach-Cart-Size-Limit", required = false) Integer xCombinationApproachCartSizeLimit,
            @PathVariable("companyId") String companyId,
            @RequestParam(value = "blockedBranches", required = false) List<String> blockedBranches,
            @RequestParam(name = "verbose", required = false, defaultValue = "false") boolean verbose,
            @RequestParam(name = "logisticInfo", required = false, defaultValue = "false") boolean logisticInfo,
            @Valid @RequestBody ShoppingCart body
    ) throws ExecutionException, InterruptedException {
        final DeliveryOptionsReturn deliveryOptionsReturn = queryService.deliveryModesQueryForShoppingCart(xApplicationName,
                xCurrentDate,
                xLocale,
                xMaxOrigins,
                xMaxOriginsStore,
                xBranchesForShippingStrategyEnum,
                xEagerBranches,
                xCombinationsTimeOut,
                xCombinationApproachCartSizeLimit,
                companyId,
                blockedBranches,
                verbose,
                logisticInfo,
                body);
        return ResponseEntity.ok(deliveryOptionsReturn);
    }

    @GetMapping(value = "/v1/companies/{companyId}/broker/delivery/cart/query/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeliveryOptionsReturn> getDeliveryModesQueryById(
            @RequestHeader(value = "X-Application-Name") String xApplicationName,
            @RequestHeader(value = "X-Current-Date") String xCurrentDate,
            @RequestHeader(value = "X-Locale") String xLocale,
            @PathVariable("companyId") String companyId,
            @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(queryService.getDeliveryModesQueryById(
                xApplicationName,
                xCurrentDate,
                xLocale,
                companyId,
                id
        ));
    }
}
