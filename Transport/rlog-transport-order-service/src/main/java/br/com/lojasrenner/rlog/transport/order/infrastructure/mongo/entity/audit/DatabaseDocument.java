package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.audit;

import java.time.LocalDateTime;

public interface DatabaseDocument {
    String getId();
    LocalDateTime getDate();
}
