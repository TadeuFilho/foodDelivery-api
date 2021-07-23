package br.com.lojasrenner.rlog.transport.order.business.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AutoReQuoteWithShippingTypeNotFound extends ResponseStatusException {

    public AutoReQuoteWithShippingTypeNotFound(String defaultMessage) {
        super(HttpStatus.NOT_FOUND, defaultMessage);
    }

    private static final long serialVersionUID = -6585842914055070727L;

}