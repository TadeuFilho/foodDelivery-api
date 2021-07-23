package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuotationNotFoundException extends ResponseStatusException {

	public QuotationNotFoundException(String defaultMessage) {
		super(HttpStatus.NOT_FOUND, defaultMessage);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -2905555138472642807L;

}
