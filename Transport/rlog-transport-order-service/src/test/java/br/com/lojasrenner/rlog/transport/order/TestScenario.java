package br.com.lojasrenner.rlog.transport.order;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestScenario {

	private Input input;
	private Output output;
	private List<Map<String, Object>> loggingAsserts;
	
}
