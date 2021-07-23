package br.com.lojasrenner.rlog.transport.order.business.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistinctOriginDTO {
	private Integer maxOrigins;
	private Integer maxStoreOrigin;
}
