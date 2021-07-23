package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.util.List;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDetailsReturn {

	private String status;
	private List<String> availableBusinessDays;
	private List<Object> messages;
	
}
