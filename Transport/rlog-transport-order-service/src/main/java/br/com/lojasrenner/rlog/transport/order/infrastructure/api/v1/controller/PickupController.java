package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.business.exception.BranchOptionsNotFoundOnGeolocationException;
import br.com.lojasrenner.rlog.transport.order.business.exception.EmptyDeliveryOptionsRequestIdException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoActiveBranchForPickupException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.PickupService;

@RestController
public class PickupController {

    @Autowired
    private PickupService pickupService;

    @GetMapping(value = "/v1/companies/{companyId}/broker/delivery/cart/query/{id}/pickup/options",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PickupOptionsReturn> deliveryPickupOptions(
            @RequestHeader(value = "X-Application-Name") String xApplicationName,
            @RequestHeader(value = "X-Current-Date") String xCurrentDate,
            @RequestHeader(value = "X-Locale") String xLocale,
            @PathVariable("companyId") String companyId,
            @PathVariable("id") String deliveryOptionsId,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "zipcode", required = false) String zipcode,
            @RequestParam(name = "skus", required = false) List<String> skus,
			@RequestParam(value= "blockedBranches", required = false) List<String> blockedBranches
    ) throws NoActiveBranchForPickupException, EmptyDeliveryOptionsRequestIdException, BranchOptionsNotFoundOnGeolocationException {
        return ResponseEntity.ok(pickupService
                .deliveryPickupOptions(xApplicationName, xCurrentDate, xLocale, companyId, deliveryOptionsId, state, zipcode, skus, blockedBranches));
    }

}
