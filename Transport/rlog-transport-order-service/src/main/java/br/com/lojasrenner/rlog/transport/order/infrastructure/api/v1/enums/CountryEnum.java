package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

import br.com.lojasrenner.rlog.transport.order.business.helper.zipcode.ArgentinaZipCodeHelper;
import br.com.lojasrenner.rlog.transport.order.business.helper.zipcode.BrazilZipCodeHelper;
import br.com.lojasrenner.rlog.transport.order.business.helper.zipcode.UruguayZipCodeHelper;
import br.com.lojasrenner.rlog.transport.order.business.helper.zipcode.ZipCodeHelper;

public enum CountryEnum {
	BR(new BrazilZipCodeHelper(), "BR"),
	AR(new ArgentinaZipCodeHelper(), "AR"),
	UY(new UruguayZipCodeHelper(), "UY");

	private final ZipCodeHelper helper;
	private final String name;

	CountryEnum(ZipCodeHelper helper, String name) {
		this.helper = helper;
		this.name = name;
	}

	public ZipCodeHelper getHelper() {
		return helper;
	}

	public static CountryEnum fromName(String name) {
		for(CountryEnum z : CountryEnum.values()) {
			if(z.name().equals(name))
				return z;
		}
		return null;
	}


}
