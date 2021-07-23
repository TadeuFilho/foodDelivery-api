package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;

public interface PickupOptionsReactiveRepository extends CrudRepository<PickupOptionsRequest, String> {

	List<PickupOptionsRequest> findByDeliveryOptionsIdOrderByDateDesc(String deliveryOptionsId);
	
}
