package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.business.exception.BrokerException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.ScheduleService;

@RestController
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping(value = "/v1/companies/{companyId}/broker/schedule/{cartId}/{deliveryModeId}/details",
            produces = {"application/json"})
    public ResponseEntity<ScheduleDetailsReturn> deliveryScheduleDetailsOptions(
            @RequestHeader(value = "X-Application-Name") String xApplicationName,
            @RequestHeader(value = "X-Current-Date") String xCurrentDate,
            @RequestHeader(value = "X-Locale") String xLocale,
            @PathVariable(name = "companyId") String companyId,
            @PathVariable(name = "cartId") String id,
            @PathVariable(name = "deliveryModeId") String deliveryModeId,
            @RequestParam(name = "quantity", required = false, defaultValue = "15") int quantity,
            @RequestParam(name = "from_today", required = false, defaultValue = "15") int fromToday
    ) throws BrokerException {
        return scheduleService.deliveryScheduleDetailsOptions(xApplicationName, xCurrentDate, xLocale, companyId, id, deliveryModeId, quantity, fromToday);
    }

}
