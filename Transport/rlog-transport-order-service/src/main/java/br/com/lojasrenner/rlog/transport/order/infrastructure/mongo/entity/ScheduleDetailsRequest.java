package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;


import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.ScheduleDetailsReturn;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document("scheduleDetailsRequest")
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ScheduleDetailsRequest extends BrokerRequest<ScheduleDetailsReturn> {

	@Indexed
	private String deliveryOptionsId;
	
	private String deliveryModeId;
	private int quantity;
	private int fromToday;
	
}