package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CheckoutInfo {
    private Boolean hasLock;
    private LocalDateTime date;
    private String extOrderCode;
}
