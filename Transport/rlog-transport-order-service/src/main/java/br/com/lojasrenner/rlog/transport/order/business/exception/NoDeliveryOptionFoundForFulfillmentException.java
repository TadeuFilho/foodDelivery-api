package br.com.lojasrenner.rlog.transport.order.business.exception;

public class NoDeliveryOptionFoundForFulfillmentException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 886211135437405464L;
	
	public NoDeliveryOptionFoundForFulfillmentException() {
		super();
	}
	
	public NoDeliveryOptionFoundForFulfillmentException(String message) {
		super(message);
	}

}
