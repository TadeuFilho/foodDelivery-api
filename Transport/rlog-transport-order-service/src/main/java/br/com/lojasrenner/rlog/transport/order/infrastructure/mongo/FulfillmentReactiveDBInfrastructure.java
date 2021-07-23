package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository.FulfillmentReactiveRepository;

import java.util.List;
import java.util.Optional;

@Component
public class FulfillmentReactiveDBInfrastructure {

	@Autowired
	private FulfillmentReactiveRepository fulfillmentReactiveRepository;
	
    public FulfillmentRequest save(FulfillmentRequest entity) {
    	return fulfillmentReactiveRepository.save(entity);
    }

    public Optional<FulfillmentRequest> findById(String id) {
        return fulfillmentReactiveRepository.findById(id);
    }

    public List<FulfillmentRequest> findByDeliveryOptionsRequestId(String id) {
        return fulfillmentReactiveRepository.findByDeliveryOptionsRequestIdOrderByDateDesc(id);
    }
    
}
