package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.FulfillmentRequest;

import java.util.List;

public interface FulfillmentReactiveRepository extends CrudRepository<FulfillmentRequest, String> {

    public List<FulfillmentRequest> findByDeliveryOptionsRequestIdOrderByDateDesc(String id);

}
