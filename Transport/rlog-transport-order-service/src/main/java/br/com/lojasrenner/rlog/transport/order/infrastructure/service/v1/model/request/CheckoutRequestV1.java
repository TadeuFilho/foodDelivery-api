package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class CheckoutRequestV1 {
    private String id;
    private String extOrderCode;
}
