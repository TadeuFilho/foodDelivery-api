package br.com.lojasrenner.rlog.transport.order;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Input {

	private List<SimpleEntry<String, Object>> headers;
	private List<SimpleEntry<String, Object>> pathParam;
	private Map<String, List<String>> queryStringParam;
	private Map<String, Object> body;
	
}
