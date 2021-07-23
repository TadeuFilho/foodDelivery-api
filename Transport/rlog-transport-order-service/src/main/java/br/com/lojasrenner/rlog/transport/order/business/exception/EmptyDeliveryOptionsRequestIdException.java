package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Delivery id can't be null or empty")
public class EmptyDeliveryOptionsRequestIdException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 822211135985445664L;

}
