package br.com.lojasrenner.rlog.transport.order.business.helper.zipcode;

import br.com.lojasrenner.rlog.transport.order.business.util.ZipCodeUtil;

import java.util.regex.Pattern;

public class BrazilZipCodeHelper implements ZipCodeHelper{

	private static final Pattern PATTERN = Pattern.compile("(\\-)|(\\.)|(\\s)");
	private static final int RANGE_BEGIN = 1000000;
	private static final int RANGE_END = 99999999;

	@Override
	public String normalize(String input) {
		return PATTERN.matcher(input).replaceAll("");
	}

	@Override
	public boolean isValid(String input) {
		String cleanZipCode = PATTERN.matcher(input).replaceAll("");
		boolean isInt = ZipCodeUtil.isInt(cleanZipCode);

		return isInt && Integer.parseInt(cleanZipCode) >= RANGE_BEGIN && Integer.parseInt(cleanZipCode) <= RANGE_END;
	}
}
