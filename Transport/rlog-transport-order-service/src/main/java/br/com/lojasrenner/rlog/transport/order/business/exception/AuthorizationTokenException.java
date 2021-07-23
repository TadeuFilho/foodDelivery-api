package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AuthorizationTokenException extends ResponseStatusException {

	private static final long serialVersionUID = 2534105626054734483L;

	public AuthorizationTokenException(String message) {
		super(HttpStatus.UNAUTHORIZED, message);
	}
}
