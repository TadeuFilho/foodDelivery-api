package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.repository.ScheduleOptionsReactiveRepository;

import java.util.List;

@Component
public class ScheduleOptionsReactiveDBInfrastructure {

	@Autowired
	private ScheduleOptionsReactiveRepository scheduleOptionsReactiveRepository;
	
    public ScheduleDetailsRequest save(ScheduleDetailsRequest entity) {
    	return scheduleOptionsReactiveRepository.save(entity);
    }

    public List<ScheduleDetailsRequest> findByDeliveryOptionsId(String deliveryOptionsId)
    {
        return scheduleOptionsReactiveRepository.findByDeliveryOptionsIdOrderByDateDesc(deliveryOptionsId);
    }
	
}
