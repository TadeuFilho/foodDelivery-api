package br.com.lojasrenner.rlog.transport.order.business;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.lojasrenner.rlog.transport.order.business.exception.DeliveryOptionsRequestNotFoundException;
import br.com.lojasrenner.rlog.transport.order.business.exception.ModalIdNotFoundException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.GetQuotationRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.ScheduleDetailsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.SchedulingDateResponseV1;

@Component
public class ScheduleBusiness {

	@Autowired
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Autowired
	private FreightServiceV1 freightService;

	public ScheduleDetailsReturn getScheduleDetailsOptions(ScheduleDetailsRequest scheduleDetailsRequest) {
		GetQuotationRequest getQuotationRequest = new GetQuotationRequest();
		getQuotationRequest.setDeliveryOptionsId(scheduleDetailsRequest.getDeliveryOptionsId());

		Optional<DeliveryOptionsRequest> optionalOptions = deliveryOptionsDB.findById(scheduleDetailsRequest.getDeliveryOptionsId());

		if (optionalOptions.isEmpty())
			throw new DeliveryOptionsRequestNotFoundException("Could not find delivery options");

		Optional<Optional<DeliveryMode>> optionalDeliveryMode = optionalOptions.get().getResponse().getDeliveryOptions()
				.stream()
				.map(o -> o.getDeliveryModesVerbose()
						.stream()
						.filter(d -> d.getModalId()!= null && d.getModalId().equals(scheduleDetailsRequest.getDeliveryModeId()))
						.findAny())
				.filter(Optional::isPresent)
				.findFirst();

		if (optionalDeliveryMode.isEmpty())
			throw new ModalIdNotFoundException("Could not find ModalId " + scheduleDetailsRequest.getDeliveryModeId());

		Optional<DeliveryMode> firstLayer = optionalDeliveryMode.get();

		if (firstLayer.isEmpty())
			throw new ModalIdNotFoundException("Could not find ModalId " + scheduleDetailsRequest.getDeliveryModeId());

		DeliveryMode deliveryMode = firstLayer.get();

		SchedulingDateResponseV1 schedulingDate = freightService.getScheduleDates(scheduleDetailsRequest.getCompanyId(),
				scheduleDetailsRequest.getXApplicationName(),
				deliveryMode.getDeliveryMethodId(),
				deliveryMode.getOrigin(),
				deliveryMode.getDestination(),
				scheduleDetailsRequest.getQuantity(),
				scheduleDetailsRequest.getFromToday())
			.getBody();

		return mapScheduleDetailsReturn(schedulingDate);
	}

	public ScheduleDetailsReturn mapScheduleDetailsReturn(SchedulingDateResponseV1 schedulingDate){
		return ScheduleDetailsReturn.builder()
				.status(schedulingDate.getStatus())
				.availableBusinessDays(schedulingDate.getContent().getAvailableBusinessDays())
				.messages(schedulingDate.getMessages()).build();
	}

}
