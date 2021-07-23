package br.com.lojasrenner.rlog.transport.order.helper;

import com.google.gson.Gson;

public class JSON {

	private static Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
	
	public static String toJson(Object o) {
		return GSON.toJson(o);
	}
	
}
