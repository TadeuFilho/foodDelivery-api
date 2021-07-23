package br.com.lojasrenner.rlog.transport.order.business.exception;

public class NoQuotationAvailableForFulfillment extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6160658662734094235L;
	
	public NoQuotationAvailableForFulfillment(String message, Throwable e) {
		super(message, e);
	}

}
