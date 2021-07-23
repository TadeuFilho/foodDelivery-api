package br.com.lojasrenner.rlog.transport.order.business.util;

public class ZipCodeUtil {

	private ZipCodeUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean isInt(String num) {
		if (num == null || num.isEmpty())
			return false;

		try {
			Integer.parseInt(num);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

}
