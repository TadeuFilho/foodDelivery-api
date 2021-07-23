package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LiveConfigRequestV1 {
	private CompanyConfigEntity business;
}
