package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.lojasrenner.exception.BaseException;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownBranchOfficeException extends BaseException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8181640785723057217L;

	public UnknownBranchOfficeException(String defaultMessage, String code) {
		super(defaultMessage, code);
	}

}
