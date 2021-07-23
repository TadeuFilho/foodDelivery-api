package br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ExtraIdentification {
	private String session;
	private String ip;
	private String pageName;
	private String url;
	private String extOrderCode;
}
