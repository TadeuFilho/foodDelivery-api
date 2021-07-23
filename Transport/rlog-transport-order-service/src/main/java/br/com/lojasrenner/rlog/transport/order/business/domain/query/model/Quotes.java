package br.com.lojasrenner.rlog.transport.order.business.domain.query.model;

import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Quotes {
	QuotationDTO quoteFromStore;
	QuotationDTO quoteFromEcomm;
	QuotationDTO quoteFromPreSale;
}
