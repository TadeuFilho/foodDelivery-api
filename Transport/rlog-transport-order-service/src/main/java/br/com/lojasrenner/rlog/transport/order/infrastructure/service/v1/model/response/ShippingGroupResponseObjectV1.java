package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ShippingGroupResponseObjectV1 {
    private String city;
    private String state;
    private String country;
    private List<ShippingGroupResponseV1> shippingGroupResponse;
}
