package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BranchWithError {
    private String branchId;
    private String city;
    private String state;
    private String country;
    private String skus;
    private String zipCode;
}
