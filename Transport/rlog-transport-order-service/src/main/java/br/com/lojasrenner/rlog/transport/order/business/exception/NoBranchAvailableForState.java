package br.com.lojasrenner.rlog.transport.order.business.exception;

public class NoBranchAvailableForState extends IllegalArgumentException {

	public NoBranchAvailableForState(String message) {
		super(message);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6788534750713855993L;

}
