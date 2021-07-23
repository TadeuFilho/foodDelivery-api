package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BrokerBadRequestException extends ResponseStatusException {
	public BrokerBadRequestException(String defaultMessage) {
		super(HttpStatus.BAD_REQUEST, defaultMessage);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 3012343745233367698L;
}
