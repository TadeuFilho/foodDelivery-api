package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransportOrderReturn {
	private String orderId;
	private LocalDateTime receivedDate;
	private String orderNumber;
	private String salesOrderNumber;
	private String partnerPlatform;
	private String status;
}
