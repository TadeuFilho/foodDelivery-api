package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ModalIdNotFoundException extends ResponseStatusException {

	public ModalIdNotFoundException(String defaultMessage) {
		super(HttpStatus.BAD_REQUEST, defaultMessage);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -2905749138472456807L;

}
