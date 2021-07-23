package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PickupOptionsReturn {
	
	private List<PickupOption> pickupOptions;
}
