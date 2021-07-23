package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BranchOfficeConfigurationEntity {
    private Boolean active;
    private String user;
    private Date lastUpdated;
    private BranchOfficeConfigPermissionEntity permission;
    private String shippingCompanyLocker;
    private Integer storeWithdrawalTerm;
    private String quotationZipcode;
    private String cdManagement;
}
