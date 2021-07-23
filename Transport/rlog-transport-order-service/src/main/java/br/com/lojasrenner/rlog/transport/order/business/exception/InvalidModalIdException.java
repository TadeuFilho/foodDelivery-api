package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidModalIdException extends ResponseStatusException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4496364999310479075L;

	public InvalidModalIdException(String defaultMessage) {
		super(HttpStatus.BAD_REQUEST, defaultMessage);
	}

}
