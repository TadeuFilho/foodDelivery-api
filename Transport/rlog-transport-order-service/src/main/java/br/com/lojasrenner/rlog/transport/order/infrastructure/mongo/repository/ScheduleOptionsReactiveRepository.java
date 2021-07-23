package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ScheduleOptionsReactiveRepository extends CrudRepository<ScheduleDetailsRequest, String> {

	List<ScheduleDetailsRequest> findByDeliveryOptionsIdOrderByDateDesc(String deliveryOptionsId);

}
