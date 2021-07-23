package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.FulfillmentReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.ScheduleOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.audit.DatabaseDocument;

@CrossOrigin
@RestController
public class AuditController {

	@Autowired
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Autowired
	private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

	@Autowired
	private ScheduleOptionsReactiveDBInfrastructure scheduleOptionsDB;

	@Autowired
	private FulfillmentReactiveDBInfrastructure fulfillmentDB;

	@GetMapping(value = "/v1/audit/external-code", produces = {"application/json"})
	public ResponseEntity<List<DatabaseDocument>> findIdsByExternalCode(
			@RequestParam(value = "externalCode") String externalCode
	) {
		return Optional.ofNullable(deliveryOptionsDB.findIdsForExternalCode(externalCode))
				.filter(Predicate.not(List::isEmpty))
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}

	@GetMapping(value = "/v1/audit/quote/{id}", produces = {"application/json"})
	public ResponseEntity<DeliveryOptionsRequest> findByQuoteId(
			@PathVariable(value = "id") String id
	) {
		return deliveryOptionsDB.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}

	@GetMapping(value = "/v1/audit/quote/{id}/pickups", produces = {"application/json"})
	public ResponseEntity<List<PickupOptionsRequest>> findPickupsForQuote(
			@PathVariable(value = "id") String id
	) {
		return Optional.ofNullable(pickupOptionsDB.findByDeliveryOptionsId(id))
				.filter(Predicate.not(List::isEmpty))
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}

	@GetMapping(value = "/v1/audit/quote/{id}/schedules", produces = {"application/json"})
	public ResponseEntity<List<ScheduleDetailsRequest>> findSchedulesForQuote(
			@PathVariable(value = "id") String id
	) {
		return Optional.ofNullable(scheduleOptionsDB.findByDeliveryOptionsId(id))
				.filter(Predicate.not(List::isEmpty))
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}

	@GetMapping(value = "/v1/audit/quote/{id}/fulfills", produces = {"application/json"})
	public ResponseEntity<List<FulfillmentRequest>> findFulfillsForQuote(
			@PathVariable(value = "id") String id
	) {
		return Optional.ofNullable(fulfillmentDB.findByDeliveryOptionsRequestId(id))
				.filter(Predicate.not(List::isEmpty))
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}

	@GetMapping(value = "/v1/audit/fulfill/{id}", produces = {"application/json"})
	public ResponseEntity<FulfillmentRequest> findByFulfillId(
			@PathVariable(value = "id") String id
	) {
		return fulfillmentDB.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(ResponseEntity.notFound()::build);
	}
}
