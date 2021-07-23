package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.stereotype.Component;

@Component
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ShippingToConfig {
    private Boolean pickupActive;
    private Boolean reQuoteActive;
    private Boolean addOperationalTimeOnEstimate;
}
