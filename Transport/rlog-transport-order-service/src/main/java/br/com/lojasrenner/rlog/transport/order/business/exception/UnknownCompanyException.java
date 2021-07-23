package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.lojasrenner.exception.BaseException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownCompanyException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1721982194587625140L;

	public UnknownCompanyException(String defaultMessage) {
		super(defaultMessage, "400");
	}

}
