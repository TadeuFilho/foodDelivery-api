package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums;

import java.util.regex.Pattern;


public enum ServicesErrorRegexEnum {
	INTELIPOST_NO_DELIVERY_OPTIONS(Pattern.compile("quote\\.no\\.delivery\\.options"));
	
	private Pattern regex;
	
	ServicesErrorRegexEnum(Pattern regex) {
		this.regex = regex;
	}
	
	public boolean isMatch(String s) {
		return regex.matcher(s).find();
	}
	
	public static String anyMatch(String s) {
		for(ServicesErrorRegexEnum r : values()) {
			boolean asMatch = r.isMatch(s);
			if(asMatch)
				return r.name();
		}
		return "UNIDENTIFIED_ERROR";
	}
	
	public static ServicesErrorRegexEnum fromValue(String text) {
        for (ServicesErrorRegexEnum b : ServicesErrorRegexEnum.values()) {
            if (String.valueOf(b.regex).equals(text)) {
                return b;
            }
        }
        return null;
    }
	
	@Override
	public String toString() {
		return this.toString();
	}
}
