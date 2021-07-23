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
public class DeliveryOptionsReturn {
	private String id;
	private LocalDateTime date;
	private List<DeliveryOption> deliveryOptions;
	private int distinctOrigins;
	private List<OriginPreview> originPreview;
}
