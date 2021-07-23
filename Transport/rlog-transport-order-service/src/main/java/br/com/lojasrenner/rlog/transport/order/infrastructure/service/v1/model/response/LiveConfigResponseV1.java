package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.LiveConfigRequestV1;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveConfigResponseV1 {
	private String message;
	private LiveConfigRequestV1 config;
}
