package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryRequestDetails {
    private List<String> eagerBranchesAdded;
}
