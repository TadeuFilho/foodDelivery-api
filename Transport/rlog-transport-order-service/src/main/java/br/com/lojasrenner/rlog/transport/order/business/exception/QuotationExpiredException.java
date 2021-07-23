package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuotationExpiredException extends ResponseStatusException {

	public QuotationExpiredException(String defaultMessage) {
		super(HttpStatus.GONE, defaultMessage);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -4789408922224275383L;

}
