package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.TransportOrder;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.TransportOrderReturn;

@RestController
public class TransportOrderController {

//	@Autowired
//	private TransportOrderService transportOrderService;

	@ResponseStatus(HttpStatus.OK)
	@PostMapping(value = "/v1/transport/orders", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TransportOrderReturn> createTransportOrder(
			@RequestHeader(value = "X-Company-Id") String xCompanyId, // empresa
			@RequestHeader(value = "X-Application-Id") String xApplicationId, // origem
			@RequestHeader(value = "X-Seller-Id") String xSellerId, // seller
			@Valid @RequestBody TransportOrder body) throws ExecutionException, InterruptedException {

		TransportOrderReturn returnOrder = TransportOrderReturn.builder().orderId("xxx")
				.orderNumber(body.getOrderNumber()).partnerPlatform(body.getPartnerPlatform().getName())
				.receivedDate(LocalDateTime.now()).salesOrderNumber(body.getSalesOrderNumber()).status("Received").build();

		return ResponseEntity.ok(returnOrder);
	}

}
