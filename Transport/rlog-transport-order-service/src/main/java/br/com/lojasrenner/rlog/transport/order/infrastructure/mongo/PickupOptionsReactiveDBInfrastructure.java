package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository.PickupOptionsReactiveRepository;

@Component
public class PickupOptionsReactiveDBInfrastructure {

	@Autowired
	private PickupOptionsReactiveRepository pickupOptionsReactiveRepository;
	
    public PickupOptionsRequest save(PickupOptionsRequest entity) {
    	return pickupOptionsReactiveRepository.save(entity);
    }
    
    public List<PickupOptionsRequest> findByDeliveryOptionsId(String deliveryOptionsId) {
    	return pickupOptionsReactiveRepository.findByDeliveryOptionsIdOrderByDateDesc(deliveryOptionsId);
    }
	
}
