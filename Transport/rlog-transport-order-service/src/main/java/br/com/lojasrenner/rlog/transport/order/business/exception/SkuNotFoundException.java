package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SkuNotFoundException extends ResponseStatusException {

	private static final long serialVersionUID = 7408001540173389289L;

	public SkuNotFoundException(String defaultMessage) {
		super(HttpStatus.BAD_REQUEST, defaultMessage);
	}

}
