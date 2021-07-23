package br.com.lojasrenner.rlog.transport.order.business.model;

import java.util.List;

import org.springframework.http.ResponseEntity;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItemWithMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StockQuoteResponseDTO {
	private String companyId;
	private CartOrder cartOrder;
	
	private String branchOfficeId;
	private List<CartItemWithMode> items;
	
	private ResponseEntity<QuoteResponseV1> quoteResponse;
}
