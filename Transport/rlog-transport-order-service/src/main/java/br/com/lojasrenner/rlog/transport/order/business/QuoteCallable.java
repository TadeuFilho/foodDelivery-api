package br.com.lojasrenner.rlog.transport.order.business;

import java.util.concurrent.Callable;

import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryRequest;

public abstract class QuoteCallable implements Callable<QuotationDTO> {

	protected DeliveryRequest<?> deliveryRequest;
	protected String state;
	protected String branchOfficeId;

	protected QuoteCallable(DeliveryRequest<?> deliveryRequest) {
		this.deliveryRequest = deliveryRequest;
	}

	protected QuoteCallable(DeliveryRequest<?> deliveryRequest, String state) {
		this.deliveryRequest = deliveryRequest;
		this.state = state;
	}

	protected QuoteCallable(DeliveryRequest<?> deliveryRequest, String branchOfficeId, String state) {
		this.deliveryRequest = deliveryRequest;
		this.state = state;
		this.branchOfficeId = branchOfficeId;
	}

	@Override
	public abstract QuotationDTO call() throws Exception;

}

