package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo;

import java.util.List;
import java.util.Optional;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.audit.DatabaseDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository.DeliveryOptionsRepository;

@Component
public class DeliveryOptionsDBInfrastructure {

	@Autowired
	private DeliveryOptionsRepository deliveryOptionsRepository;

	public DeliveryOptionsRequest save(DeliveryOptionsRequest entity) {
		return deliveryOptionsRepository.save(entity);
	}

	public Optional<DeliveryOptionsRequest> findById(String id){
		return deliveryOptionsRepository.findById(id);
	}

	public List<DatabaseDocument> findIdsForExternalCode(String externalCode) {
		return deliveryOptionsRepository.findIdsForExternalCode(externalCode);
	}
}
