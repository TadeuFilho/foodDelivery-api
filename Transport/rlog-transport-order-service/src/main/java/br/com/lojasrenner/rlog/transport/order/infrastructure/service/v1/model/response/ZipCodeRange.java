package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
public class ZipCodeRange {

	private int from;
	private int to;
	
}
