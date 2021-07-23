package br.com.lojasrenner.rlog.transport.order;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Output {

	private List<SimpleEntry<String, Object>> headers;
	private Map<String, Object> body;
	
}
