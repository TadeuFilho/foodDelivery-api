package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.audit.DatabaseDocument;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DeliveryOptionsRepository extends CrudRepository<DeliveryOptionsRequest, String> {

	@Query(value = "{ 'shoppingCart.extraIdentification.extOrderCode' : ?0 }", sort = "{ date: -1 }")
	List<DatabaseDocument> findIdsForExternalCode(String externalCode);

}
