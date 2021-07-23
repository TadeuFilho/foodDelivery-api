package br.com.lojasrenner.rlog.transport.order.business.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class QuotationDTO {
	private Map<String, List<CartItem>> itemListMap;
	private Map<String, QuoteResponseV1> quoteMap;
	private Map<String, PickupOptionsReturn> pickupOptionsReturnMap;
	private Boolean preSale;
	
	public void addPickupOption(String key, PickupOptionsReturn pickupOptions) {
		if (pickupOptionsReturnMap == null)
			pickupOptionsReturnMap = new ConcurrentHashMap<>();

		pickupOptionsReturnMap.put(key, pickupOptions);
	}

	public QuotationDTO(Map<String, List<CartItem>> itemListMap, Map<String, QuoteResponseV1> quoteMap, Map<String, PickupOptionsReturn> pickupOptionsReturnMap){
		this(itemListMap, quoteMap, pickupOptionsReturnMap, false);
	}
}
