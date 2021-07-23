package br.com.lojasrenner.rlog.transport.order.business.helper.zipcode;

public interface ZipCodeHelper {
	String normalize(String input);
	boolean isValid(String input);
}
