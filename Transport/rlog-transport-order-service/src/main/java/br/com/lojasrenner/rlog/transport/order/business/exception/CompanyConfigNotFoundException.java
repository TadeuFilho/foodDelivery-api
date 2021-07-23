package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CompanyConfigNotFoundException extends ResponseStatusException {
	public CompanyConfigNotFoundException(String defaultMessage) {
		super(HttpStatus.NOT_FOUND, defaultMessage);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3013528745883367698L;
}
