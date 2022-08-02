package br.com.lojasrenner.rlog.transport.order.hub.infrastructure.api.v1.controller;

import br.com.lojasrenner.rlog.transport.order.hub.infrastructure.service.v1.TransportOrderHubService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;

@RestController
@Import(FeignClientsConfiguration.class)
@Log4j2
public class TransportOrderHubController {

	@Autowired
	private TransportOrderHubService transportOrderHubService;

	@ResponseStatus(HttpStatus.OK)
	@PostMapping(value = "/v1/transportOrderHub/{partnerId}/{method}")
	public ResponseEntity<Object> createHub(@Valid @RequestBody String body, @PathVariable String partnerId, @PathVariable String method) throws URISyntaxException {
		return ResponseEntity.ok(transportOrderHubService.sendToRightPartner(body, partnerId, method));

	}

	@ResponseStatus(HttpStatus.OK)
	@PostMapping(value = "/v1/saveData/{partnerId}/{method}",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> saveData(@Valid @RequestBody Object body, @PathVariable String partnerId, @PathVariable String method) throws URISyntaxException {
		return ResponseEntity.ok(transportOrderHubService.save(body, partnerId, method));

	}


}
