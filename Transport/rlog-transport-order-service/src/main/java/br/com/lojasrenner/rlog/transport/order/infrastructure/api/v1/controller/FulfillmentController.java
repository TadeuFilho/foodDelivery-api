package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.business.exception.NoQuotationAvailableForFulfillment;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.CartOrderResult;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FulfillmentService;

@RestController
public class FulfillmentController {

    @Autowired
    private FulfillmentService fulfillmentService;

    @PostMapping(value = "/v1/companies/{companyId}/broker/delivery/cart/fulfillment",
            produces = {"application/json"})
    public ResponseEntity<CartOrderResult> deliveryFulfillmentForShoppingCart(
            @RequestHeader(value = "X-Application-Name") String xApplicationName,
            @RequestHeader(value = "X-Current-Date") String xCurrentDate,
            @RequestHeader(value = "X-Locale") String xLocale,
            @RequestParam(value = "blockedBranches", required = false) List<String> blockedBranches,
            @PathVariable("companyId") String companyId,
            @Valid @RequestBody CartOrder cartOrder
    ) throws NoQuotationAvailableForFulfillment {
        return ResponseEntity.ok(fulfillmentService.deliveryFulfillmentForShoppingCart(
                xApplicationName, xCurrentDate, xLocale, blockedBranches, companyId, cartOrder));
    }


}
